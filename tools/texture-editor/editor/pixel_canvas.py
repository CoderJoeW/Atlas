from PIL import Image as PILImage
from PyQt6.QtWidgets import QWidget
from PyQt6.QtCore import Qt
from PyQt6.QtGui import QPainter, QColor, QImage, QMouseEvent, QWheelEvent, QPen

from editor.tools import BrushTool


class PixelCanvas(QWidget):
    """Grid-based pixel editor with zoom, pan, and tool delegation."""

    MIN_ZOOM = 1.0
    MAX_ZOOM = 64.0

    def __init__(self, model, parent=None):
        super().__init__(parent)
        self.model = model
        self._cached_image = None
        self._cached_reference = None
        self._ref_bytes = None
        self._panning = False
        self._last_pan_pos = None
        self._zoom = 0.0  # 0 = fit-to-widget, >0 = manual zoom level (pixels per cell)
        self._pan_x = 0.0  # pan offset in widget pixels
        self._pan_y = 0.0
        self._active_tool = BrushTool(model)
        self.setMinimumSize(256, 256)
        self.setMouseTracking(True)
        self.setFocusPolicy(Qt.FocusPolicy.WheelFocus)
        self.model.face_updated.connect(self._on_face_updated)
        self.model.reference_changed.connect(self._on_reference_changed)
        self._rebuild_cache()

    def set_tool(self, tool):
        self._active_tool = tool
        self._previous_tool = None
        self.update()

    def activate_one_shot(self, tool):
        """Temporarily switch to a one-shot tool, reverting after one click."""
        self._previous_tool = self._active_tool
        self._active_tool = tool
        self.update()

    def _on_face_updated(self, face):
        self._rebuild_cache()
        self.update()

    def _on_reference_changed(self):
        self._rebuild_cache()
        self.update()

    def _cell_size(self):
        """Return the current cell size in widget pixels."""
        if self._zoom > 0:
            return self._zoom
        # Fit to widget
        fit = min(self.width(), self.height()) / max(self.model.size, 1)
        return max(1.0, fit)

    def _rebuild_cache(self):
        composite = self.model.get_composite()
        w, h = composite.width, composite.height
        cell = self._cell_size()
        cell_int = max(1, int(cell))
        canvas_w = cell_int * w
        canvas_h = cell_int * h

        qimg = QImage(canvas_w, canvas_h, QImage.Format.Format_ARGB32)
        painter = QPainter(qimg)

        # Draw checkerboard as two solid passes for speed
        painter.fillRect(0, 0, canvas_w, canvas_h, QColor(255, 255, 255))
        check_color = QColor(200, 200, 200)
        if cell_int >= 4:
            check_size = max(1, cell_int // 2)
            for cy in range(h):
                for cx in range(w):
                    rx = cx * cell_int
                    ry = cy * cell_int
                    painter.fillRect(rx, ry, check_size, check_size, check_color)
                    painter.fillRect(
                        rx + check_size, ry + check_size,
                        check_size, check_size, check_color,
                    )

        # Scale up the composite with nearest-neighbor and draw it on top
        scaled = composite.resize(
            (canvas_w, canvas_h), resample=0,  # 0 = NEAREST
        )
        raw = scaled.tobytes("raw", "BGRA")
        tex_qimg = QImage(raw, canvas_w, canvas_h, QImage.Format.Format_ARGB32)
        # QImage doesn't own the buffer, so we must keep a reference
        self._scaled_bytes = raw
        painter.drawImage(0, 0, tex_qimg)

        # Grid lines
        if cell_int >= 4:
            pen = QPen(QColor(180, 180, 180, 80))
            painter.setPen(pen)
            for cx in range(w + 1):
                painter.drawLine(cx * cell_int, 0, cx * cell_int, canvas_h)
            for cy in range(h + 1):
                painter.drawLine(0, cy * cell_int, canvas_w, cy * cell_int)

        painter.end()
        self._cached_image = qimg

        # Reference image cache
        if self.model.reference_image is not None:
            ref = self.model.reference_image
            scaled_ref = ref.resize((canvas_w, canvas_h), PILImage.Resampling.BILINEAR)
            ref_raw = scaled_ref.tobytes("raw", "BGRA")
            self._ref_bytes = ref_raw
            self._cached_reference = QImage(
                ref_raw, canvas_w, canvas_h, QImage.Format.Format_ARGB32,
            )
        else:
            self._cached_reference = None
            self._ref_bytes = None

    def _canvas_origin(self):
        """Top-left corner of the canvas in widget coordinates."""
        cell = max(1, int(self._cell_size()))
        size = self.model.size
        canvas_w = cell * size
        canvas_h = cell * size
        ox = (self.width() - canvas_w) / 2.0 + self._pan_x
        oy = (self.height() - canvas_h) / 2.0 + self._pan_y
        return ox, oy

    def paintEvent(self, event):
        if self._cached_image is None:
            self._rebuild_cache()
        painter = QPainter(self)
        painter.fillRect(self.rect(), QColor(50, 50, 50))
        ox, oy = self._canvas_origin()
        painter.drawImage(int(ox), int(oy), self._cached_image)

        # Reference image overlay
        if self._cached_reference is not None:
            painter.setOpacity(self.model.reference_opacity)
            painter.drawImage(int(ox), int(oy), self._cached_reference)
            painter.setOpacity(1.0)

        # Delegate overlay drawing to the active tool
        if not self._panning:
            self._active_tool.draw_overlay(painter, self)

        painter.end()

    def resizeEvent(self, event):
        self._rebuild_cache()
        super().resizeEvent(event)

    def _pixel_at(self, pos):
        ox, oy = self._canvas_origin()
        cell = self._cell_size()
        x = int((pos.x() - ox) / cell)
        y = int((pos.y() - oy) / cell)
        return x, y

    def mousePressEvent(self, event: QMouseEvent):
        # Middle button: pan (universal)
        if event.button() == Qt.MouseButton.MiddleButton:
            self._panning = True
            self._last_pan_pos = event.position()
            self.setCursor(Qt.CursorShape.ClosedHandCursor)
            return

        pos = event.position().toPoint()

        # Right button: eyedropper (universal)
        if event.button() == Qt.MouseButton.RightButton:
            x, y = self._pixel_at(pos)
            color = self.model.get_pixel(x, y)
            if color:
                self.model.current_color = tuple(color)
                self.model.color_changed.emit()
            return

        # Left button: delegate to active tool
        x, y = self._pixel_at(pos)
        handled = self._active_tool.on_press(x, y, event.button(), event.modifiers())
        if handled and self._active_tool.one_shot and self._previous_tool:
            self._active_tool = self._previous_tool
            self._previous_tool = None
            self.update()

    def mouseMoveEvent(self, event: QMouseEvent):
        if self._panning and self._last_pan_pos is not None:
            dx = event.position().x() - self._last_pan_pos.x()
            dy = event.position().y() - self._last_pan_pos.y()
            self._pan_x += dx
            self._pan_y += dy
            self._last_pan_pos = event.position()
            self.update()
            return

        x, y = self._pixel_at(event.position().toPoint())
        self._active_tool.on_move(x, y, event.buttons(), event.modifiers())

        # Repaint for cursor / overlay updates
        self.update()

    def mouseReleaseEvent(self, event: QMouseEvent):
        if event.button() == Qt.MouseButton.MiddleButton:
            self._panning = False
            self._last_pan_pos = None
            self.setCursor(Qt.CursorShape.ArrowCursor)
            return

        x, y = self._pixel_at(event.position().toPoint())
        self._active_tool.on_release(x, y, event.button(), event.modifiers())

    def keyPressEvent(self, event):
        if self._active_tool.on_key_press(event.key(), event.modifiers()):
            self.update()
            return
        super().keyPressEvent(event)

    def wheelEvent(self, event: QWheelEvent):
        delta = event.angleDelta().y()
        if delta == 0:
            return

        old_cell = self._cell_size()

        # Zoom toward/away from cursor position
        mouse_pos = event.position()
        ox, oy = self._canvas_origin()
        # Pixel coordinate under the mouse before zoom
        px_before = (mouse_pos.x() - ox) / old_cell
        py_before = (mouse_pos.y() - oy) / old_cell

        # Adjust zoom level
        factor = 1.25 if delta > 0 else 1.0 / 1.25
        if self._zoom <= 0:
            self._zoom = old_cell
        self._zoom = max(self.MIN_ZOOM, min(self.MAX_ZOOM, self._zoom * factor))

        new_cell = self._cell_size()

        # Adjust pan so the pixel under the cursor stays in place
        size = self.model.size
        new_canvas_w = int(new_cell) * size
        new_canvas_h = int(new_cell) * size
        new_center_ox = (self.width() - new_canvas_w) / 2.0
        new_center_oy = (self.height() - new_canvas_h) / 2.0
        target_ox = mouse_pos.x() - px_before * new_cell
        target_oy = mouse_pos.y() - py_before * new_cell
        self._pan_x = target_ox - new_center_ox
        self._pan_y = target_oy - new_center_oy

        self._rebuild_cache()
        self.update()
