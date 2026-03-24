import copy
import json
import os
from collections import deque
from PIL import Image
from PyQt6.QtCore import QObject, pyqtSignal

FACE_NAMES = ["north", "south", "east", "west", "up", "down"]

SYMMETRY_NONE = "none"
SYMMETRY_HORIZONTAL = "horizontal"
SYMMETRY_VERTICAL = "vertical"
SYMMETRY_QUAD = "quad"
SYMMETRY_MODES = [SYMMETRY_NONE, SYMMETRY_HORIZONTAL, SYMMETRY_VERTICAL, SYMMETRY_QUAD]

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


def _element_to_geometry(from_pos, to_pos, faces_dict):
    """Convert a single element's Minecraft coords to GL render geometry.

    from_pos/to_pos: [x, y, z] in 0-16 space.
    faces_dict: {face_name: {"texture": "#x", "uv": [...], "cullface": ...}}
    Returns a geometry dict with "faces" key.
    """
    x1 = from_pos[0] / 16.0 - 0.5
    y1 = from_pos[1] / 16.0 - 0.5
    z1 = from_pos[2] / 16.0 - 0.5
    x2 = to_pos[0] / 16.0 - 0.5
    y2 = to_pos[1] / 16.0 - 0.5
    z2 = to_pos[2] / 16.0 - 0.5

    face_defs = {
        "north": ((x1, y1, z1), (x2, y1, z1), (x2, y2, z1), (x1, y2, z1), (0, 0, -1)),
        "south": ((x2, y1, z2), (x1, y1, z2), (x1, y2, z2), (x2, y2, z2), (0, 0, 1)),
        "east": ((x2, y1, z1), (x2, y1, z2), (x2, y2, z2), (x2, y2, z1), (1, 0, 0)),
        "west": ((x1, y1, z2), (x1, y1, z1), (x1, y2, z1), (x1, y2, z2), (-1, 0, 0)),
        "up": ((x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2), (0, 1, 0)),
        "down": ((x1, y1, z2), (x2, y1, z2), (x2, y1, z1), (x1, y1, z1), (0, -1, 0)),
    }

    faces = {}
    for face_dir, face_data in faces_dict.items():
        if face_dir not in face_defs:
            continue
        v0, v1, v2, v3, normal = face_defs[face_dir]

        tex_ref = face_data.get("texture", "")
        tex_key = tex_ref.lstrip("#") if tex_ref.startswith("#") else face_dir

        if "uv" in face_data:
            mu1, mv1, mu2, mv2 = face_data["uv"]
        else:
            mu1, mv1, mu2, mv2 = 0, 0, 16, 16

        nu1 = mu1 / 16.0
        nu2 = mu2 / 16.0
        gl_v_bottom = 1.0 - mv2 / 16.0
        gl_v_top = 1.0 - mv1 / 16.0
        tc = [
            (nu1, gl_v_bottom), (nu2, gl_v_bottom),
            (nu2, gl_v_top), (nu1, gl_v_top),
        ]

        faces[face_dir] = {
            "texture_key": tex_key,
            "vertices": [v0, v1, v2, v3],
            "tex_coords": tc,
            "normal": normal,
        }

    return {"faces": faces}


class ModelElement:
    """One axis-aligned cuboid element in Minecraft 0-16 coordinate space."""

    def __init__(self, from_pos=None, to_pos=None, faces=None):
        self.from_pos = list(from_pos) if from_pos else [0, 0, 0]
        self.to_pos = list(to_pos) if to_pos else [16, 16, 16]
        if faces is not None:
            self.faces = faces
        else:
            self.faces = {}
            for face in FACE_NAMES:
                self.faces[face] = {
                    "texture": f"#{face}",
                    "uv": [0, 0, 16, 16],
                }

    def to_dict(self):
        """Serialize to Minecraft model JSON element format."""
        result = {
            "from": list(self.from_pos),
            "to": list(self.to_pos),
            "faces": {},
        }
        for face_name, face_data in self.faces.items():
            entry = {"texture": face_data.get("texture", f"#{face_name}")}
            uv = face_data.get("uv")
            if uv and uv != [0, 0, 16, 16]:
                entry["uv"] = list(uv)
            cullface = face_data.get("cullface")
            if cullface:
                entry["cullface"] = cullface
            result["faces"][face_name] = entry
        return result

    def to_geometry(self):
        """Convert this element to GL render geometry."""
        return _element_to_geometry(self.from_pos, self.to_pos, self.faces)


