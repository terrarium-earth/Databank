#version 330

#moj_import <fog.glsl>
#moj_import <dynamictransforms.glsl>

uniform sampler2D ProjectedTarget;

in vec2 texCoord0;
in float sphericalVertexDistance;
in float cylindricalVertexDistance;

out vec4 fragColor;

void main() {
    vec4 color = texture(ProjectedTarget, vec2(texCoord0.x, texCoord0.y));
    if (color.a == 0.0) {
        discard;
    }
    color = color * ColorModulator;
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
