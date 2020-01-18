
attribute vec4 a_Position;
uniform mat4 mvpMatrix;

void main() {
    gl_Position = mvpMatrix*a_Position;
}
