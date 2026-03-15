import json
import os
from PIL import Image
from PyQt6.QtCore import QObject, pyqtSignal

FACE_NAMES = ["north", "south", "east", "west", "up", "down"]

# Maps CraftEngine parent model texture keys to face names
PARENT_MODEL_MAPPINGS = {
    "cube_bottom_top": {
        "top": "up",
        "bottom": "down",
        "side": ["north", "south", "east", "west"],
    },
    "cube": {
        "north": "north",
        "south": "south",
        "east": "east",
        "west": "west",
        "up": "up",
        "down": "down",
    },
    "cube_all": {
        "all": FACE_NAMES,
    },
}

MODELS_DIR = os.path.normpath(os.path.join(
    os.path.dirname(__file__), "..", "..", "..",
    "src", "main", "resources", "atlas", "resourcepack",
    "assets", "minecraft", "models",
))

# Default cube geometry: one element, full 16x16x16 cube
DEFAULT_GEOMETRY = [{
    "faces": {
        "north": {
            "texture_key": "north",
            "vertices": [
                (-0.5, -0.5, -0.5), (0.5, -0.5, -0.5),
                (0.5, 0.5, -0.5), (-0.5, 0.5, -0.5),
            ],
            "tex_coords": [(0, 0), (1, 0), (1, 1), (0, 1)],
            "normal": (0, 0, -1),
        },
        "south": {
            "texture_key": "south",
            "vertices": [
                (0.5, -0.5, 0.5), (-0.5, -0.5, 0.5),
                (-0.5, 0.5, 0.5), (0.5, 0.5, 0.5),
            ],
            "tex_coords": [(0, 0), (1, 0), (1, 1), (0, 1)],
            "normal": (0, 0, 1),
        },
        "east": {
            "texture_key": "east",
            "vertices": [
                (0.5, -0.5, -0.5), (0.5, -0.5, 0.5),
                (0.5, 0.5, 0.5), (0.5, 0.5, -0.5),
            ],
            "tex_coords": [(0, 0), (1, 0), (1, 1), (0, 1)],
            "normal": (1, 0, 0),
        },
        "west": {
            "texture_key": "west",
            "vertices": [
                (-0.5, -0.5, 0.5), (-0.5, -0.5, -0.5),
                (-0.5, 0.5, -0.5), (-0.5, 0.5, 0.5),
            ],
            "tex_coords": [(0, 0), (1, 0), (1, 1), (0, 1)],
            "normal": (-1, 0, 0),
        },
        "up": {
            "texture_key": "up",
            "vertices": [
                (-0.5, 0.5, -0.5), (0.5, 0.5, -0.5),
                (0.5, 0.5, 0.5), (-0.5, 0.5, 0.5),
            ],
            "tex_coords": [(0, 0), (1, 0), (1, 1), (0, 1)],
            "normal": (0, 1, 0),
        },
        "down": {
            "texture_key": "down",
            "vertices": [
                (-0.5, -0.5, 0.5), (0.5, -0.5, 0.5),
                (0.5, -0.5, -0.5), (-0.5, -0.5, -0.5),
            ],
            "tex_coords": [(0, 0), (1, 0), (1, 1), (0, 1)],
            "normal": (0, -1, 0),
        },
    },
}]


