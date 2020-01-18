#extension GL_OES_EGL_image_external : require

precision mediump float;

uniform samplerExternalOES uCameraTexture;
varying vec2 vTexCoordinates;

void main() {
    vec4 color = texture2D(uCameraTexture,vTexCoordinates);
    //float gray_scale = 0.2126*color.r + 0.7152*color.g + 0.0722*color.b;
    gl_FragColor = color;//vec4(gray_scale,gray_scale,gray_scale,color.a);
}