class ModelData:
    """Editable block model in Minecraft JSON format — source of truth for geometry."""

    def __init__(self, elements=None, textures=None, source_path=None):
        self.elements = elements if elements else []
        self.textures = textures if textures else {}
        self.source_path = source_path
        self._undo_stack = []
        self._redo_stack = []

    def to_geometry(self):
        """Convert all elements to render-format geometry list."""
        return [elem.to_geometry() for elem in self.elements]

    def to_json(self):
        """Serialize to Minecraft model JSON dict."""
        result = {}
        if self.textures:
            result["textures"] = dict(self.textures)
        result["elements"] = [elem.to_dict() for elem in self.elements]
        return result

    @staticmethod
    def from_json(data, source_path=None):
        """Parse a Minecraft model JSON dict into a ModelData instance."""
        textures = data.get("textures", {})
        elements = []
        for elem_data in data.get("elements", []):
            faces = {}
            for face_name, face_info in elem_data.get("faces", {}).items():
                faces[face_name] = {
                    "texture": face_info.get("texture", f"#{face_name}"),
                    "uv": list(face_info.get("uv", [0, 0, 16, 16])),
                }
                if "cullface" in face_info:
                    faces[face_name]["cullface"] = face_info["cullface"]
            elements.append(ModelElement(
                from_pos=elem_data.get("from", [0, 0, 0]),
                to_pos=elem_data.get("to", [16, 16, 16]),
                faces=faces,
            ))
        return ModelData(elements=elements, textures=textures, source_path=source_path)

    # ------------------------------------------------------------------
    # Snapshot-based undo / redo
    # ------------------------------------------------------------------

    def _push_undo(self):
        snapshot = copy.deepcopy(self.elements)
        self._undo_stack.append(snapshot)
        if len(self._undo_stack) > 50:
            self._undo_stack.pop(0)
        self._redo_stack.clear()

    def undo(self):
        if not self._undo_stack:
            return False
        self._redo_stack.append(copy.deepcopy(self.elements))
        self.elements = self._undo_stack.pop()
        return True

    def redo(self):
        if not self._redo_stack:
            return False
        self._undo_stack.append(copy.deepcopy(self.elements))
        self.elements = self._redo_stack.pop()
        return True

    # ------------------------------------------------------------------
    # Element mutations (all push undo before mutating)
    # ------------------------------------------------------------------

    def add_element(self, from_pos=None, to_pos=None):
        self._push_undo()
        elem = ModelElement(from_pos=from_pos, to_pos=to_pos)
        self.elements.append(elem)
        return len(self.elements) - 1

    def remove_element(self, idx):
        if 0 <= idx < len(self.elements):
            self._push_undo()
            self.elements.pop(idx)

    def duplicate_element(self, idx):
        if 0 <= idx < len(self.elements):
            self._push_undo()
            elem = copy.deepcopy(self.elements[idx])
            self.elements.insert(idx + 1, elem)
            return idx + 1
        return -1

    def update_element(self, idx, from_pos=None, to_pos=None):
        if 0 <= idx < len(self.elements):
            self._push_undo()
            if from_pos is not None:
                self.elements[idx].from_pos = list(from_pos)
            if to_pos is not None:
                self.elements[idx].to_pos = list(to_pos)

    def set_face_visible(self, elem_idx, face, visible):
        if not (0 <= elem_idx < len(self.elements)):
            return
        self._push_undo()
        elem = self.elements[elem_idx]
        if visible:
            if face not in elem.faces:
                elem.faces[face] = {
                    "texture": f"#{face}",
                    "uv": [0, 0, 16, 16],
                }
        else:
            elem.faces.pop(face, None)

    def set_face_uv(self, elem_idx, face, uv):
        if not (0 <= elem_idx < len(self.elements)):
            return
        elem = self.elements[elem_idx]
        if face in elem.faces:
            self._push_undo()
            elem.faces[face]["uv"] = list(uv)

    def set_face_cullface(self, elem_idx, face, cullface):
        if not (0 <= elem_idx < len(self.elements)):
            return
        elem = self.elements[elem_idx]
        if face in elem.faces:
            self._push_undo()
            if cullface:
                elem.faces[face]["cullface"] = cullface
            else:
                elem.faces[face].pop("cullface", None)


