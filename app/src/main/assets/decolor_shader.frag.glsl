#extension GL_OES_EGL_image_external : require

precision highp float;

varying vec2 vPreviewCoord;

uniform samplerExternalOES texPreview;
uniform mat4 uDecoloring;

void main() {
    gl_FragColor = uDecoloring * texture2D(texPreview, vPreviewCoord).rgba;
}