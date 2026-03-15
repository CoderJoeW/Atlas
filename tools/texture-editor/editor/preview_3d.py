from PIL import Image
from PyQt6.QtOpenGLWidgets import QOpenGLWidget
from PyQt6.QtCore import Qt
from PyQt6.QtGui import QMouseEvent, QWheelEvent

from OpenGL.GL import (
    glClearColor, glClear, glEnable,
    GL_COLOR_BUFFER_BIT, GL_DEPTH_BUFFER_BIT, GL_DEPTH_TEST,
    GL_TEXTURE_2D, GL_BLEND, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA,
    glBlendFunc, glDepthMask, GL_FALSE, GL_TRUE,
    glGenTextures, glBindTexture, glTexImage2D, glTexSubImage2D,
    glTexParameteri,
    GL_TEXTURE_MIN_FILTER, GL_TEXTURE_MAG_FILTER, GL_NEAREST,
    GL_TEXTURE_WRAP_S, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE,
    GL_RGBA, GL_UNSIGNED_BYTE,
    glBegin, glEnd, glVertex3f, glTexCoord2f, glNormal3f,
    GL_QUADS,
    glMatrixMode, glLoadIdentity, glTranslatef, glRotatef,
    GL_PROJECTION, GL_MODELVIEW,
    glViewport,
)
from OpenGL.GLU import gluPerspective

from editor.texture_model import FACE_NAMES


class Preview3D(QOpenGLWidget):
    """OpenGL 3D block preview with per-face textures and model geometry."""

    def __init__(self, model, parent=None):
        super().__init__(parent)
        self.model = model
        self.setMinimumSize(256, 256)

        self._azimuth = 30.0
        self._elevation = 25.0
        self._zoom = -3.0
        self._last_mouse_pos = None
        self._textures = {}

        self.model.face_updated.connect(self._on_face_updated)
        self.model.state_changed.connect(self._on_state_changed)

    def initializeGL(self):
        glClearColor(0.15, 0.15, 0.15, 1.0)
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        self._upload_all_textures()

    def _pil_to_gl_bytes(self, img):
        return img.transpose(Image.Transpose.FLIP_TOP_BOTTOM).tobytes()

    def _upload_all_textures(self):
        for face in FACE_NAMES:
            img = self.model.get_image(face)
            tex_id = glGenTextures(1)
            glBindTexture(GL_TEXTURE_2D, tex_id)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            data = self._pil_to_gl_bytes(img)
            glTexImage2D(
                GL_TEXTURE_2D, 0, GL_RGBA,
                img.width, img.height, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, data,
            )
            self._textures[face] = tex_id

    def _on_face_updated(self, face):
        self.makeCurrent()
        img = self.model.get_image(face)
        tex_id = self._textures.get(face)
        if tex_id is not None:
            glBindTexture(GL_TEXTURE_2D, tex_id)
            data = self._pil_to_gl_bytes(img)
            glTexSubImage2D(
                GL_TEXTURE_2D, 0, 0, 0,
                img.width, img.height,
                GL_RGBA, GL_UNSIGNED_BYTE, data,
            )
        self.doneCurrent()
        self.update()

    def _on_state_changed(self, index):
        self.makeCurrent()
        for face in FACE_NAMES:
            img = self.model.get_image(face)
            tex_id = self._textures.get(face)
            if tex_id is not None:
                glBindTexture(GL_TEXTURE_2D, tex_id)
                data = self._pil_to_gl_bytes(img)
                glTexImage2D(
                    GL_TEXTURE_2D, 0, GL_RGBA,
                    img.width, img.height, 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, data,
                )
        self.doneCurrent()
        self.update()

    def resizeGL(self, w, h):
        glViewport(0, 0, w, h)
        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        aspect = w / max(h, 1)
        gluPerspective(45.0, aspect, 0.1, 100.0)
        glMatrixMode(GL_MODELVIEW)

    def _face_has_transparency(self, tex_key):
        """Check if a face texture contains any transparent pixels."""
        img = self.model.get_image(tex_key)
        if img.mode != "RGBA":
            return False
        extrema = img.getextrema()
        # extrema[3] is (min_alpha, max_alpha)
        return extrema[3][0] < 255

    def _draw_face(self, face_data):
        tex_key = face_data["texture_key"]
        tex_id = self._textures.get(tex_key)
        if tex_id is None:
            return
        glBindTexture(GL_TEXTURE_2D, tex_id)
        nx, ny, nz = face_data["normal"]
        glBegin(GL_QUADS)
        glNormal3f(nx, ny, nz)
        for i, (vx, vy, vz) in enumerate(face_data["vertices"]):
            tx, ty = face_data["tex_coords"][i]
            glTexCoord2f(tx, ty)
            glVertex3f(vx, vy, vz)
        glEnd()

    def paintGL(self):
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
        glTranslatef(0, 0, self._zoom)
        glRotatef(self._elevation, 1, 0, 0)
        glRotatef(self._azimuth, 0, 1, 0)

        glEnable(GL_TEXTURE_2D)

        # Collect all faces, split into opaque and transparent
        opaque = []
        transparent = []
        geometry = self.model.get_geometry()
        for element in geometry:
            for face_data in element["faces"].values():
                tex_key = face_data["texture_key"]
                if self._textures.get(tex_key) is None:
                    continue
                if self._face_has_transparency(tex_key):
                    transparent.append(face_data)
                else:
                    opaque.append(face_data)

        # Pass 1: opaque faces with depth write on
        glDepthMask(GL_TRUE)
        for face_data in opaque:
            self._draw_face(face_data)

        # Pass 2: transparent faces with depth write off
        if transparent:
            glDepthMask(GL_FALSE)
            for face_data in transparent:
                self._draw_face(face_data)
            glDepthMask(GL_TRUE)

    def mousePressEvent(self, event: QMouseEvent):
        if event.button() == Qt.MouseButton.LeftButton:
            self._last_mouse_pos = event.position()

    def mouseMoveEvent(self, event: QMouseEvent):
        if self._last_mouse_pos is not None:
            dx = event.position().x() - self._last_mouse_pos.x()
            dy = event.position().y() - self._last_mouse_pos.y()
            self._azimuth += dx * 0.5
            self._elevation = max(-89, min(89, self._elevation + dy * 0.5))
            self._last_mouse_pos = event.position()
            self.update()

    def mouseReleaseEvent(self, event: QMouseEvent):
        if event.button() == Qt.MouseButton.LeftButton:
            self._last_mouse_pos = None

    def wheelEvent(self, event: QWheelEvent):
        delta = event.angleDelta().y() / 120.0
        self._zoom = max(-10, min(-1.5, self._zoom + delta * 0.3))
        self.update()
