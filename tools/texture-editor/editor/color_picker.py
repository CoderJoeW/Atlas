from PyQt6.QtWidgets import (
    QWidget, QHBoxLayout, QVBoxLayout, QPushButton, QColorDialog,
    QLabel, QSpinBox, QComboBox, QCheckBox,
)
from PyQt6.QtCore import Qt, pyqtSignal
from PyQt6.QtGui import QPainter, QColor, QMouseEvent

from editor.texture_model import SYMMETRY_MODES
from editor.tools import ALL_TOOLS


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
        cs = 9
        for row in range(4):
            for col in range(4):
                gray = 200 if (row + col) % 2 == 0 else 255
                painter.fillRect(
                    col * cs, row * cs, cs, cs, QColor(gray, gray, gray),
                )
        r, g, b, a = self.color
        painter.fillRect(self.rect(), QColor(r, g, b, a))
        painter.setPen(QColor(100, 100, 100))
        painter.drawRect(0, 0, self.width() - 1, self.height() - 1)
        painter.end()


class ColorPicker(QWidget):
    """RGBA color picker with brush size, symmetry, and tool controls."""

    MAX_RECENT = 8

    tool_changed = pyqtSignal(str)
    filled_changed = pyqtSignal(bool)
    end_color_changed = pyqtSignal(tuple)
    eyedropper_clicked = pyqtSignal()
    fill_clicked = pyqtSignal()

    def __init__(self, model, parent=None):
        super().__init__(parent)
        self.model = model

        layout = QVBoxLayout(self)
        layout.setContentsMargins(4, 4, 4, 4)

        # Row 1: color swatch + pick button + brush size + symmetry
        top_row = QHBoxLayout()
        self.swatch = ColorSwatch(model.current_color)
        top_row.addWidget(self.swatch)

        pick_btn = QPushButton("Pick Color")
        pick_btn.clicked.connect(self._open_dialog)
        top_row.addWidget(pick_btn)

        eyedrop_btn = QPushButton("Eyedropper (I)")
        eyedrop_btn.setToolTip("Sample a color from the canvas")
        eyedrop_btn.clicked.connect(self.eyedropper_clicked.emit)
        top_row.addWidget(eyedrop_btn)

        fill_btn = QPushButton("Fill (F)")
        fill_btn.setToolTip("Flood fill a region on the canvas")
        fill_btn.clicked.connect(self.fill_clicked.emit)
        top_row.addWidget(fill_btn)

        top_row.addSpacing(12)
        top_row.addWidget(QLabel("Brush:"))
        self.brush_spin = QSpinBox()
        self.brush_spin.setRange(1, 32)
        self.brush_spin.setValue(model.brush_size)
        self.brush_spin.setFixedWidth(50)
        self.brush_spin.valueChanged.connect(self._on_brush_changed)
        top_row.addWidget(self.brush_spin)

        top_row.addSpacing(12)
        top_row.addWidget(QLabel("Mirror:"))
        self.symmetry_combo = QComboBox()
        for mode in SYMMETRY_MODES:
            self.symmetry_combo.addItem(mode.capitalize(), mode)
        self.symmetry_combo.setCurrentIndex(0)
        self.symmetry_combo.currentIndexChanged.connect(self._on_symmetry_changed)
        top_row.addWidget(self.symmetry_combo)

        top_row.addStretch()
        layout.addLayout(top_row)

        # Row 2: tool selector + filled checkbox
        tool_row = QHBoxLayout()
        tool_row.addWidget(QLabel("Tool:"))
        self.tool_combo = QComboBox()
        for tool_cls in ALL_TOOLS:
            self.tool_combo.addItem(tool_cls.name)
        self.tool_combo.setCurrentIndex(0)
        self.tool_combo.currentTextChanged.connect(self._on_tool_changed)
        tool_row.addWidget(self.tool_combo)

        self.filled_check = QCheckBox("Filled")
        self.filled_check.setVisible(False)
        self.filled_check.toggled.connect(self.filled_changed.emit)
        tool_row.addWidget(self.filled_check)

        tool_row.addSpacing(12)

        # Gradient end-color controls (hidden by default)
        self._end_color_label = QLabel("End Color:")
        self._end_color_label.setVisible(False)
        tool_row.addWidget(self._end_color_label)

        self._end_swatch = ColorSwatch((255, 255, 255, 255))
        self._end_swatch.setFixedSize(24, 24)
        self._end_swatch.setVisible(False)
        tool_row.addWidget(self._end_swatch)

        self._end_color_btn = QPushButton("Pick")
        self._end_color_btn.setVisible(False)
        self._end_color_btn.clicked.connect(self._open_end_color_dialog)
        tool_row.addWidget(self._end_color_btn)

        tool_row.addStretch()
        layout.addLayout(tool_row)

        # Row 3: recent colors
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
        self._end_color = (255, 255, 255, 255)

    def set_filled_visible(self, visible):
        self.filled_check.setVisible(visible)

    def set_end_color_visible(self, visible):
        self._end_color_label.setVisible(visible)
        self._end_swatch.setVisible(visible)
        self._end_color_btn.setVisible(visible)

    def _on_tool_changed(self, text):
        self.tool_changed.emit(text)

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

    def _open_end_color_dialog(self):
        r, g, b, a = self._end_color
        initial = QColor(r, g, b, a)
        color = QColorDialog.getColor(
            initial, self, "Pick End Color",
            QColorDialog.ColorDialogOption.ShowAlphaChannel,
        )
        if color.isValid():
            self._end_color = (color.red(), color.green(), color.blue(), color.alpha())
            self._end_swatch.set_color(self._end_color)
            self.end_color_changed.emit(self._end_color)

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

    def _on_symmetry_changed(self, index):
        self.model.symmetry = self.symmetry_combo.currentData()
