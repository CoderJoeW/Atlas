from PyQt6.QtWidgets import (
    QWidget, QHBoxLayout, QVBoxLayout, QPushButton, QColorDialog,
    QLabel, QSpinBox,
)
from PyQt6.QtCore import Qt, QSize
from PyQt6.QtGui import QPainter, QColor, QMouseEvent


class ColorSwatch(QWidget):
    """Displays a color with checkerboard behind transparent colors."""

    def __init__(self, color=(0, 0, 0, 255), parent=None):
        super().__init__(parent)
        self.color = color
        self.setFixedSize(36, 36)

    def set_color(self, color):
        self.color = color
        self.update()

    def paintEvent(self, event):
        painter = QPainter(self)
        # Checkerboard
        cs = 9
        for row in range(4):
            for col in range(4):
                gray = 200 if (row + col) % 2 == 0 else 255
                painter.fillRect(col * cs, row * cs, cs, cs, QColor(gray, gray, gray))
        r, g, b, a = self.color
        painter.fillRect(self.rect(), QColor(r, g, b, a))
        painter.setPen(QColor(100, 100, 100))
        painter.drawRect(0, 0, self.width() - 1, self.height() - 1)
        painter.end()


class ColorPicker(QWidget):
    """RGBA color picker with recent colors row."""

    MAX_RECENT = 8

    def __init__(self, model, parent=None):
        super().__init__(parent)
        self.model = model

        layout = QVBoxLayout(self)
        layout.setContentsMargins(4, 4, 4, 4)

        # Current color swatch + pick button + brush size
        top_row = QHBoxLayout()
        self.swatch = ColorSwatch(model.current_color)
        top_row.addWidget(self.swatch)

        pick_btn = QPushButton("Pick Color")
        pick_btn.clicked.connect(self._open_dialog)
        top_row.addWidget(pick_btn)

        top_row.addSpacing(12)
        top_row.addWidget(QLabel("Brush:"))
        self.brush_spin = QSpinBox()
        self.brush_spin.setRange(1, 32)
        self.brush_spin.setValue(model.brush_size)
        self.brush_spin.setFixedWidth(50)
        self.brush_spin.valueChanged.connect(self._on_brush_changed)
        top_row.addWidget(self.brush_spin)

        top_row.addStretch()
        layout.addLayout(top_row)

        # Recent colors
        self._recent_colors = []
        self._recent_row = QHBoxLayout()
        self._recent_row.setSpacing(2)
        self._recent_swatches = []
        for _ in range(self.MAX_RECENT):
            sw = ColorSwatch((200, 200, 200, 255))
            sw.setFixedSize(24, 24)
            sw.setCursor(Qt.CursorShape.PointingHandCursor)
            sw.mousePressEvent = lambda e, s=sw: self._pick_recent(s)
            self._recent_swatches.append(sw)
            self._recent_row.addWidget(sw)
        self._recent_row.addStretch()
        layout.addLayout(self._recent_row)

        self.model.color_changed.connect(self._sync_swatch)

    def _sync_swatch(self):
        self.swatch.set_color(self.model.current_color)

    def _open_dialog(self):
        r, g, b, a = self.model.current_color
        initial = QColor(r, g, b, a)
        color = QColorDialog.getColor(
            initial, self, "Pick Color",
            QColorDialog.ColorDialogOption.ShowAlphaChannel,
        )
        if color.isValid():
            new_color = (color.red(), color.green(), color.blue(), color.alpha())
            self.model.current_color = new_color
            self.model.color_changed.emit()
            self.swatch.set_color(new_color)
            self._add_recent(new_color)

    def _add_recent(self, color):
        if color in self._recent_colors:
            self._recent_colors.remove(color)
        self._recent_colors.insert(0, color)
        self._recent_colors = self._recent_colors[:self.MAX_RECENT]
        for i, sw in enumerate(self._recent_swatches):
            if i < len(self._recent_colors):
                sw.set_color(self._recent_colors[i])

    def _pick_recent(self, swatch):
        color = swatch.color
        self.model.current_color = color
        self.model.color_changed.emit()
        self.swatch.set_color(color)

    def _on_brush_changed(self, value):
        self.model.brush_size = value