def parse_model_json(model_ref):
    """Load and parse a Minecraft block model JSON from a resource reference.

    model_ref: e.g. 'minecraft:block/custom/conveyor_belt_base'
    Returns parsed geometry list, or None if not found.
    """
    if ":" in model_ref:
        _, path = model_ref.split(":", 1)
    else:
        path = model_ref
    json_path = os.path.join(MODELS_DIR, path + ".json")
    if not os.path.exists(json_path):
        return None

    with open(json_path, "r") as f:
        model_data = json.load(f)

    elements = model_data.get("elements", [])
    if not elements:
        return None

    geometry = []
    for element in elements:
        from_pos = element["from"]
        to_pos = element["to"]

        # Normalize from 0-16 Minecraft coords to -0.5..0.5 GL coords
        x1 = from_pos[0] / 16.0 - 0.5
        y1 = from_pos[1] / 16.0 - 0.5
        z1 = from_pos[2] / 16.0 - 0.5
        x2 = to_pos[0] / 16.0 - 0.5
        y2 = to_pos[1] / 16.0 - 0.5
        z2 = to_pos[2] / 16.0 - 0.5

        faces = {}
        for face_dir, face_data in element.get("faces", {}).items():
            # Resolve texture variable name → texture key
            tex_ref = face_data.get("texture", "")
            tex_key = tex_ref.lstrip("#") if tex_ref.startswith("#") else face_dir

            # UV: explicit or default to full texture
            if "uv" in face_data:
                mu1, mv1, mu2, mv2 = face_data["uv"]
            else:
                mu1, mv1, mu2, mv2 = 0, 0, 16, 16

            # Normalize UV to 0..1, flip v (Minecraft is top-down, GL is bottom-up)
            nu1 = mu1 / 16.0
            nu2 = mu2 / 16.0
            gl_v_bottom = 1.0 - mv2 / 16.0
            gl_v_top = 1.0 - mv1 / 16.0
            tc = [
                (nu1, gl_v_bottom), (nu2, gl_v_bottom),
                (nu2, gl_v_top), (nu1, gl_v_top),
            ]

            # Vertices: 4 corners of the quad (BL, BR, TR, TL from outside)
            if face_dir == "north":
                verts = [
                    (x1, y1, z1), (x2, y1, z1), (x2, y2, z1), (x1, y2, z1),
                ]
                normal = (0, 0, -1)
            elif face_dir == "south":
                verts = [
                    (x2, y1, z2), (x1, y1, z2), (x1, y2, z2), (x2, y2, z2),
                ]
                normal = (0, 0, 1)
            elif face_dir == "east":
                verts = [
                    (x2, y1, z1), (x2, y1, z2), (x2, y2, z2), (x2, y2, z1),
                ]
                normal = (1, 0, 0)
            elif face_dir == "west":
                verts = [
                    (x1, y1, z2), (x1, y1, z1), (x1, y2, z1), (x1, y2, z2),
                ]
                normal = (-1, 0, 0)
            elif face_dir == "up":
                verts = [
                    (x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2),
                ]
                normal = (0, 1, 0)
            elif face_dir == "down":
                verts = [
                    (x1, y1, z2), (x2, y1, z2), (x2, y1, z1), (x1, y1, z1),
                ]
                normal = (0, -1, 0)
            else:
                continue

            faces[face_dir] = {
                "texture_key": tex_key,
                "vertices": verts,
                "tex_coords": tc,
                "normal": normal,
            }

        geometry.append({"faces": faces})

    return geometry


class BlockState:
    """A single block state containing 6 face textures and optional geometry."""

    def __init__(self, name, size=32):
        self.name = name
        self.geometry = None  # None = default cube, otherwise parsed model
        self.faces = {}
        for face in FACE_NAMES:
            self.faces[face] = Image.new("RGBA", (size, size), (200, 200, 200, 255))