def parse_model_json(model_ref):
    """Load and parse a Minecraft block model JSON from a resource reference.

    model_ref: e.g. 'minecraft:block/custom/conveyor_belt_base'
    Returns (ModelData, geometry_list) tuple, or (None, None) if not found.
    """
    if ":" in model_ref:
        _, path = model_ref.split(":", 1)
    else:
        path = model_ref
    json_path = os.path.join(MODELS_DIR, path + ".json")
    if not os.path.exists(json_path):
        return None, None

    with open(json_path, "r") as f:
        raw_data = json.load(f)

    elements = raw_data.get("elements", [])
    if not elements:
        return None, None

    md = ModelData.from_json(raw_data, source_path=json_path)
    geometry = md.to_geometry()
    return md, geometry


class Layer:
    """A single layer within a face texture."""

    def __init__(self, name, size, fill_color=(0, 0, 0, 0)):
        self.name = name
        self.image = Image.new("RGBA", (size, size), fill_color)
        self.opacity = 1.0
        self.visible = True


class UndoEntry:
    """Delta-based undo entry storing only the changed region."""

    __slots__ = ('state_idx', 'face', 'layer_idx', 'bbox', 'region')

    def __init__(self, state_idx, face, layer_idx, bbox, region):
        self.state_idx = state_idx
        self.face = face
        self.layer_idx = layer_idx
        self.bbox = bbox
        self.region = region


class BlockState:
    """A single block state containing layered face textures and optional geometry."""

    def __init__(self, name, size=32):
        self.name = name
        self.geometry = None
        self.model_data = None   # ModelData instance when editable model is loaded
        self.layers = {}        # face -> [Layer, ...]
        self.active_layer = {}   # face -> int (index into layers list)
        self.source_paths = {}   # face -> original file path
        for face in FACE_NAMES:
            self.layers[face] = [Layer("Background", size, (200, 200, 200, 255))]
            self.active_layer[face] = 0


