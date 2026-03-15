"""Tiling preview widget — displays the current face texture in a 3x3 grid."""

from PIL import Image
from PyQt6.QtWidgets import QWidget
from PyQt6.QtCore import Qt
from PyQt6.QtGui import QPainter, QImage


class TilePreview(QWidget):
    """Displays the active face texture tiled 3x3 for seamless-tiling inspection."""

    TILE_COUNT = 3

    def __init__(self, model, parent=None):
        super().__init__(parent)
        self.model = model
        self.setMinimumSize(128, 128)
        self._cached_qimage = None
        self._cached_bytes = None
        self.model.face_updated.connect(self._on_face_updated)

    def _on_face_updated(self, face):
        self._cached_qimage = None
        self.update()

    def paintEvent(self, event):
        painter = QPainter(self)
        painter.fillRect(self.rect(), Qt.GlobalColor.black)

        img = self.model.get_composite()
        tw, th = img.width, img.height
        total_w = tw * self.TILE_COUNT
        total_h = th * self.TILE_COUNT

        # Build 3x3 tiled PIL image
        tiled = Image.new("RGBA", (total_w, total_h))
        for ty in range(self.TILE_COUNT):
            for tx in range(self.TILE_COUNT):
                tiled.paste(img, (tx * tw, ty * th))

        # Convert to QImage
        raw = tiled.tobytes("raw", "BGRA")
        self._cached_bytes = raw
        qimg = QImage(raw, total_w, total_h, QImage.Format.Format_ARGB32)

        # Scale to fit widget while keeping aspect ratio
        widget_w, widget_h = self.width(), self.height()
        scale = min(widget_w / total_w, widget_h / total_h)
        scaled_w = int(total_w * scale)
        scaled_h = int(total_h * scale)
        scaled = qimg.scaled(scaled_w, scaled_h, transformMode=Qt.TransformationMode.FastTransformation)

        # Draw centered
        dx = (widget_w - scaled_w) // 2
        dy = (widget_h - scaled_h) // 2
        painter.drawImage(dx, dy, scaled)
        painter.end()
