#extension GL_OES_EGL_image_external : require

precision mediump float;

uniform samplerExternalOES uCameraTexture;
varying vec2 vTexCoordinates;

void main() {
    gl_FragColor = texture2D(uCameraTexture,vTexCoordinates);
}