class TextureModel(QObject):
    """Shared data model holding multi-state block textures."""

    face_updated = pyqtSignal(str)
    state_changed = pyqtSignal(int)
    color_changed = pyqtSignal()

    def __init__(self, size=32):
        super().__init__()
        self.size = size
        self.states = [BlockState("default", size)]
        self.active_state_index = 0
        self.active_face = "north"
        self.current_color = (0, 0, 0, 255)
        self.brush_size = 1
        self._undo_stack = []
        self._redo_stack = []
        self._stroke_snapshot = None

    @property
    def active_state(self):
        return self.states[self.active_state_index]

    def get_geometry(self):
        """Return the active state's geometry, or the default cube."""
        geo = self.active_state.geometry
        return geo if geo is not None else DEFAULT_GEOMETRY

    def get_image(self, face=None, state_index=None):
        if state_index is None:
            state_index = self.active_state_index
        if face is None:
            face = self.active_face
        return self.states[state_index].faces[face]

    def set_active_face(self, face):
        if face in FACE_NAMES:
            self.active_face = face
            self.face_updated.emit(face)

    def set_active_state(self, index):
        if 0 <= index < len(self.states):
            self.active_state_index = index
            self.state_changed.emit(index)
            self.face_updated.emit(self.active_face)

    def add_state(self, name):
        self.states.append(BlockState(name, self.size))

    def resize_all(self, new_size):
        """Resize all face textures across all states to a new size."""
        self.size = new_size
        for state in self.states:
            for face in FACE_NAMES:
                old_img = state.faces[face]
                if old_img.width == new_size and old_img.height == new_size:
                    continue
                state.faces[face] = old_img.resize(
                    (new_size, new_size), Image.Resampling.NEAREST,
                )
        self._undo_stack.clear()
        self._redo_stack.clear()
        self.state_changed.emit(self.active_state_index)
        self.face_updated.emit(self.active_face)

    def begin_stroke(self):
        """Save a snapshot before a paint stroke begins."""
        self._stroke_snapshot = self.get_image().copy()

    def end_stroke(self):
        """Finalize a paint stroke, pushing the snapshot to undo stack."""
        if self._stroke_snapshot is not None:
            self._undo_stack.append((
                self.active_state_index,
                self.active_face,
                self._stroke_snapshot,
            ))
            if len(self._undo_stack) > 20:
                self._undo_stack.pop(0)
            self._redo_stack.clear()
            self._stroke_snapshot = None

    def undo(self):
        if not self._undo_stack:
            return
        state_idx, face, snapshot = self._undo_stack.pop()
        current = self.states[state_idx].faces[face].copy()
        self._redo_stack.append((state_idx, face, current))
        self.states[state_idx].faces[face] = snapshot
        if state_idx == self.active_state_index:
            self.face_updated.emit(face)

    def redo(self):
        if not self._redo_stack:
            return
        state_idx, face, snapshot = self._redo_stack.pop()
        current = self.states[state_idx].faces[face].copy()
        self._undo_stack.append((state_idx, face, current))
        self.states[state_idx].faces[face] = snapshot
        if state_idx == self.active_state_index:
            self.face_updated.emit(face)

    def set_pixel(self, x, y, emit=True):
        img = self.get_image()
        if 0 <= x < img.width and 0 <= y < img.height:
            img.putpixel((x, y), self.current_color)
            if emit:
                self.face_updated.emit(self.active_face)

    def paint_brush(self, cx, cy, brush_size):
        """Paint a brush_size square centered on (cx, cy). Emits once."""
        img = self.get_image()
        offset = brush_size // 2
        changed = False
        for dy in range(brush_size):
            for dx in range(brush_size):
                px = cx - offset + dx
                py = cy - offset + dy
                if 0 <= px < img.width and 0 <= py < img.height:
                    img.putpixel((px, py), self.current_color)
                    changed = True
        if changed:
            self.face_updated.emit(self.active_face)

    def get_pixel(self, x, y):
        img = self.get_image()
        if 0 <= x < img.width and 0 <= y < img.height:
            return img.getpixel((x, y))
        return None

    def load_face(self, face, filepath):
        img = Image.open(filepath).convert("RGBA")
        self.active_state.faces[face] = img
        if self.size != img.width:
            self.size = img.width
        self.face_updated.emit(face)

    def save_face(self, face, filepath):
        self.active_state.faces[face].save(filepath, "PNG")

    def _load_state_from_generation(self, state_name, generation, texture_dir):
        """Build a BlockState from a CraftEngine generation block."""
        parent = generation.get("parent", "")
        textures = generation.get("textures", {})

        parent_type = None
        for ptype in PARENT_MODEL_MAPPINGS:
            if ptype in parent:
                parent_type = ptype
                break

        # If no known parent matched but textures use standard face keys,
        # treat it as a cube-style mapping (e.g. custom parent models)
        if not parent_type and textures:
            cube_keys = set(PARENT_MODEL_MAPPINGS["cube"].keys())
            if any(k in cube_keys for k in textures):
                parent_type = "cube"

        if not parent_type or not textures:
            return None

        block_state = BlockState(state_name, self.size)
        mapping = PARENT_MODEL_MAPPINGS[parent_type]

        for tex_key, tex_path in textures.items():
            if tex_key not in mapping:
                continue
            filename = tex_path.split("/")[-1] + ".png"
            filepath = os.path.join(texture_dir, filename)
            if os.path.exists(filepath):
                img = Image.open(filepath).convert("RGBA")
                if self.size != img.width:
                    self.size = img.width
                target_faces = mapping[tex_key]
                if isinstance(target_faces, str):
                    target_faces = [target_faces]
                for face in target_faces:
                    block_state.faces[face] = img.copy()

        # Try parsing the parent as a custom model JSON for geometry
        if parent_type == "cube" and parent:
            parsed = parse_model_json(parent)
            if parsed:
                block_state.geometry = parsed

        return block_state

    def load_block_from_yaml(self, yaml_data, texture_dir):
        """Load all states from parsed CraftEngine YAML config."""
        self.states.clear()
        self.active_state_index = 0

        sections = []
        for key in yaml_data:
            if key.startswith("items"):
                sections.append((key, yaml_data[key]))

        # Sort so 'items' comes first, then 'items#1', 'items#2', etc.
        sections.sort(key=lambda x: (x[0] != "items", x[0]))

        for section_key, section_data in sections:
            for item_id, item_config in section_data.items():
                state_name = item_id.split(":")[-1] if ":" in item_id else item_id
                behavior = item_config.get("behavior", {})
                block = behavior.get("block", {})

                # Format 1: simple single-state — state.model.generation
                state_section = block.get("state", {})
                model = state_section.get("model", {})
                generation = model.get("generation", {})
                if generation:
                    bs = self._load_state_from_generation(
                        state_name, generation, texture_dir,
                    )
                    if bs:
                        self.states.append(bs)
                    continue

                # Format 2: directional / multi-appearance — states.appearances
                states_section = block.get("states", {})
                appearances = states_section.get("appearances", {})
                if appearances:
                    for app_name, app_data in appearances.items():
                        app_model = app_data.get("model", {})
                        app_gen = app_model.get("generation", {})
                        if not app_gen:
                            continue
                        display_name = f"{state_name} ({app_name})"
                        bs = self._load_state_from_generation(
                            display_name, app_gen, texture_dir,
                        )
                        if bs:
                            self.states.append(bs)

        if not self.states:
            self.states.append(BlockState("default", self.size))

        self.active_state_index = 0
        self.active_face = "north"
        self.state_changed.emit(0)
        self.face_updated.emit(self.active_face)

    def load_block_by_filename(self, block_id, texture_dir):
        """Fallback: discover textures by filename pattern."""
        self.states.clear()
        self.active_state_index = 0

        block_state = BlockState(block_id, self.size)
        loaded_any = False

        # Try cube_bottom_top layout: {id}_top, {id}_bottom, {id}_side
        top_path = os.path.join(texture_dir, f"{block_id}_top.png")
        bottom_path = os.path.join(texture_dir, f"{block_id}_bottom.png")
        side_path = os.path.join(texture_dir, f"{block_id}_side.png")

        if (os.path.exists(top_path) and os.path.exists(bottom_path)
                and os.path.exists(side_path)):
            top_img = Image.open(top_path).convert("RGBA")
            bottom_img = Image.open(bottom_path).convert("RGBA")
            side_img = Image.open(side_path).convert("RGBA")
            self.size = top_img.width
            block_state = BlockState(block_id, self.size)
            block_state.faces["up"] = top_img
            block_state.faces["down"] = bottom_img
            for face in ["north", "south", "east", "west"]:
                block_state.faces[face] = side_img.copy()
            loaded_any = True
        else:
            # Try cube layout: {id}_north, {id}_south, etc.
            face_map = {}
            for face in FACE_NAMES:
                path = os.path.join(texture_dir, f"{block_id}_{face}.png")
                if os.path.exists(path):
                    face_map[face] = path

            if face_map:
                first_img = Image.open(list(face_map.values())[0]).convert("RGBA")
                self.size = first_img.width
                block_state = BlockState(block_id, self.size)
                for face, path in face_map.items():
                    block_state.faces[face] = Image.open(path).convert("RGBA")
                loaded_any = True
            else:
                # Try cube_all layout: just {id}.png
                all_path = os.path.join(texture_dir, f"{block_id}.png")
                if os.path.exists(all_path):
                    img = Image.open(all_path).convert("RGBA")
                    self.size = img.width
                    block_state = BlockState(block_id, self.size)
                    for face in FACE_NAMES:
                        block_state.faces[face] = img.copy()
                    loaded_any = True

        self.states.append(block_state)
        if not loaded_any:
            self.states[0] = BlockState("default", self.size)

        self.active_state_index = 0
        self.active_face = "north"
        self.state_changed.emit(0)
        self.face_updated.emit(self.active_face)
        return loaded_any
