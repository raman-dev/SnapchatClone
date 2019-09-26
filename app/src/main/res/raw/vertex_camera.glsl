attribute vec4 aPosition;
attribute vec2 aTexCoordinates;

uniform mat4 mvpMatrix;
varying vec2 vTexCoordinates;
void main() {
    gl_Position = mvpMatrix*aPosition;
    vTexCoordinates = aTexCoordinates;
}
