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
from editor.tile_preview import TilePreview
from editor.layer_panel import LayerPanel
from editor.tools import ALL_TOOLS, ACTION_TOOLS, DragShapeTool, GradientTool


class TextureEditor(QMainWindow):
    def __init__(self, size=32, block_id=None):
        super().__init__()
        self.setWindowTitle("Atlas Texture Editor")
        self.resize(1400, 850)

        self.model = TextureModel(size)

        # Central splitter: left (canvas + color picker) | middle (3D + tile) | right
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

        # Middle: 3D preview + tiling preview
        middle = QWidget()
        middle_layout = QVBoxLayout(middle)
        middle_layout.setContentsMargins(0, 0, 0, 0)

        self.preview = Preview3D(self.model)
        middle_layout.addWidget(self.preview, stretch=2)

        self.tile_preview = TilePreview(self.model)
        middle_layout.addWidget(self.tile_preview, stretch=1)

        splitter.addWidget(middle)

        # Right side: face panel + layer panel
        right = QWidget()
        right_layout = QVBoxLayout(right)
        right_layout.setContentsMargins(0, 0, 0, 0)
        right_layout.setSpacing(0)

        self.face_panel = FacePanel(self.model)
        right_layout.addWidget(self.face_panel)

        self.layer_panel = LayerPanel(self.model)
        right_layout.addWidget(self.layer_panel)

        right.setFixedWidth(280)
        splitter.addWidget(right)

        splitter.setSizes([500, 420, 280])
        self.setCentralWidget(splitter)

        # Tool system
        self._tools = {}
        for tool_cls in ALL_TOOLS:
            tool = tool_cls(self.model)
            self._tools[tool.name] = tool
        self.canvas.set_tool(self._tools["Brush"])

        # One-shot action tools (eyedropper, fill)
        self._action_tools = {}
        for name, cls in ACTION_TOOLS.items():
            self._action_tools[name] = cls(self.model)

        self.color_picker.tool_changed.connect(self._on_tool_changed)
        self.color_picker.filled_changed.connect(self._on_filled_changed)
        self.color_picker.end_color_changed.connect(self._on_end_color_changed)
        self.color_picker.eyedropper_clicked.connect(
            lambda: self._activate_action("Eyedropper"))
        self.color_picker.fill_clicked.connect(
            lambda: self._activate_action("Fill"))

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

    def _on_tool_changed(self, tool_name):
        tool = self._tools.get(tool_name)
        if tool:
            self.canvas.set_tool(tool)
            # Show/hide filled checkbox
            self.color_picker.set_filled_visible(isinstance(tool, DragShapeTool)
                                                  and not isinstance(tool, GradientTool))
            # Show/hide gradient end color
            self.color_picker.set_end_color_visible(isinstance(tool, GradientTool))

    def _on_filled_changed(self, filled):
        tool = self.canvas._active_tool
        if isinstance(tool, DragShapeTool):
            tool.filled = filled

    def _on_end_color_changed(self, color):
        tool = self._tools.get("Gradient")
        if isinstance(tool, GradientTool):
            tool.end_color = color

    def _activate_action(self, name):
        tool = self._action_tools.get(name)
        if tool:
            self.canvas.activate_one_shot(tool)

    def _select_tool(self, name):
        if name in self._action_tools:
            self._activate_action(name)
            return
        idx = self.color_picker.tool_combo.findText(name)
        if idx >= 0:
            self.color_picker.tool_combo.setCurrentIndex(idx)

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

        # Tool shortcuts
        for key, name in [("B", "Brush"), ("F", "Fill"), ("I", "Eyedropper"),
                          ("L", "Line"), ("R", "Rectangle"),
                          ("E", "Ellipse"), ("G", "Gradient"), ("M", "Selection")]:
            sc = QShortcut(QKeySequence(key), self)
            sc.activated.connect(lambda n=name: self._select_tool(n))

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
