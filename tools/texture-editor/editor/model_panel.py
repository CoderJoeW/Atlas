"""Model editor panel — create and modify block shape elements."""

import json
import os

from PyQt6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QPushButton,
    QLabel, QListWidget, QListWidgetItem, QSpinBox,
    QCheckBox, QComboBox, QFileDialog, QMessageBox,
    QGroupBox, QGridLayout,
)
from PyQt6.QtCore import Qt

from editor.texture_model import FACE_NAMES, MODELS_DIR, ModelData, ModelElement


CULLFACE_OPTIONS = ["(none)"] + FACE_NAMES

CUSTOM_MODELS_DIR = os.path.join(MODELS_DIR, "block", "custom")


class ModelPanel(QWidget):
    """Displays and edits block model elements for the active state."""

    def __init__(self, model, parent=None):
        super().__init__(parent)
        self.model = model
        self._selected_elem = -1
        self._selected_face = None
        self._updating = False

        layout = QVBoxLayout(self)
        layout.setContentsMargins(4, 4, 4, 4)

        # Header
        header_row = QHBoxLayout()
        header_row.addWidget(QLabel("Model"))
        self._file_label = QLabel("")
        self._file_label.setStyleSheet("color: #888; font-size: 11px;")
        header_row.addWidget(self._file_label, stretch=1)
        layout.addLayout(header_row)

        # Element list
        self.elem_list = QListWidget()
        self.elem_list.setMaximumHeight(120)
        self.elem_list.currentRowChanged.connect(self._on_elem_selected)
        layout.addWidget(self.elem_list)

        # Element buttons
        elem_btn_row = QHBoxLayout()
        add_btn = QPushButton("+")
        add_btn.setFixedWidth(30)
        add_btn.setToolTip("Add element")
        add_btn.clicked.connect(self._add_element)
        elem_btn_row.addWidget(add_btn)

        del_btn = QPushButton("-")
        del_btn.setFixedWidth(30)
        del_btn.setToolTip("Delete element")
        del_btn.clicked.connect(self._delete_element)
        elem_btn_row.addWidget(del_btn)

        dup_btn = QPushButton("Dup")
        dup_btn.setToolTip("Duplicate element")
        dup_btn.clicked.connect(self._duplicate_element)
        elem_btn_row.addWidget(dup_btn)

        self._wireframe_cb = QCheckBox("Wireframe")
        self._wireframe_cb.setChecked(False)
        self._wireframe_cb.toggled.connect(self._on_wireframe_toggled)
        elem_btn_row.addStretch()
        elem_btn_row.addWidget(self._wireframe_cb)
        layout.addLayout(elem_btn_row)

        # Dimension editors
        self._dims_group = QGroupBox("Dimensions")
        dims_layout = QGridLayout(self._dims_group)
        dims_layout.setContentsMargins(4, 4, 4, 4)

        dims_layout.addWidget(QLabel("From:"), 0, 0)
        self._from_spins = []
        for i, axis in enumerate(("X", "Y", "Z")):
            spin = QSpinBox()
            spin.setRange(0, 16)
            spin.setPrefix(f"{axis}: ")
            spin.valueChanged.connect(self._on_dims_changed)
            self._from_spins.append(spin)
            dims_layout.addWidget(spin, 0, i + 1)

        dims_layout.addWidget(QLabel("To:"), 1, 0)
        self._to_spins = []
        for i, axis in enumerate(("X", "Y", "Z")):
            spin = QSpinBox()
            spin.setRange(0, 16)
            spin.setPrefix(f"{axis}: ")
            spin.valueChanged.connect(self._on_dims_changed)
            self._to_spins.append(spin)
            dims_layout.addWidget(spin, 1, i + 1)

        self._dims_group.setVisible(False)
        layout.addWidget(self._dims_group)

        # Face visibility checkboxes
        self._faces_group = QGroupBox("Faces")
        faces_layout = QGridLayout(self._faces_group)
        faces_layout.setContentsMargins(4, 4, 4, 4)

        self._face_checks = {}
        face_labels = {"north": "N", "south": "S", "east": "E",
                       "west": "W", "up": "Up", "down": "Dn"}
        for i, face in enumerate(FACE_NAMES):
            cb = QCheckBox(face_labels[face])
            cb.clicked.connect(lambda checked, f=face: self._on_face_toggled(f, checked))
            self._face_checks[face] = cb
            faces_layout.addWidget(cb, i // 3, i % 3)

        self._faces_group.setVisible(False)
        layout.addWidget(self._faces_group)

        # Per-face UV editor
        self._uv_group = QGroupBox("Face UV")
        uv_layout = QGridLayout(self._uv_group)
        uv_layout.setContentsMargins(4, 4, 4, 4)

        self._face_select_combo = QComboBox()
        for face in FACE_NAMES:
            self._face_select_combo.addItem(face.capitalize(), face)
        self._face_select_combo.currentIndexChanged.connect(self._on_uv_face_changed)
        uv_layout.addWidget(QLabel("Face:"), 0, 0)
        uv_layout.addWidget(self._face_select_combo, 0, 1, 1, 3)

        uv_labels = ["U1", "V1", "U2", "V2"]
        self._uv_spins = []
        for i, label in enumerate(uv_labels):
            spin = QSpinBox()
            spin.setRange(0, 16)
            spin.setPrefix(f"{label}: ")
            spin.valueChanged.connect(self._on_uv_changed)
            self._uv_spins.append(spin)
            uv_layout.addWidget(spin, 1, i)

        uv_layout.addWidget(QLabel("Cullface:"), 2, 0)
        self._cullface_combo = QComboBox()
        for opt in CULLFACE_OPTIONS:
            self._cullface_combo.addItem(opt)
        self._cullface_combo.currentIndexChanged.connect(self._on_cullface_changed)
        uv_layout.addWidget(self._cullface_combo, 2, 1, 1, 3)

        self._uv_group.setVisible(False)
        layout.addWidget(self._uv_group)

        # File operations
        layout.addSpacing(8)
        save_btn = QPushButton("Save Model")
        save_btn.clicked.connect(self._save_model)
        layout.addWidget(save_btn)

        save_as_btn = QPushButton("Save As...")
        save_as_btn.clicked.connect(self._save_model_as)
        layout.addWidget(save_as_btn)

        new_btn = QPushButton("New Model")
        new_btn.setToolTip(
            "Create an editable model for blocks using a default cube parent.\n"
            "Note: You will need to update the YAML parent reference manually."
        )
        new_btn.clicked.connect(self._new_model)
        layout.addWidget(new_btn)

        self._yaml_note = QLabel(
            "Note: Creating a new model requires\n"
            "updating the YAML parent reference."
        )
        self._yaml_note.setStyleSheet("color: #a87; font-size: 10px;")
        self._yaml_note.setVisible(False)
        layout.addWidget(self._yaml_note)

        # Connect signals
        self.model.state_changed.connect(lambda _: self._refresh())
        self.model.geometry_changed.connect(self._refresh)

        self._refresh()

    def _get_model_data(self):
        return self.model.active_state.model_data

    def _refresh(self):
        self._updating = True
        md = self._get_model_data()

        # File label
        if md and md.source_path:
            self._file_label.setText(os.path.basename(md.source_path))
        elif md:
            self._file_label.setText("(unsaved)")
        else:
            self._file_label.setText("Default cube")

        # Element list
        self.elem_list.clear()
        if md:
            for i, elem in enumerate(md.elements):
                f = elem.from_pos
                t = elem.to_pos
                text = f"Element {i} ({f[0]},{f[1]},{f[2]}" \
                       + f" \u2192 {t[0]},{t[1]},{t[2]})"
                self.elem_list.addItem(text)

            if 0 <= self._selected_elem < len(md.elements):
                self.elem_list.setCurrentRow(self._selected_elem)
            elif md.elements:
                self._selected_elem = 0
                self.elem_list.setCurrentRow(0)
            else:
                self._selected_elem = -1
        else:
            self._selected_elem = -1

        self._sync_selected_element()
        has_selection = md is not None and 0 <= self._selected_elem < len(md.elements)
        self._dims_group.setVisible(has_selection)
        self._faces_group.setVisible(has_selection)
        self._uv_group.setVisible(has_selection)

        if has_selection:
            elem = md.elements[self._selected_elem]
            for i in range(3):
                self._from_spins[i].setValue(int(elem.from_pos[i]))
                self._to_spins[i].setValue(int(elem.to_pos[i]))
            for face in FACE_NAMES:
                self._face_checks[face].setChecked(face in elem.faces)
            self._refresh_uv()

        self._updating = False

    def _refresh_uv(self):
        md = self._get_model_data()
        if not md or not (0 <= self._selected_elem < len(md.elements)):
            return
        elem = md.elements[self._selected_elem]
        face = self._face_select_combo.currentData()
        if face and face in elem.faces:
            face_data = elem.faces[face]
            uv = face_data.get("uv", [0, 0, 16, 16])
            for i in range(4):
                self._uv_spins[i].setValue(int(uv[i]))
            cullface = face_data.get("cullface", "")
            idx = CULLFACE_OPTIONS.index(cullface) if cullface in CULLFACE_OPTIONS else 0
            self._cullface_combo.setCurrentIndex(idx)
            for spin in self._uv_spins:
                spin.setEnabled(True)
            self._cullface_combo.setEnabled(True)
        else:
            for spin in self._uv_spins:
                spin.setValue(0)
                spin.setEnabled(False)
            self._cullface_combo.setCurrentIndex(0)
            self._cullface_combo.setEnabled(False)

    def _on_elem_selected(self, row):
        if self._updating:
            return
        self._selected_elem = row
        self._updating = True
        md = self._get_model_data()
        has_selection = md is not None and 0 <= row < len(md.elements)
        self._dims_group.setVisible(has_selection)
        self._faces_group.setVisible(has_selection)
        self._uv_group.setVisible(has_selection)
        if has_selection:
            elem = md.elements[row]
            for i in range(3):
                self._from_spins[i].setValue(int(elem.from_pos[i]))
                self._to_spins[i].setValue(int(elem.to_pos[i]))
            for face in FACE_NAMES:
                self._face_checks[face].setChecked(face in elem.faces)
            self._refresh_uv()
        self._updating = False
        self._sync_selected_element()
        self.model.geometry_changed.emit()

    def _on_dims_changed(self):
        if self._updating:
            return
        md = self._get_model_data()
        if not md or not (0 <= self._selected_elem < len(md.elements)):
            return
        from_pos = [self._from_spins[i].value() for i in range(3)]
        to_pos = [self._to_spins[i].value() for i in range(3)]
        md.update_element(self._selected_elem, from_pos=from_pos, to_pos=to_pos)
        self._auto_update_uvs(md.elements[self._selected_elem])
        self._emit_geometry_changed()

    @staticmethod
    def _auto_update_uvs(elem):
        """Recalculate UVs from element dimensions, matching Minecraft's auto-UV."""
        fp = elem.from_pos
        tp = elem.to_pos
        uv_map = {
            "north": [16 - tp[0], 16 - tp[1], 16 - fp[0], 16 - fp[1]],
            "south": [fp[0], 16 - tp[1], tp[0], 16 - fp[1]],
            "east": [16 - tp[2], 16 - tp[1], 16 - fp[2], 16 - fp[1]],
            "west": [fp[2], 16 - tp[1], tp[2], 16 - fp[1]],
            "up": [fp[0], 16 - tp[2], tp[0], 16 - fp[2]],
            "down": [fp[0], fp[2], tp[0], tp[2]],
        }
        for face, uv in uv_map.items():
            if face in elem.faces:
                elem.faces[face]["uv"] = uv

    def _on_face_toggled(self, face, checked):
        if self._updating:
            return
        md = self._get_model_data()
        if not md or not (0 <= self._selected_elem < len(md.elements)):
            return
        md.set_face_visible(self._selected_elem, face, checked)
        self._refresh_uv()
        self._emit_geometry_changed()

    def _on_uv_face_changed(self):
        if self._updating:
            return
        self._updating = True
        self._refresh_uv()
        self._updating = False

    def _on_uv_changed(self):
        if self._updating:
            return
        md = self._get_model_data()
        if not md or not (0 <= self._selected_elem < len(md.elements)):
            return
        face = self._face_select_combo.currentData()
        if not face:
            return
        uv = [self._uv_spins[i].value() for i in range(4)]
        md.set_face_uv(self._selected_elem, face, uv)
        self._emit_geometry_changed()

    def _on_cullface_changed(self):
        if self._updating:
            return
        md = self._get_model_data()
        if not md or not (0 <= self._selected_elem < len(md.elements)):
            return
        face = self._face_select_combo.currentData()
        if not face:
            return
        idx = self._cullface_combo.currentIndex()
        cullface = CULLFACE_OPTIONS[idx] if idx > 0 else None
        md.set_face_cullface(self._selected_elem, face, cullface)

    def _add_element(self):
        md = self._get_model_data()
        if not md:
            return
        idx = md.add_element()
        self._selected_elem = idx
        self._emit_geometry_changed()

    def _delete_element(self):
        md = self._get_model_data()
        if not md or not (0 <= self._selected_elem < len(md.elements)):
            return
        md.remove_element(self._selected_elem)
        if self._selected_elem >= len(md.elements):
            self._selected_elem = len(md.elements) - 1
        self._emit_geometry_changed()

    def _duplicate_element(self):
        md = self._get_model_data()
        if not md or not (0 <= self._selected_elem < len(md.elements)):
            return
        new_idx = md.duplicate_element(self._selected_elem)
        if new_idx >= 0:
            self._selected_elem = new_idx
        self._emit_geometry_changed()

    def _sync_selected_element(self):
        self.model.selected_model_element = self._selected_elem

    def _emit_geometry_changed(self):
        self._sync_selected_element()
        self.model.geometry_changed.emit()
        self._refresh()

    def _save_model(self):
        md = self._get_model_data()
        if not md:
            QMessageBox.information(self, "Save Model", "No editable model to save.")
            return
        if not md.source_path:
            self._save_model_as()
            return
        data = md.to_json()
        with open(md.source_path, "w") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
        QMessageBox.information(
            self, "Save Model",
            f"Saved to {os.path.basename(md.source_path)}",
        )

    def _save_model_as(self):
        md = self._get_model_data()
        if not md:
            QMessageBox.information(self, "Save Model", "No editable model to save.")
            return
        start_dir = CUSTOM_MODELS_DIR
        if md.source_path:
            start_dir = os.path.dirname(md.source_path)
        filepath, _ = QFileDialog.getSaveFileName(
            self, "Save Model As",
            start_dir,
            "JSON Files (*.json)",
        )
        if not filepath:
            return
        if not filepath.endswith(".json"):
            filepath += ".json"
        md.source_path = filepath
        data = md.to_json()
        with open(filepath, "w") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
        self._file_label.setText(os.path.basename(filepath))

    def _new_model(self):
        state = self.model.active_state
        if state.model_data is not None:
            reply = QMessageBox.question(
                self, "New Model",
                "Replace the current model with a new default cube?",
                QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
            )
            if reply != QMessageBox.StandardButton.Yes:
                return
        md = ModelData(
            elements=[ModelElement()],
            textures={"particle": "#up"},
        )
        state.model_data = md
        self._selected_elem = 0
        self._yaml_note.setVisible(True)
        self._emit_geometry_changed()

    def model_undo(self):
        md = self._get_model_data()
        if md and md.undo():
            self._emit_geometry_changed()
            return True
        return False

    def model_redo(self):
        md = self._get_model_data()
        if md and md.redo():
            self._emit_geometry_changed()
            return True
        return False

    def get_selected_element_index(self):
        if self._wireframe_cb.isChecked():
            return self._selected_elem
        return -1

    def _on_wireframe_toggled(self, checked):
        self.model.geometry_changed.emit()
