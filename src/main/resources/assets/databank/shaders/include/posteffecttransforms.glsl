#version 330

layout(std140) uniform PostEffectTransforms {
    float time;
    vec3 CameraPosition;
    mat4 ModelViewMat;
    mat4 invViewMat;
    mat4 invProjMat;
};
