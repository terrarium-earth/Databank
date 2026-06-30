#version 330

uniform sampler2D DiffuseSampler;
uniform sampler2D ImpactSampler;
uniform sampler2D FrozenImpactSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform alpha {
    float AlphaValue;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 oneTexel = 1.0 / InSize;

    vec4 color = texture(ImpactSampler, texCoord);
    vec4 colorFrozen = texture(FrozenImpactSampler, texCoord);
    float impactAlpha = max(color.a, colorFrozen.a);
    fragColor = vec4(mix(texture(DiffuseSampler, texCoord).rgb, mix(vec3(0.0, 0.0, 0.0), vec3(1.0, 1.0, 1.0), impactAlpha), AlphaValue), 1.0);
}
