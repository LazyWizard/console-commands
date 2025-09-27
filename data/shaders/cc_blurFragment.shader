#version 110
// should have a version define

//shader by shiozakana

uniform sampler2D tex;
uniform vec2 resolution;
// uniform vec2 pixelStep;
uniform bool displayBlur;
// one of pass set 1, another pass set 0
uniform bool verticalPass;
uniform float darkening;

vec2 texCoord = gl_TexCoord[0].xy;

#define PI 3.14159265
// also uniform
#define FILTER_I 12
#define FILTER_I_F float(FILTER_I)
const float PER_STEP = (1.0 / (FILTER_I_F * 0.1111111 * PI));

float getGaussian(float x) {
	float p = x * PER_STEP;
	return PER_STEP * 0.3989423 * exp(-0.5 * p * p);
}

// separated gaussian filter (a.k.a gaussian blur), draw it twice, and use last screen capture for each draw
// GL_BLEND should disabled, it was unnecessary here
void main() {
	// should use uniform for pixelStep
	vec2 pixelStep = 1.0 / resolution;
	// normally, alpha channel is unnecessary in post-effect
	// should use texture2D() in lower OpenGL 2.0 ~ 3.2(glsl 110 ~ 150), not texture()
	// and texture2D() functions are deprecated since OpenGL 3.3(glsl 330)
	vec3 Color = displayBlur ? vec3(0.0) : texture2D(tex, texCoord).xyz;

	if (displayBlur) {
		float weightSum = 0.0, currWeight, ift;
		vec2 offset;

		for (int i = -FILTER_I; i <= FILTER_I; i++) {
			ift = float(i);
			currWeight = getGaussian(ift);
			offset = verticalPass ? vec2(0.0, pixelStep.y) : vec2(pixelStep.x, 0.0);
			offset *= ift;
			Color += texture2D(tex, texCoord + offset).xyz * currWeight;
			weightSum += currWeight;
		}
		Color /= weightSum;
	}

	// sqrt(darkening) per pass, or multiply it in last pass
	gl_FragColor = vec4(Color * darkening, 1.0);
}