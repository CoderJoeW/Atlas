import os
import yaml
from PyQt6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QPushButton,
    QComboBox, QFileDialog, QLabel, QMessageBox, QCompleter,
)
from PyQt6.QtCore import Qt, QSortFilterProxyModel, QStringListModel

from editor.texture_model import FACE_NAMES

DEFAULT_TEXTURE_DIR = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..", "..", "..",
    "src", "main", "resources", "atlas", "resourcepack",
    "assets", "minecraft", "textures", "block", "custom",
))

CONFIG_DIR = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..", "..", "..",
    "src", "main", "resources", "atlas", "configuration",
))


TEXTURE_SIZES = [16, 32, 64, 128, 256, 512, 1024]


class FacePanel(QWidget):
    """State selector, face buttons, and file I/O controls."""

    def __init__(self, model, parent=None):
        super().__init__(parent)
        self.model = model
        self._face_buttons = {}
        self._texture_dir = DEFAULT_TEXTURE_DIR
        self._block_ids = []

        layout = QVBoxLayout(self)
        layout.setContentsMargins(4, 4, 4, 4)

        # Texture size selector
        size_row = QHBoxLayout()
        size_row.addWidget(QLabel("Size:"))
        self.size_combo = QComboBox()
        for s in TEXTURE_SIZES:
            self.size_combo.addItem(f"{s}x{s}", s)
        current_idx = TEXTURE_SIZES.index(self.model.size) if self.model.size in TEXTURE_SIZES else 1
        self.size_combo.setCurrentIndex(current_idx)
        self.size_combo.currentIndexChanged.connect(self._on_size_changed)
        size_row.addWidget(self.size_combo, stretch=1)
        layout.addLayout(size_row)

        # Block loader with searchable dropdown
        layout.addSpacing(4)
        layout.addWidget(QLabel("Block:"))
        self.block_combo = QComboBox()
        self.block_combo.setEditable(True)
        self.block_combo.setInsertPolicy(QComboBox.InsertPolicy.NoInsert)
        self.block_combo.setPlaceholderText("Search blocks...")
        self.block_combo.setCurrentIndex(-1)
        self._populate_block_list()
        completer = self.block_combo.completer()
        if completer:
            completer.setFilterMode(Qt.MatchFlag.MatchContains)
            completer.setCaseSensitivity(Qt.CaseSensitivity.CaseInsensitive)

        load_block_btn = QPushButton("Load Block")
        load_block_btn.clicked.connect(self._load_block)
        block_row = QHBoxLayout()
        block_row.addWidget(self.block_combo, stretch=1)
        block_row.addWidget(load_block_btn)
        layout.addLayout(block_row)

        # State selector
        layout.addSpacing(8)
        state_row = QHBoxLayout()
        state_row.addWidget(QLabel("State:"))
        self.state_combo = QComboBox()
        self.state_combo.addItem(self.model.states[0].name)
        self.state_combo.currentIndexChanged.connect(self._on_state_selected)
        state_row.addWidget(self.state_combo, stretch=1)
        layout.addLayout(state_row)

        # Face buttons
        layout.addWidget(QLabel("Face:"))
        face_grid = QVBoxLayout()
        row1 = QHBoxLayout()
        row2 = QHBoxLayout()
        for i, face in enumerate(FACE_NAMES):
            btn = QPushButton(face.capitalize())
            btn.setCheckable(True)
            btn.setFixedWidth(70)
            btn.clicked.connect(lambda checked, f=face: self._on_face_clicked(f))
            self._face_buttons[face] = btn
            if i < 3:
                row1.addWidget(btn)
            else:
                row2.addWidget(btn)
        face_grid.addLayout(row1)
        face_grid.addLayout(row2)
        layout.addLayout(face_grid)
        self._face_buttons[self.model.active_face].setChecked(True)

        # File I/O
        layout.addSpacing(8)

        open_btn = QPushButton("Open Face...")
        open_btn.clicked.connect(self._open_face)
        layout.addWidget(open_btn)

        save_btn = QPushButton("Save Face...")
        save_btn.clicked.connect(self._save_face)
        layout.addWidget(save_btn)

        save_all_btn = QPushButton("Save All...")
        save_all_btn.clicked.connect(self._save_all)
        layout.addWidget(save_all_btn)

        layout.addStretch()

        self.model.state_changed.connect(self._sync_state_combo)

    def _on_size_changed(self, index):
        new_size = self.size_combo.currentData()
        if new_size and new_size != self.model.size:
            self.model.resize_all(new_size)

    def _populate_block_list(self):
        """Discover available blocks from YAML configs and texture filenames."""
        block_ids = set()

        # From YAML config files (primary source of truth)
        if os.path.isdir(CONFIG_DIR):
            for fname in os.listdir(CONFIG_DIR):
                if fname.endswith(".yml"):
                    block_ids.add(fname[:-4])

        # From texture filenames — only add blocks with a clear multi-face layout
        if os.path.isdir(self._texture_dir):
            texture_names = set()
            for fname in os.listdir(self._texture_dir):
                if fname.endswith(".png"):
                    texture_names.add(fname[:-4])

            candidates = set()
            for name in texture_names:
                for suffix in ("_top", "_bottom", "_side",
                               "_north", "_south", "_east", "_west"):
                    if name.endswith(suffix):
                        candidates.add(name[:-len(suffix)])

            for candidate in candidates:
                if candidate in block_ids:
                    continue
                # Skip if this candidate is a sub-texture of an already-known block
                # e.g. "conveyor_belt_top" is a face of "conveyor_belt"
                is_sub_texture = any(
                    candidate.startswith(bid + "_") for bid in block_ids
                )
                if is_sub_texture:
                    continue
                # cube_bottom_top: needs all three of _top, _bottom, _side
                has_cbt = (f"{candidate}_top" in texture_names
                           and f"{candidate}_bottom" in texture_names
                           and f"{candidate}_side" in texture_names)
                # cube: needs 3+ directional faces
                dir_count = sum(
                    1 for f in ("north", "south", "east", "west")
                    if f"{candidate}_{f}" in texture_names
                )
                if has_cbt or dir_count >= 3:
                    block_ids.add(candidate)

        self._block_ids = sorted(block_ids)
        self.block_combo.clear()
        for bid in self._block_ids:
            self.block_combo.addItem(bid)

    def _on_face_clicked(self, face):
        for f, btn in self._face_buttons.items():
            btn.setChecked(f == face)
        self.model.set_active_face(face)

    def _on_state_selected(self, index):
        if 0 <= index < len(self.model.states):
            self.model.set_active_state(index)

    def _sync_state_combo(self, index):
        self.state_combo.blockSignals(True)
        self.state_combo.setCurrentIndex(index)
        self.state_combo.blockSignals(False)

    def _refresh_state_combo(self):
        self.state_combo.blockSignals(True)
        self.state_combo.clear()
        for state in self.model.states:
            self.state_combo.addItem(state.name)
        self.state_combo.setCurrentIndex(self.model.active_state_index)
        self.state_combo.blockSignals(False)

    def _load_block(self):
        block_id = self.block_combo.currentText().strip()
        if not block_id:
            return

        # Try YAML config first
        yaml_path = os.path.join(CONFIG_DIR, f"{block_id}.yml")
        if os.path.exists(yaml_path):
            with open(yaml_path, "r") as f:
                data = yaml.safe_load(f)
            if data:
                self.model.load_block_from_yaml(data, self._texture_dir)
                self._refresh_state_combo()
                self._sync_size_combo()
                return

        # Fallback to filename scanning
        loaded = self.model.load_block_by_filename(block_id, self._texture_dir)
        self._refresh_state_combo()
        self._sync_size_combo()
        if not loaded:
            QMessageBox.warning(
                self, "Not Found",
                f"No textures found for block '{block_id}'.",
            )

    def _sync_size_combo(self):
        """Update the size dropdown to reflect the model's current texture size."""
        self.size_combo.blockSignals(True)
        if self.model.size in TEXTURE_SIZES:
            self.size_combo.setCurrentIndex(TEXTURE_SIZES.index(self.model.size))
        self.size_combo.blockSignals(False)

    def _open_face(self):
        filepath, _ = QFileDialog.getOpenFileName(
            self, "Open Texture",
            self._texture_dir,
            "PNG Images (*.png)",
        )
        if filepath:
            self.model.load_face(self.model.active_face, filepath)

    def _save_face(self):
        filepath, _ = QFileDialog.getSaveFileName(
            self, "Save Texture",
            self._texture_dir,
            "PNG Images (*.png)",
        )
        if filepath:
            if not filepath.endswith(".png"):
                filepath += ".png"
            self.model.save_face(self.model.active_face, filepath)

    def _save_all(self):
        directory = QFileDialog.getExistingDirectory(
            self, "Save All Textures To",
            self._texture_dir,
        )
        if not directory:
            return
        for state in self.model.states:
            for face in FACE_NAMES:
                filename = f"{state.name}_{face}.png"
                filepath = os.path.join(directory, filename)
                state.faces[face].save(filepath, "PNG")

    def select_face_by_number(self, num):
        """Select face by number 1-6."""
        if 1 <= num <= 6:
            face = FACE_NAMES[num - 1]
            self._on_face_clicked(face)
