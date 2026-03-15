"""Tool system for the pixel canvas — brush, shapes, gradient, selection."""

import math
from PyQt6.QtCore import Qt
from PyQt6.QtGui import QColor, QPen


class Tool:
    """Base class for all canvas tools."""

    name = "tool"
    one_shot = False

    def __init__(self, model):
        self.model = model

    def on_press(self, x, y, button, modifiers):
        """Return True if handled."""
        return False

    def on_move(self, x, y, buttons, modifiers):
        pass

    def on_release(self, x, y, button, modifiers):
        pass

    def on_key_press(self, key, modifiers):
        """Return True if handled."""
        return False

    def draw_overlay(self, painter, canvas):
        """Draw tool overlay. painter is QPainter in widget coords."""
        pass

    def cursor(self):
        return Qt.CursorShape.CrossCursor


class BrushTool(Tool):
    """Standard brush tool with optional shift+click flood fill shortcut."""

    name = "Brush"

    def __init__(self, model):
        super().__init__(model)
        self._painting = False

    def on_press(self, x, y, button, modifiers):
        if button != Qt.MouseButton.LeftButton:
            return False
        if modifiers & Qt.KeyboardModifier.ShiftModifier:
            self.model.flood_fill(x, y)
            return True
        self.model.begin_stroke()
        self.model.paint_brush(x, y, self.model.brush_size)
        self._painting = True
        return True

    def on_move(self, x, y, buttons, modifiers):
        if self._painting:
            self.model.paint_brush(x, y, self.model.brush_size)

    def on_release(self, x, y, button, modifiers):
        if button == Qt.MouseButton.LeftButton and self._painting:
            self._painting = False
            self.model.end_stroke()

    def draw_overlay(self, painter, canvas):
        if not canvas.underMouse():
            return
        pos = canvas.mapFromGlobal(canvas.cursor().pos())
        px, py = canvas._pixel_at(pos)
        brush = self.model.brush_size
        cell = max(1, int(canvas._cell_size()))
        ox, oy = canvas._canvas_origin()
        cx = int(ox + (px - brush // 2) * cell)
        cy = int(oy + (py - brush // 2) * cell)
        painter.setPen(QColor(255, 255, 255, 160))
        painter.drawRect(cx, cy, brush * cell, brush * cell)


class FillTool(Tool):
    """Flood fill — one-shot tool that reverts after a single click."""

    name = "Fill"
    one_shot = True

    def on_press(self, x, y, button, modifiers):
        if button != Qt.MouseButton.LeftButton:
            return False
        self.model.flood_fill(x, y)
        return True

    def draw_overlay(self, painter, canvas):
        if not canvas.underMouse():
            return
        pos = canvas.mapFromGlobal(canvas.cursor().pos())
        px, py = canvas._pixel_at(pos)
        cell = max(1, int(canvas._cell_size()))
        ox, oy = canvas._canvas_origin()
        cx = int(ox + px * cell)
        cy = int(oy + py * cell)
        pen = QPen(QColor(255, 255, 255, 160))
        pen.setWidth(2)
        painter.setPen(pen)
        painter.setBrush(Qt.BrushStyle.NoBrush)
        painter.drawRect(cx + 2, cy + 2, cell - 4, cell - 4)


class EyedropperTool(Tool):
    """Color sampler — one-shot tool that reverts after a single click."""

    name = "Eyedropper"
    one_shot = True

    def on_press(self, x, y, button, modifiers):
        if button != Qt.MouseButton.LeftButton:
            return False
        color = self.model.get_pixel(x, y)
        if color:
            self.model.current_color = tuple(color)
            self.model.color_changed.emit()
        return True

    def draw_overlay(self, painter, canvas):
        if not canvas.underMouse():
            return
        pos = canvas.mapFromGlobal(canvas.cursor().pos())
        px, py = canvas._pixel_at(pos)
        cell = max(1, int(canvas._cell_size()))
        ox, oy = canvas._canvas_origin()
        cx = int(ox + px * cell)
        cy = int(oy + py * cell)
        painter.setPen(QColor(255, 255, 255, 200))
        mid_x = cx + cell // 2
        mid_y = cy + cell // 2
        painter.drawLine(mid_x - cell, mid_y, mid_x + cell, mid_y)
        painter.drawLine(mid_x, mid_y - cell, mid_x, mid_y + cell)


# ---------------------------------------------------------------------------
# Drag-based tools (shapes, gradient)
# ---------------------------------------------------------------------------

class DragShapeTool(Tool):
    """Base for tools that drag from start to end then commit."""

    def __init__(self, model):
        super().__init__(model)
        self._start = None
        self._end = None
        self._dragging = False
        self.filled = False

    def on_press(self, x, y, button, modifiers):
        if button != Qt.MouseButton.LeftButton:
            return False
        self._start = (x, y)
        self._end = (x, y)
        self._dragging = True
        self.model.begin_stroke()
        return True

    def on_move(self, x, y, buttons, modifiers):
        if self._dragging:
            self._end = (x, y)

    def on_release(self, x, y, button, modifiers):
        if button == Qt.MouseButton.LeftButton and self._dragging:
            self._end = (x, y)
            self._commit()
            self.model.end_stroke()
            self._dragging = False
            self._start = None
            self._end = None

    def _commit(self):
        pass

    # Helpers for subclasses to convert pixel coords -> widget coords
    def _widget_xy(self, canvas, px, py):
        ox, oy = canvas._canvas_origin()
        cell = max(1, int(canvas._cell_size()))
        return int(ox + px * cell), int(oy + py * cell)

    def _overlay_pen(self):
        pen = QPen(QColor(255, 255, 0, 180))
        pen.setWidth(2)
        pen.setStyle(Qt.PenStyle.DashLine)
        return pen


def _bresenham(x0, y0, x1, y1):
    """Yield (x, y) pixels along a line from (x0, y0) to (x1, y1)."""
    dx = abs(x1 - x0)
    dy = abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx - dy
    while True:
        yield x0, y0
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 > -dy:
            err -= dy
            x0 += sx
        if e2 < dx:
            err += dx
            y0 += sy


class LineTool(DragShapeTool):
    name = "Line"

    def _commit(self):
        img = self.model.get_image()
        w, h = img.width, img.height
        for sx, sy, ex, ey in self.model._mirror_pair(*self._start, *self._end):
            for px, py in _bresenham(sx, sy, ex, ey):
                if 0 <= px < w and 0 <= py < h:
                    self.model.set_pixel(px, py, emit=False)
        self.model.face_updated.emit(self.model.active_face)

    def draw_overlay(self, painter, canvas):
        if not self._dragging or self._start is None:
            return
        cell = max(1, int(canvas._cell_size()))
        half = cell // 2
        sx, sy = self._widget_xy(canvas, self._start[0], self._start[1])
        ex, ey = self._widget_xy(canvas, self._end[0], self._end[1])
        painter.setPen(self._overlay_pen())
        painter.drawLine(sx + half, sy + half, ex + half, ey + half)


class RectTool(DragShapeTool):
    name = "Rectangle"

    def _commit(self):
        img = self.model.get_image()
        w, h = img.width, img.height
        for sx, sy, ex, ey in self.model._mirror_pair(*self._start, *self._end):
            lx, rx = min(sx, ex), max(sx, ex)
            ty, by = min(sy, ey), max(sy, ey)
            if self.filled:
                for py in range(ty, by + 1):
                    for px in range(lx, rx + 1):
                        if 0 <= px < w and 0 <= py < h:
                            self.model.set_pixel(px, py, emit=False)
            else:
                for px in range(lx, rx + 1):
                    for py in (ty, by):
                        if 0 <= px < w and 0 <= py < h:
                            self.model.set_pixel(px, py, emit=False)
                for py in range(ty, by + 1):
                    for px in (lx, rx):
                        if 0 <= px < w and 0 <= py < h:
                            self.model.set_pixel(px, py, emit=False)
        self.model.face_updated.emit(self.model.active_face)

    def draw_overlay(self, painter, canvas):
        if not self._dragging or self._start is None:
            return
        cell = max(1, int(canvas._cell_size()))
        sx, sy = self._widget_xy(canvas, min(self._start[0], self._end[0]),
                                  min(self._start[1], self._end[1]))
        ex, ey = self._widget_xy(canvas, max(self._start[0], self._end[0]),
                                  max(self._start[1], self._end[1]))
        painter.setPen(self._overlay_pen())
        painter.drawRect(sx, sy, ex - sx + cell, ey - sy + cell)


def _ellipse_pixels(cx, cy, rx, ry):
    """Yield outline pixels for an axis-aligned ellipse (midpoint algorithm)."""
    if rx == 0 and ry == 0:
        yield cx, cy
        return
    if rx == 0:
        for y in range(-ry, ry + 1):
            yield cx, cy + y
        return
    if ry == 0:
        for x in range(-rx, rx + 1):
            yield cx + x, cy
        return

    points = set()
    x = 0
    y = ry
    rx2 = rx * rx
    ry2 = ry * ry
    p1 = ry2 - rx2 * ry + rx2 // 4

    while 2 * ry2 * x <= 2 * rx2 * y:
        points.add((cx + x, cy + y))
        points.add((cx - x, cy + y))
        points.add((cx + x, cy - y))
        points.add((cx - x, cy - y))
        x += 1
        if p1 < 0:
            p1 += 2 * ry2 * x + ry2
        else:
            y -= 1
            p1 += 2 * ry2 * x - 2 * rx2 * y + ry2

    p2 = (ry2 * (x * 2 + 1) * (x * 2 + 1)) // 4 + rx2 * (y - 1) * (y - 1) - rx2 * ry2
    while y >= 0:
        points.add((cx + x, cy + y))
        points.add((cx - x, cy + y))
        points.add((cx + x, cy - y))
        points.add((cx - x, cy - y))
        y -= 1
        if p2 > 0:
            p2 -= 2 * rx2 * y + rx2
        else:
            x += 1
            p2 += 2 * ry2 * x - 2 * rx2 * y + rx2

    yield from points


class EllipseTool(DragShapeTool):
    name = "Ellipse"

    def _commit(self):
        img = self.model.get_image()
        w, h = img.width, img.height

        for sx, sy, ex, ey in self.model._mirror_pair(*self._start, *self._end):
            lx, rx = min(sx, ex), max(sx, ex)
            ty, by = min(sy, ey), max(sy, ey)
            cx_f = (lx + rx) / 2.0
            cy_f = (ty + by) / 2.0
            erx = (rx - lx) / 2.0
            ery = (by - ty) / 2.0

            if self.filled:
                for py in range(ty, by + 1):
                    for px in range(lx, rx + 1):
                        if erx > 0 and ery > 0:
                            dx = (px - cx_f) / erx
                            dy = (py - cy_f) / ery
                            if dx * dx + dy * dy > 1.0:
                                continue
                        if 0 <= px < w and 0 <= py < h:
                            self.model.set_pixel(px, py, emit=False)
            else:
                icx = round(cx_f)
                icy = round(cy_f)
                irx = max(0, round(erx))
                iry = max(0, round(ery))
                for px, py in _ellipse_pixels(icx, icy, irx, iry):
                    if 0 <= px < w and 0 <= py < h:
                        self.model.set_pixel(px, py, emit=False)

        self.model.face_updated.emit(self.model.active_face)

    def draw_overlay(self, painter, canvas):
        if not self._dragging or self._start is None:
            return
        cell = max(1, int(canvas._cell_size()))
        sx, sy = self._widget_xy(canvas, min(self._start[0], self._end[0]),
                                  min(self._start[1], self._end[1]))
        ex, ey = self._widget_xy(canvas, max(self._start[0], self._end[0]),
                                  max(self._start[1], self._end[1]))
        painter.setPen(self._overlay_pen())
        painter.drawEllipse(sx, sy, ex - sx + cell, ey - sy + cell)


# ---------------------------------------------------------------------------
# Gradient tool
# ---------------------------------------------------------------------------

class GradientTool(DragShapeTool):
    name = "Gradient"

    def __init__(self, model):
        super().__init__(model)
        self.end_color = (255, 255, 255, 255)

    def _commit(self):
        x0, y0 = self._start
        x1, y1 = self._end
        dx = x1 - x0
        dy = y1 - y0
        length_sq = dx * dx + dy * dy
        img = self.model.get_image()
        w, h = img.width, img.height
        pixels = img.load()
        c0 = self.model.current_color
        c1 = self.end_color

        # Constrain to selection if active
        sel = self.model.selection_rect
        if sel:
            min_x = max(0, sel[0])
            min_y = max(0, sel[1])
            max_x = min(w, sel[0] + sel[2])
            max_y = min(h, sel[1] + sel[3])
        else:
            min_x, min_y = 0, 0
            max_x, max_y = w, h

        for py in range(min_y, max_y):
            for px in range(min_x, max_x):
                if length_sq == 0:
                    t = 0.0
                else:
                    t = ((px - x0) * dx + (py - y0) * dy) / length_sq
                t = max(0.0, min(1.0, t))
                r = int(c0[0] + (c1[0] - c0[0]) * t)
                g = int(c0[1] + (c1[1] - c0[1]) * t)
                b = int(c0[2] + (c1[2] - c0[2]) * t)
                a = int(c0[3] + (c1[3] - c0[3]) * t)
                pixels[px, py] = (r, g, b, a)
                self.model._expand_stroke_bbox(px, py)

        self.model.face_updated.emit(self.model.active_face)

    def draw_overlay(self, painter, canvas):
        if not self._dragging or self._start is None:
            return
        cell = max(1, int(canvas._cell_size()))
        half = cell // 2
        sx, sy = self._widget_xy(canvas, self._start[0], self._start[1])
        ex, ey = self._widget_xy(canvas, self._end[0], self._end[1])
        painter.setPen(self._overlay_pen())
        painter.drawLine(sx + half, sy + half, ex + half, ey + half)
        # Color dots at endpoints
        c0 = self.model.current_color
        c1 = self.end_color
        painter.setBrush(QColor(*c0))
        painter.setPen(Qt.PenStyle.NoPen)
        painter.drawEllipse(sx + half - 5, sy + half - 5, 10, 10)
        painter.setBrush(QColor(*c1))
        painter.drawEllipse(ex + half - 5, ey + half - 5, 10, 10)


# ---------------------------------------------------------------------------
# Selection tool
# ---------------------------------------------------------------------------

class SelectionTool(Tool):
    name = "Selection"

    STATE_IDLE = "idle"
    STATE_SELECTED = "selected"
    STATE_FLOATING = "floating"

    def __init__(self, model):
        super().__init__(model)
        self._state = self.STATE_IDLE
        self._rect = None           # (x, y, w, h) in pixel coords
        self._clipboard = None      # PIL Image
        self._float_image = None    # floating paste image
        self._float_pos = None      # (x, y)
        self._drag_start = None     # mouse start for drag
        self._drag_origin = None    # original rect/float pos at drag start
        self._dragging_selection = False
        self._dragging_float = False
        self._creating = False

    def _update_model_selection(self):
        """Sync selection_rect on the model for other tools (e.g. gradient)."""
        if self._state == self.STATE_SELECTED and self._rect:
            self.model.selection_rect = self._rect
        else:
            self.model.selection_rect = None

    def _point_in_rect(self, px, py):
        if self._rect is None:
            return False
        rx, ry, rw, rh = self._rect
        return rx <= px < rx + rw and ry <= py < ry + rh

    def on_press(self, x, y, button, modifiers):
        if button != Qt.MouseButton.LeftButton:
            return False

        # Floating state: click commits, then start fresh
        if self._state == self.STATE_FLOATING:
            self._commit_float()

        # If selected and clicking inside, start moving selection content
        if self._state == self.STATE_SELECTED and self._point_in_rect(x, y):
            self._start_move(x, y)
            return True

        # Start new selection
        self._state = self.STATE_IDLE
        self._rect = None
        self._update_model_selection()
        self._drag_start = (x, y)
        self._creating = True
        return True

    def on_move(self, x, y, buttons, modifiers):
        if self._creating and self._drag_start is not None:
            sx, sy = self._drag_start
            lx, ty = min(sx, x), min(sy, y)
            rw = abs(x - sx) + 1
            rh = abs(y - sy) + 1
            self._rect = (lx, ty, rw, rh)
        elif self._dragging_float and self._float_pos is not None:
            ox, oy = self._drag_origin
            sx, sy = self._drag_start
            self._float_pos = (ox + x - sx, oy + y - sy)
        elif self._dragging_selection and self._rect is not None:
            ox, oy, ow, oh = self._drag_origin
            sx, sy = self._drag_start
            self._rect = (ox + x - sx, oy + y - sy, ow, oh)

    def on_release(self, x, y, button, modifiers):
        if button != Qt.MouseButton.LeftButton:
            return
        if self._creating:
            self._creating = False
            if self._rect and self._rect[2] > 0 and self._rect[3] > 0:
                self._state = self.STATE_SELECTED
            else:
                self._state = self.STATE_IDLE
                self._rect = None
            self._update_model_selection()
        self._dragging_selection = False
        self._dragging_float = False

    def _start_move(self, x, y):
        """Cut selection content into a floating image and start dragging it."""
        from PIL import Image
        rx, ry, rw, rh = self._rect
        img = self.model.get_image()
        self.model.begin_stroke()
        self._float_image = img.crop((rx, ry, rx + rw, ry + rh)).copy()
        # Clear the original area
        transparent = (0, 0, 0, 0)
        pixels = img.load()
        for py in range(ry, ry + rh):
            for px in range(rx, rx + rw):
                if 0 <= px < img.width and 0 <= py < img.height:
                    pixels[px, py] = transparent
        self.model.face_updated.emit(self.model.active_face)
        self.model.end_stroke()

        self._float_pos = (rx, ry)
        self._drag_start = (x, y)
        self._drag_origin = (rx, ry)
        self._dragging_float = True
        self._state = self.STATE_FLOATING
        self._update_model_selection()

    def _commit_float(self):
        """Paste floating image onto the canvas."""
        if self._float_image is None or self._float_pos is None:
            self._state = self.STATE_IDLE
            self._update_model_selection()
            return
        self.model.begin_stroke()
        img = self.model.get_image()
        fx, fy = self._float_pos
        img.paste(self._float_image, (int(fx), int(fy)), self._float_image)
        self.model.face_updated.emit(self.model.active_face)
        self.model.end_stroke()
        self._float_image = None
        self._float_pos = None
        self._rect = None
        self._state = self.STATE_IDLE
        self._update_model_selection()

    def on_key_press(self, key, modifiers):
        ctrl = modifiers & Qt.KeyboardModifier.ControlModifier

        if key == Qt.Key.Key_Escape:
            if self._state == self.STATE_FLOATING:
                # Cancel float — discard without committing
                self._float_image = None
                self._float_pos = None
            self._rect = None
            self._state = self.STATE_IDLE
            self._update_model_selection()
            return True

        if key == Qt.Key.Key_Return or key == Qt.Key.Key_Enter:
            if self._state == self.STATE_FLOATING:
                self._commit_float()
                return True

        if key == Qt.Key.Key_Delete or key == Qt.Key.Key_Backspace:
            if self._state == self.STATE_SELECTED and self._rect:
                self._delete_selection()
                return True

        if ctrl and key == Qt.Key.Key_C:
            if self._rect and self._state == self.STATE_SELECTED:
                self._copy_selection()
                return True

        if ctrl and key == Qt.Key.Key_X:
            if self._rect and self._state == self.STATE_SELECTED:
                self._cut_selection()
                return True

        if ctrl and key == Qt.Key.Key_V:
            if self._clipboard is not None:
                self._paste()
                return True

        return False

    def _copy_selection(self):
        rx, ry, rw, rh = self._rect
        img = self.model.get_image()
        self._clipboard = img.crop((rx, ry, rx + rw, ry + rh)).copy()

    def _cut_selection(self):
        self._copy_selection()
        self._delete_selection()

    def _delete_selection(self):
        from PIL import Image
        rx, ry, rw, rh = self._rect
        img = self.model.get_image()
        self.model.begin_stroke()
        pixels = img.load()
        transparent = (0, 0, 0, 0)
        for py in range(ry, ry + rh):
            for px in range(rx, rx + rw):
                if 0 <= px < img.width and 0 <= py < img.height:
                    pixels[px, py] = transparent
        self.model.face_updated.emit(self.model.active_face)
        self.model.end_stroke()
        self._rect = None
        self._state = self.STATE_IDLE
        self._update_model_selection()

    def _paste(self):
        if self._state == self.STATE_FLOATING:
            self._commit_float()
        self._float_image = self._clipboard.copy()
        self._float_pos = (0, 0)
        self._state = self.STATE_FLOATING
        self._update_model_selection()

    def draw_overlay(self, painter, canvas):
        cell = max(1, int(canvas._cell_size()))
        ox, oy = canvas._canvas_origin()

        # Draw floating image
        if self._state == self.STATE_FLOATING and self._float_image is not None:
            fx, fy = self._float_pos
            fw, fh = self._float_image.width, self._float_image.height
            # Convert PIL image to QImage for overlay
            raw = self._float_image.tobytes("raw", "BGRA")
            from PyQt6.QtGui import QImage
            qimg = QImage(raw, fw, fh, QImage.Format.Format_ARGB32)
            self._float_bytes = raw  # prevent GC
            wx = int(ox + fx * cell)
            wy = int(oy + fy * cell)
            scaled = qimg.scaled(fw * cell, fh * cell)
            painter.setOpacity(0.7)
            painter.drawImage(wx, wy, scaled)
            painter.setOpacity(1.0)
            # Dashed border around float
            self._draw_dashed_rect(painter, wx, wy, fw * cell, fh * cell)

        # Draw selection rectangle
        if self._rect and self._state in (self.STATE_SELECTED, self.STATE_IDLE) and self._creating:
            rx, ry, rw, rh = self._rect
            wx = int(ox + rx * cell)
            wy = int(oy + ry * cell)
            self._draw_dashed_rect(painter, wx, wy, rw * cell, rh * cell)
        elif self._rect and self._state == self.STATE_SELECTED:
            rx, ry, rw, rh = self._rect
            wx = int(ox + rx * cell)
            wy = int(oy + ry * cell)
            self._draw_dashed_rect(painter, wx, wy, rw * cell, rh * cell)

    def _draw_dashed_rect(self, painter, x, y, w, h):
        """Draw a marching-ants style dashed rectangle."""
        # White pass
        pen = QPen(QColor(255, 255, 255, 200))
        pen.setWidth(2)
        pen.setStyle(Qt.PenStyle.DashLine)
        painter.setPen(pen)
        painter.setBrush(Qt.BrushStyle.NoBrush)
        painter.drawRect(x, y, w, h)
        # Black pass offset
        pen2 = QPen(QColor(0, 0, 0, 200))
        pen2.setWidth(2)
        pen2.setStyle(Qt.PenStyle.DashLine)
        pen2.setDashOffset(4)
        painter.setPen(pen2)
        painter.drawRect(x, y, w, h)


# Tool classes for the tool dropdown (persistent tools you switch between)
ALL_TOOLS = [
    BrushTool, LineTool, RectTool, EllipseTool,
    GradientTool, SelectionTool,
]

# One-shot action tools (used once then revert to previous tool)
ACTION_TOOLS = {
    "Fill": FillTool,
    "Eyedropper": EyedropperTool,
}
