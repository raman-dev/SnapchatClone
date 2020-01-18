attribute vec4 aPosition;
attribute vec4 aTexCoordinates;

uniform mat4 mvpMatrix;
uniform mat4 uTextureTransform;

varying vec2 vTexCoordinates;

void main() {
    gl_Position = mvpMatrix*aPosition;
    vTexCoordinates = (uTextureTransform * aTexCoordinates).st;
}
