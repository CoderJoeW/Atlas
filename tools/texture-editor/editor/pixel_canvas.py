from PyQt6.QtWidgets import QWidget
from PyQt6.QtCore import Qt
from PyQt6.QtGui import QPainter, QColor, QImage, QMouseEvent, QWheelEvent, QPen


class PixelCanvas(QWidget):
    """Grid-based pixel editor with zoom, pan, and brush size support."""

    MIN_ZOOM = 1.0
    MAX_ZOOM = 64.0

    def __init__(self, model, parent=None):
        super().__init__(parent)
        self.model = model
        self._cached_image = None
        self._painting = False
        self._panning = False
        self._last_pan_pos = None
        self._zoom = 0.0  # 0 = fit-to-widget, >0 = manual zoom level (pixels per cell)
        self._pan_x = 0.0  # pan offset in widget pixels
        self._pan_y = 0.0
        self.setMinimumSize(256, 256)
        self.setMouseTracking(True)
        self.setFocusPolicy(Qt.FocusPolicy.WheelFocus)
        self.model.face_updated.connect(self._on_face_updated)
        self._rebuild_cache()

    def _on_face_updated(self, face):
        self._rebuild_cache()
        self.update()

    def _cell_size(self):
        """Return the current cell size in widget pixels."""
        img = self.model.get_image()
        if self._zoom > 0:
            return self._zoom
        # Fit to widget
        fit = min(self.width(), self.height()) / max(img.width, img.height)
        return max(1.0, fit)

    def _rebuild_cache(self):
        img = self.model.get_image()
        w, h = img.width, img.height
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

        # Scale up the texture image with nearest-neighbor and draw it on top
        scaled = img.resize(
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

    def _canvas_origin(self):
        """Top-left corner of the canvas in widget coordinates."""
        img = self.model.get_image()
        cell = max(1, int(self._cell_size()))
        canvas_w = cell * img.width
        canvas_h = cell * img.height
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

        # Draw brush cursor
        if self.underMouse() and not self._panning:
            pos = self.mapFromGlobal(self.cursor().pos())
            px, py = self._pixel_at(pos)
            brush = self.model.brush_size
            cell = max(1, int(self._cell_size()))
            cx = int(ox + (px - brush // 2) * cell)
            cy = int(oy + (py - brush // 2) * cell)
            painter.setPen(QColor(255, 255, 255, 160))
            painter.drawRect(cx, cy, brush * cell, brush * cell)

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

    def _paint_at(self, pos):
        """Paint a brush_size square centered on the pixel under pos."""
        cx, cy = self._pixel_at(pos)
        self.model.paint_brush(cx, cy, self.model.brush_size)

    def mousePressEvent(self, event: QMouseEvent):
        if event.button() == Qt.MouseButton.MiddleButton:
            self._panning = True
            self._last_pan_pos = event.position()
            self.setCursor(Qt.CursorShape.ClosedHandCursor)
            return

        pos = event.position().toPoint()
        if event.button() == Qt.MouseButton.LeftButton:
            self.model.begin_stroke()
            self._paint_at(pos)
            self._painting = True
        elif event.button() == Qt.MouseButton.RightButton:
            x, y = self._pixel_at(pos)
            color = self.model.get_pixel(x, y)
            if color:
                self.model.current_color = tuple(color)
                self.model.color_changed.emit()

    def mouseMoveEvent(self, event: QMouseEvent):
        if self._panning and self._last_pan_pos is not None:
            dx = event.position().x() - self._last_pan_pos.x()
            dy = event.position().y() - self._last_pan_pos.y()
            self._pan_x += dx
            self._pan_y += dy
            self._last_pan_pos = event.position()
            self.update()
            return

        if self._painting:
            self._paint_at(event.position().toPoint())

        # Update brush cursor position
        self.update()

    def mouseReleaseEvent(self, event: QMouseEvent):
        if event.button() == Qt.MouseButton.MiddleButton:
            self._panning = False
            self._last_pan_pos = None
            self.setCursor(Qt.CursorShape.ArrowCursor)
            return

        if event.button() == Qt.MouseButton.LeftButton and self._painting:
            self._painting = False
            self.model.end_stroke()

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
        img = self.model.get_image()
        new_canvas_w = int(new_cell) * img.width
        new_canvas_h = int(new_cell) * img.height
        new_center_ox = (self.width() - new_canvas_w) / 2.0
        new_center_oy = (self.height() - new_canvas_h) / 2.0
        target_ox = mouse_pos.x() - px_before * new_cell
        target_oy = mouse_pos.y() - py_before * new_cell
        self._pan_x = target_ox - new_center_ox
        self._pan_y = target_oy - new_center_oy

        self._rebuild_cache()
        self.update()
