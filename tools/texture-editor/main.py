#!/usr/bin/env python3
"""Atlas Texture Editor — edit block textures and preview them on a 3D cube."""

import sys
import os
import argparse

from PyQt6.QtWidgets import QApplication, QMainWindow, QSplitter, QVBoxLayout, QWidget
from PyQt6.QtCore import Qt
from PyQt6.QtGui import QKeySequence, QShortcut

from editor.texture_model import TextureModel
from editor.pixel_canvas import PixelCanvas
from editor.color_picker import ColorPicker
from editor.preview_3d import Preview3D
from editor.face_panel import FacePanel


class TextureEditor(QMainWindow):
    def __init__(self, size=32, block_id=None):
        super().__init__()
        self.setWindowTitle("Atlas Texture Editor")
        self.resize(1100, 650)

        self.model = TextureModel(size)

        # Central splitter: left (canvas + color picker) | right (3D preview)
        splitter = QSplitter(Qt.Orientation.Horizontal)

        # Left side: canvas + color picker
        left = QWidget()
        left_layout = QVBoxLayout(left)
        left_layout.setContentsMargins(4, 4, 4, 4)

        self.canvas = PixelCanvas(self.model)
        left_layout.addWidget(self.canvas, stretch=1)

        self.color_picker = ColorPicker(self.model)
        left_layout.addWidget(self.color_picker)

        splitter.addWidget(left)

        # Middle: 3D preview
        self.preview = Preview3D(self.model)
        splitter.addWidget(self.preview)

        # Right side: face panel
        self.face_panel = FacePanel(self.model)
        self.face_panel.setFixedWidth(280)
        splitter.addWidget(self.face_panel)

        splitter.setSizes([380, 380, 280])
        self.setCentralWidget(splitter)

        self._setup_shortcuts()

        # Load block if specified via CLI
        if block_id:
            self._load_initial_block(block_id)

    def _load_initial_block(self, block_id):
        """Load a block by ID at startup."""
        import yaml
        from editor.face_panel import CONFIG_DIR, DEFAULT_TEXTURE_DIR
        yaml_path = os.path.join(CONFIG_DIR, f"{block_id}.yml")
        if os.path.exists(yaml_path):
            with open(yaml_path, "r") as f:
                data = yaml.safe_load(f)
            if data:
                self.model.load_block_from_yaml(data, DEFAULT_TEXTURE_DIR)
                self.face_panel._refresh_state_combo()
                return
        self.model.load_block_by_filename(block_id, DEFAULT_TEXTURE_DIR)
        self.face_panel._refresh_state_combo()

    def _setup_shortcuts(self):
        # Ctrl+S: save current face
        save_sc = QShortcut(QKeySequence("Ctrl+S"), self)
        save_sc.activated.connect(self.face_panel._save_face)

        # Ctrl+Z: undo
        undo_sc = QShortcut(QKeySequence("Ctrl+Z"), self)
        undo_sc.activated.connect(self.model.undo)

        # Ctrl+Shift+Z: redo
        redo_sc = QShortcut(QKeySequence("Ctrl+Shift+Z"), self)
        redo_sc.activated.connect(self.model.redo)

        # 1-6: face selection
        for i in range(1, 7):
            sc = QShortcut(QKeySequence(str(i)), self)
            sc.activated.connect(lambda n=i: self.face_panel.select_face_by_number(n))

        # [ / ]: decrease / increase brush size
        dec_brush = QShortcut(QKeySequence("["), self)
        dec_brush.activated.connect(self._decrease_brush)
        inc_brush = QShortcut(QKeySequence("]"), self)
        inc_brush.activated.connect(self._increase_brush)

    def _decrease_brush(self):
        new_val = max(1, self.model.brush_size - 1)
        self.model.brush_size = new_val
        self.color_picker.brush_spin.setValue(new_val)

    def _increase_brush(self):
        new_val = min(32, self.model.brush_size + 1)
        self.model.brush_size = new_val
        self.color_picker.brush_spin.setValue(new_val)


def main():
    parser = argparse.ArgumentParser(description="Atlas Texture Editor")
    parser.add_argument(
        "--size", type=int, default=32, choices=[16, 32, 64, 128, 256, 512, 1024],
        help="Texture size in pixels (default: 32)",
    )
    parser.add_argument("--block", type=str, default=None, help="Block ID to load (e.g. fluid_pump)")
    args = parser.parse_args()

    app = QApplication(sys.argv)
    window = TextureEditor(size=args.size, block_id=args.block)
    window.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