class TextureModel(QObject):
    """Shared data model holding multi-state block textures with layer support."""

    face_updated = pyqtSignal(str)
    state_changed = pyqtSignal(int)
    color_changed = pyqtSignal()
    layers_changed = pyqtSignal()
    reference_changed = pyqtSignal()
    geometry_changed = pyqtSignal()

    def __init__(self, size=32):
        super().__init__()
        self.size = size
        self.states = [BlockState("default", size)]
        self.active_state_index = 0
        self.active_face = "north"
        self.current_color = (0, 0, 0, 255)
        self.brush_size = 1
        self.symmetry = SYMMETRY_NONE
        self.selection_rect = None       # (x, y, w, h) or None — set by SelectionTool
        self.selected_model_element = -1  # index of selected element in model panel
        self.reference_image = None      # PIL Image or None
        self.reference_opacity = 0.3
        self.reference_offset_x = 0.0   # offset in pixel-space (0 = aligned to canvas origin)
        self.reference_offset_y = 0.0
        self.reference_scale = 1.0      # 1.0 = fit to canvas, >1 = larger
        self._undo_stack = []
        self._redo_stack = []
        self._stroke_snapshot = None
        self._stroke_state = None
        self._stroke_face = None
        self._stroke_layer = None
        self._stroke_bbox = None  # (x1, y1, x2, y2) dirty region tracked during painting

    @property
    def active_state(self):
        return self.states[self.active_state_index]

    def get_geometry(self):
        """Return the active state's geometry, or the default cube."""
        md = self.active_state.model_data
        if md is not None:
            return md.to_geometry()
        geo = self.active_state.geometry
        return geo if geo is not None else DEFAULT_GEOMETRY

    def get_image(self, face=None, state_index=None):
        """Return the active layer's image for direct editing."""
        if state_index is None:
            state_index = self.active_state_index
        if face is None:
            face = self.active_face
        state = self.states[state_index]
        idx = state.active_layer[face]
        return state.layers[face][idx].image

    def get_active_face_dimensions_pixels(self):
        """Return the face dimensions for the active face in pixel coordinates.

        Computes the physical face size from the element's from/to positions.
        Returns (width, height) in pixel space, or None if no model data.
        """
        md = self.active_state.model_data
        idx = self.selected_model_element
        if md is None or not (0 <= idx < len(md.elements)):
            return None
        elem = md.elements[idx]
        face = self.active_face
        if face not in elem.faces:
            return None
        fp = elem.from_pos
        tp = elem.to_pos
        dx = abs(tp[0] - fp[0])
        dy = abs(tp[1] - fp[1])
        dz = abs(tp[2] - fp[2])
        if face in ("north", "south"):
            fw, fh = dx, dy
        elif face in ("east", "west"):
            fw, fh = dz, dy
        else:  # up, down
            fw, fh = dx, dz
        scale = self.size / 16.0
        return (int(fw * scale), int(fh * scale))

    def get_active_face_uv_pixels(self):
        """Return the UV region for the active face in pixel coordinates.

        Returns (x1, y1, x2, y2) in pixel space, or None if unavailable.
        """
        md = self.active_state.model_data
        idx = self.selected_model_element
        if md is None or not (0 <= idx < len(md.elements)):
            return None
        elem = md.elements[idx]
        face = self.active_face
        if face not in elem.faces:
            return None
        uv = elem.faces[face].get("uv", [0, 0, 16, 16])
        scale = self.size / 16.0
        return (
            int(uv[0] * scale),
            int(uv[1] * scale),
            int(uv[2] * scale),
            int(uv[3] * scale),
        )

    def get_composite(self, face=None, state_index=None):
        """Return the flattened composite of all visible layers for a face."""
        if state_index is None:
            state_index = self.active_state_index
        if face is None:
            face = self.active_face
        state = self.states[state_index]
        layers = state.layers[face]

        # Fast path: single visible layer at full opacity
        visible = [layer for layer in layers if layer.visible]
        if len(visible) == 1 and visible[0].opacity >= 1.0:
            return visible[0].image

        result = Image.new("RGBA", (self.size, self.size), (0, 0, 0, 0))
        for layer in layers:
            if not layer.visible:
                continue
            if layer.opacity >= 1.0:
                result = Image.alpha_composite(result, layer.image)
            else:
                temp = layer.image.copy()
                bands = temp.split()
                alpha = bands[3].point(lambda x, o=layer.opacity: int(x * o))
                temp = Image.merge("RGBA", (bands[0], bands[1], bands[2], alpha))
                result = Image.alpha_composite(result, temp)
        return result

    def set_active_face(self, face):
        if face in FACE_NAMES:
            self.active_face = face
            self.face_updated.emit(face)
            self.layers_changed.emit()

    def set_active_state(self, index):
        if 0 <= index < len(self.states):
            self.active_state_index = index
            self.state_changed.emit(index)
            self.face_updated.emit(self.active_face)
            self.layers_changed.emit()

    def add_state(self, name):
        self.states.append(BlockState(name, self.size))

    def resize_all(self, new_size):
        """Resize all layer textures across all states to a new size."""
        self.size = new_size
        for state in self.states:
            for face in FACE_NAMES:
                for layer in state.layers[face]:
                    if layer.image.width == new_size and layer.image.height == new_size:
                        continue
                    layer.image = layer.image.resize(
                        (new_size, new_size), Image.Resampling.NEAREST,
                    )
        self._undo_stack.clear()
        self._redo_stack.clear()
        self.state_changed.emit(self.active_state_index)
        self.face_updated.emit(self.active_face)
        self.layers_changed.emit()

    # ------------------------------------------------------------------
    # Layer management
    # ------------------------------------------------------------------

    def set_active_layer(self, index):
        state = self.active_state
        face = self.active_face
        if 0 <= index < len(state.layers[face]):
            state.active_layer[face] = index
            self.layers_changed.emit()

    def add_layer(self, name=None):
        state = self.active_state
        face = self.active_face
        if name is None:
            name = f"Layer {len(state.layers[face])}"
        idx = state.active_layer[face] + 1
        new_layer = Layer(name, self.size)
        state.layers[face].insert(idx, new_layer)
        state.active_layer[face] = idx
        self.layers_changed.emit()
        self.face_updated.emit(face)

    def delete_layer(self):
        state = self.active_state
        face = self.active_face
        layers = state.layers[face]
        if len(layers) <= 1:
            return
        idx = state.active_layer[face]
        layers.pop(idx)
        state.active_layer[face] = min(idx, len(layers) - 1)
        self._undo_stack.clear()
        self._redo_stack.clear()
        self.layers_changed.emit()
        self.face_updated.emit(face)

    def merge_layer_down(self):
        state = self.active_state
        face = self.active_face
        layers = state.layers[face]
        idx = state.active_layer[face]
        if idx <= 0:
            return
        upper = layers[idx]
        lower = layers[idx - 1]
        if upper.opacity >= 1.0:
            lower.image = Image.alpha_composite(lower.image, upper.image)
        else:
            temp = upper.image.copy()
            bands = temp.split()
            alpha = bands[3].point(lambda x, o=upper.opacity: int(x * o))
            temp = Image.merge("RGBA", (bands[0], bands[1], bands[2], alpha))
            lower.image = Image.alpha_composite(lower.image, temp)
        layers.pop(idx)
        state.active_layer[face] = idx - 1
        self._undo_stack.clear()
        self._redo_stack.clear()
        self.layers_changed.emit()
        self.face_updated.emit(face)

    def move_layer(self, direction):
        """Move active layer up (+1) or down (-1)."""
        state = self.active_state
        face = self.active_face
        layers = state.layers[face]
        idx = state.active_layer[face]
        new_idx = idx + direction
        if new_idx < 0 or new_idx >= len(layers):
            return
        layers[idx], layers[new_idx] = layers[new_idx], layers[idx]
        state.active_layer[face] = new_idx
        self.layers_changed.emit()
        self.face_updated.emit(face)

    def set_layer_opacity(self, opacity):
        state = self.active_state
        face = self.active_face
        idx = state.active_layer[face]
        layers = state.layers[face]
        if 0 <= idx < len(layers):
            layers[idx].opacity = opacity
            self.face_updated.emit(face)
            self.layers_changed.emit()

    def set_layer_visible(self, layer_idx, visible):
        state = self.active_state
        face = self.active_face
        layers = state.layers[face]
        if 0 <= layer_idx < len(layers):
            layers[layer_idx].visible = visible
            self.face_updated.emit(face)
            self.layers_changed.emit()

    # ------------------------------------------------------------------
    # Delta-based undo / redo
    # ------------------------------------------------------------------

    def begin_stroke(self):
        """Save a snapshot before a paint stroke begins."""
        self._stroke_snapshot = self.get_image().copy()
        self._stroke_state = self.active_state_index
        self._stroke_face = self.active_face
        self._stroke_layer = self.active_state.active_layer[self.active_face]
        self._stroke_bbox = None

    def _expand_stroke_bbox(self, x, y):
        """Expand the dirty region to include pixel (x, y)."""
        if self._stroke_bbox is None:
            self._stroke_bbox = (x, y, x + 1, y + 1)
        else:
            x1, y1, x2, y2 = self._stroke_bbox
            self._stroke_bbox = (min(x1, x), min(y1, y), max(x2, x + 1), max(y2, y + 1))

    def end_stroke(self):
        """Finalize a paint stroke, storing only the changed region."""
        if self._stroke_snapshot is None:
            self._stroke_snapshot = None
            return
        bbox = self._stroke_bbox
        if bbox is None:
            self._stroke_snapshot = None
            return
        old_region = self._stroke_snapshot.crop(bbox)
        self._undo_stack.append(UndoEntry(
            self._stroke_state, self._stroke_face,
            self._stroke_layer, bbox, old_region,
        ))
        if len(self._undo_stack) > 100:
            self._undo_stack.pop(0)
        self._redo_stack.clear()
        self._stroke_snapshot = None
        self._stroke_bbox = None

    def undo(self):
        if not self._undo_stack:
            return
        entry = self._undo_stack.pop()
        layer = self.states[entry.state_idx].layers[entry.face][entry.layer_idx]
        current_region = layer.image.crop(entry.bbox)
        self._redo_stack.append(UndoEntry(
            entry.state_idx, entry.face, entry.layer_idx,
            entry.bbox, current_region,
        ))
        layer.image.paste(entry.region, (entry.bbox[0], entry.bbox[1]))
        if entry.state_idx == self.active_state_index:
            self.face_updated.emit(entry.face)

    def redo(self):
        if not self._redo_stack:
            return
        entry = self._redo_stack.pop()
        layer = self.states[entry.state_idx].layers[entry.face][entry.layer_idx]
        current_region = layer.image.crop(entry.bbox)
        self._undo_stack.append(UndoEntry(
            entry.state_idx, entry.face, entry.layer_idx,
            entry.bbox, current_region,
        ))
        layer.image.paste(entry.region, (entry.bbox[0], entry.bbox[1]))
        if entry.state_idx == self.active_state_index:
            self.face_updated.emit(entry.face)

    # ------------------------------------------------------------------
    # Symmetry helpers
    # ------------------------------------------------------------------

    def _mirror_points(self, cx, cy):
        """Return list of mirrored (cx, cy) points based on symmetry mode."""
        img = self.get_image()
        w, h = img.width, img.height
        points = [(cx, cy)]
        if self.symmetry == SYMMETRY_HORIZONTAL:
            points.append((w - 1 - cx, cy))
        elif self.symmetry == SYMMETRY_VERTICAL:
            points.append((cx, h - 1 - cy))
        elif self.symmetry == SYMMETRY_QUAD:
            points.append((w - 1 - cx, cy))
            points.append((cx, h - 1 - cy))
            points.append((w - 1 - cx, h - 1 - cy))
        return points

    def _mirror_pair(self, x0, y0, x1, y1):
        """Return mirrored (start, end) point pairs for shape symmetry."""
        img = self.get_image()
        w, h = img.width, img.height
        pairs = [(x0, y0, x1, y1)]
        if self.symmetry == SYMMETRY_HORIZONTAL:
            pairs.append((w - 1 - x0, y0, w - 1 - x1, y1))
        elif self.symmetry == SYMMETRY_VERTICAL:
            pairs.append((x0, h - 1 - y0, x1, h - 1 - y1))
        elif self.symmetry == SYMMETRY_QUAD:
            pairs.append((w - 1 - x0, y0, w - 1 - x1, y1))
            pairs.append((x0, h - 1 - y0, x1, h - 1 - y1))
            pairs.append((w - 1 - x0, h - 1 - y0, w - 1 - x1, h - 1 - y1))
        return pairs

    # ------------------------------------------------------------------
    # Pixel operations
    # ------------------------------------------------------------------

    def set_pixel(self, x, y, emit=True):
        img = self.get_image()
        if 0 <= x < img.width and 0 <= y < img.height:
            img.putpixel((x, y), self.current_color)
            self._expand_stroke_bbox(x, y)
            if emit:
                self.face_updated.emit(self.active_face)

    def paint_brush(self, cx, cy, brush_size):
        """Paint a brush_size square centered on (cx, cy) with symmetry."""
        img = self.get_image()
        w, h = img.width, img.height
        offset = brush_size // 2
        changed = False
        for mcx, mcy in self._mirror_points(cx, cy):
            for dy in range(brush_size):
                for dx in range(brush_size):
                    px = mcx - offset + dx
                    py = mcy - offset + dy
                    if 0 <= px < w and 0 <= py < h:
                        img.putpixel((px, py), self.current_color)
                        self._expand_stroke_bbox(px, py)
                        changed = True
        if changed:
            self.face_updated.emit(self.active_face)

    def flood_fill(self, x, y):
        """Flood fill starting at (x, y) with the current color."""
        img = self.get_image()
        w, h = img.width, img.height
        if not (0 <= x < w and 0 <= y < h):
            return

        target_color = img.getpixel((x, y))
        fill_color = self.current_color
        if target_color == fill_color:
            return

        # Snapshot for undo
        self.begin_stroke()

        pixels = img.load()
        queue = deque()
        queue.append((x, y))
        visited = set()
        visited.add((x, y))

        while queue:
            px, py = queue.popleft()
            pixels[px, py] = fill_color
            self._expand_stroke_bbox(px, py)
            for nx, ny in ((px - 1, py), (px + 1, py), (px, py - 1), (px, py + 1)):
                if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in visited:
                    if pixels[nx, ny] == target_color:
                        visited.add((nx, ny))
                        queue.append((nx, ny))

        self.face_updated.emit(self.active_face)
        self.end_stroke()

    def copy_face(self, src_face, dst_face):
        """Copy all layers from one face to another within the active state."""
        if src_face == dst_face:
            return
        state = self.active_state
        src_layers = state.layers[src_face]
        new_layers = []
        for src_l in src_layers:
            dst_l = Layer(src_l.name, self.size)
            dst_l.image = src_l.image.copy()
            dst_l.opacity = src_l.opacity
            dst_l.visible = src_l.visible
            new_layers.append(dst_l)
        state.layers[dst_face] = new_layers
        state.active_layer[dst_face] = state.active_layer[src_face]
        self.face_updated.emit(dst_face)
        self.layers_changed.emit()

    def get_pixel(self, x, y):
        """Sample a pixel from the composite (what the user sees)."""
        img = self.get_composite()
        if 0 <= x < img.width and 0 <= y < img.height:
            return img.getpixel((x, y))
        return None

    # ------------------------------------------------------------------
    # File I/O
    # ------------------------------------------------------------------

    def load_face(self, face, filepath):
        img = Image.open(filepath).convert("RGBA")
        state = self.active_state
        state.layers[face] = [Layer("Background", img.width)]
        state.layers[face][0].image = img
        state.active_layer[face] = 0
        state.source_paths[face] = filepath
        if self.size != img.width:
            self.size = img.width
        self._undo_stack.clear()
        self._redo_stack.clear()
        self.face_updated.emit(face)
        self.layers_changed.emit()

    def save_face(self, face, filepath):
        self.get_composite(face).save(filepath, "PNG")

    def save_to_source_paths(self):
        """Save all faces across all states back to their original file paths.

        Returns (saved_count, skipped_faces) tuple.
        """
        saved = 0
        skipped = []
        for si, state in enumerate(self.states):
            for face in FACE_NAMES:
                path = state.source_paths.get(face)
                if path:
                    self.get_composite(face, si).save(path, "PNG")
                    saved += 1
                else:
                    skipped.append(f"{state.name}/{face}")
        return saved, skipped

    def has_source_paths(self):
        """Check if any state has source paths from loading."""
        return any(
            bool(state.source_paths) for state in self.states
        )

    # ------------------------------------------------------------------
    # Reference image
    # ------------------------------------------------------------------

    def set_reference(self, image):
        """Set a reference image (PIL Image) or None to clear."""
        self.reference_image = image
        self.reference_offset_x = 0.0
        self.reference_offset_y = 0.0
        self.reference_scale = 1.0
        self.reference_changed.emit()

    def set_reference_opacity(self, opacity):
        self.reference_opacity = max(0.0, min(1.0, opacity))
        self.reference_changed.emit()

    def set_reference_offset(self, x, y):
        self.reference_offset_x = x
        self.reference_offset_y = y
        self.reference_changed.emit()

    def set_reference_scale(self, scale):
        self.reference_scale = max(0.1, min(10.0, scale))
        self.reference_changed.emit()

    def reset_reference_transform(self):
        self.reference_offset_x = 0.0
        self.reference_offset_y = 0.0
        self.reference_scale = 1.0
        self.reference_changed.emit()

    # ------------------------------------------------------------------
    # Block loading
    # ------------------------------------------------------------------

    def _load_state_from_generation(self, state_name, generation, texture_dir):
        """Build a BlockState from a CraftEngine generation block."""
        parent = generation.get("parent", "")
        textures = generation.get("textures", {})

        parent_type = None
        for ptype in PARENT_MODEL_MAPPINGS:
            if ptype in parent:
                parent_type = ptype
                break

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
                    block_state.layers[face] = [Layer("Background", img.width)]
                    block_state.layers[face][0].image = img.copy()
                    block_state.active_layer[face] = 0
                    block_state.source_paths[face] = filepath

        if parent_type == "cube" and parent:
            md, parsed = parse_model_json(parent)
            if parsed:
                block_state.geometry = parsed
                block_state.model_data = md

        return block_state

    def load_block_from_yaml(self, yaml_data, texture_dir):
        """Load all states from parsed CraftEngine YAML config."""
        self.states.clear()
        self.active_state_index = 0

        sections = []
        for key in yaml_data:
            if key.startswith("items"):
                sections.append((key, yaml_data[key]))

        sections.sort(key=lambda x: (x[0] != "items", x[0]))

        for section_key, section_data in sections:
            for item_id, item_config in section_data.items():
                state_name = (
                    item_id.split(":")[-1] if ":" in item_id else item_id
                )
                behavior = item_config.get("behavior", {})
                block = behavior.get("block", {})

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
        self._undo_stack.clear()
        self._redo_stack.clear()
        self.state_changed.emit(0)
        self.face_updated.emit(self.active_face)
        self.layers_changed.emit()

    def load_block_by_filename(self, block_id, texture_dir):
        """Fallback: discover textures by filename pattern."""
        self.states.clear()
        self.active_state_index = 0

        block_state = BlockState(block_id, self.size)
        loaded_any = False

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
            block_state.layers["up"] = [Layer("Background", self.size)]
            block_state.layers["up"][0].image = top_img
            block_state.source_paths["up"] = top_path
            block_state.layers["down"] = [Layer("Background", self.size)]
            block_state.layers["down"][0].image = bottom_img
            block_state.source_paths["down"] = bottom_path
            for face in ["north", "south", "east", "west"]:
                block_state.layers[face] = [Layer("Background", self.size)]
                block_state.layers[face][0].image = side_img.copy()
                block_state.source_paths[face] = side_path
            loaded_any = True
        else:
            face_map = {}
            for face in FACE_NAMES:
                path = os.path.join(texture_dir, f"{block_id}_{face}.png")
                if os.path.exists(path):
                    face_map[face] = path

            if face_map:
                first_img = Image.open(
                    list(face_map.values())[0],
                ).convert("RGBA")
                self.size = first_img.width
                block_state = BlockState(block_id, self.size)
                for face, path in face_map.items():
                    img = Image.open(path).convert("RGBA")
                    block_state.layers[face] = [Layer("Background", self.size)]
                    block_state.layers[face][0].image = img
                    block_state.active_layer[face] = 0
                    block_state.source_paths[face] = path
                loaded_any = True
            else:
                all_path = os.path.join(texture_dir, f"{block_id}.png")
                if os.path.exists(all_path):
                    img = Image.open(all_path).convert("RGBA")
                    self.size = img.width
                    block_state = BlockState(block_id, self.size)
                    for face in FACE_NAMES:
                        block_state.layers[face] = [Layer("Background", self.size)]
                        block_state.layers[face][0].image = img.copy()
                        block_state.active_layer[face] = 0
                        block_state.source_paths[face] = all_path
                    loaded_any = True

        self.states.append(block_state)
        if not loaded_any:
            self.states[0] = BlockState("default", self.size)

        self.active_state_index = 0
        self.active_face = "north"
        self._undo_stack.clear()
        self._redo_stack.clear()
        self.state_changed.emit(0)
        self.face_updated.emit(self.active_face)
        self.layers_changed.emit()
        return loaded_any
