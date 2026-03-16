"""Layer management panel — add, delete, reorder, and adjust layer properties."""

from PyQt6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QPushButton,
    QLabel, QListWidget, QListWidgetItem, QSlider,
)
from PyQt6.QtCore import Qt


class LayerPanel(QWidget):
    """Displays and manages layers for the active face."""

    def __init__(self, model, parent=None):
        super().__init__(parent)
        self.model = model

        layout = QVBoxLayout(self)
        layout.setContentsMargins(4, 4, 4, 4)

        layout.addWidget(QLabel("Layers"))

        self.layer_list = QListWidget()
        self.layer_list.currentRowChanged.connect(self._on_layer_selected)
        self.layer_list.itemChanged.connect(self._on_item_changed)
        layout.addWidget(self.layer_list)

        # Buttons
        btn_row = QHBoxLayout()

        add_btn = QPushButton("+")
        add_btn.setFixedWidth(30)
        add_btn.setToolTip("Add layer")
        add_btn.clicked.connect(self._add_layer)
        btn_row.addWidget(add_btn)

        del_btn = QPushButton("-")
        del_btn.setFixedWidth(30)
        del_btn.setToolTip("Delete layer")
        del_btn.clicked.connect(self._delete_layer)
        btn_row.addWidget(del_btn)

        merge_btn = QPushButton("Merge")
        merge_btn.setToolTip("Merge active layer down")
        merge_btn.clicked.connect(self._merge_down)
        btn_row.addWidget(merge_btn)

        up_btn = QPushButton("Up")
        up_btn.setToolTip("Move layer up")
        up_btn.clicked.connect(self._move_up)
        btn_row.addWidget(up_btn)

        down_btn = QPushButton("Down")
        down_btn.setToolTip("Move layer down")
        down_btn.clicked.connect(self._move_down)
        btn_row.addWidget(down_btn)

        layout.addLayout(btn_row)

        # Opacity
        opacity_row = QHBoxLayout()
        opacity_row.addWidget(QLabel("Opacity:"))
        self.opacity_slider = QSlider(Qt.Orientation.Horizontal)
        self.opacity_slider.setRange(0, 100)
        self.opacity_slider.setValue(100)
        self.opacity_slider.valueChanged.connect(self._on_opacity_changed)
        opacity_row.addWidget(self.opacity_slider)
        self.opacity_label = QLabel("100%")
        self.opacity_label.setFixedWidth(40)
        opacity_row.addWidget(self.opacity_label)
        layout.addLayout(opacity_row)

        self.model.face_updated.connect(lambda _: self._refresh())
        self.model.state_changed.connect(lambda _: self._refresh())
        self.model.layers_changed.connect(self._refresh)

        self._refresh()

    def _refresh(self):
        state = self.model.active_state
        face = self.model.active_face
        layers = state.layers[face]
        active_idx = state.active_layer[face]

        self.layer_list.blockSignals(True)
        self.layer_list.clear()

        # Show layers top-to-bottom (highest index first)
        for i in range(len(layers) - 1, -1, -1):
            layer = layers[i]
            item = QListWidgetItem(layer.name)
            item.setData(Qt.ItemDataRole.UserRole, i)
            item.setFlags(item.flags() | Qt.ItemFlag.ItemIsUserCheckable)
            item.setCheckState(
                Qt.CheckState.Checked if layer.visible else Qt.CheckState.Unchecked
            )
            self.layer_list.addItem(item)

        # Select the active layer
        for row in range(self.layer_list.count()):
            item = self.layer_list.item(row)
            if item.data(Qt.ItemDataRole.UserRole) == active_idx:
                self.layer_list.setCurrentRow(row)
                break

        self.layer_list.blockSignals(False)

        # Update opacity slider
        if 0 <= active_idx < len(layers):
            pct = int(layers[active_idx].opacity * 100)
            self.opacity_slider.blockSignals(True)
            self.opacity_slider.setValue(pct)
            self.opacity_slider.blockSignals(False)
            self.opacity_label.setText(f"{pct}%")

    def _on_layer_selected(self, row):
        if row < 0:
            return
        item = self.layer_list.item(row)
        if item is None:
            return
        layer_idx = item.data(Qt.ItemDataRole.UserRole)
        self.model.set_active_layer(layer_idx)

    def _on_item_changed(self, item):
        layer_idx = item.data(Qt.ItemDataRole.UserRole)
        checked = item.checkState() == Qt.CheckState.Checked
        self.model.set_layer_visible(layer_idx, checked)

    def _add_layer(self):
        self.model.add_layer()

    def _delete_layer(self):
        self.model.delete_layer()

    def _merge_down(self):
        self.model.merge_layer_down()

    def _move_up(self):
        self.model.move_layer(1)

    def _move_down(self):
        self.model.move_layer(-1)

    def _on_opacity_changed(self, value):
        self.model.set_layer_opacity(value / 100.0)
        self.opacity_label.setText(f"{value}%")
