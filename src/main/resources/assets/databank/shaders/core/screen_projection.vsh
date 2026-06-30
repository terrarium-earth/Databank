#version 330

#moj_import <fog.glsl>
#moj_import <dynamictransforms.glsl>

in vec3 Position;
in vec2 UV0;

out vec2 texCoord0;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;

void main() {
    vec4 pos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * pos;

    texCoord0 = UV0;

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
}
