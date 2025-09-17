

uniform sampler2D tex;
vec2 texCoord = gl_TexCoord[0].xy;
uniform vec2 resolution;

//Shader from https://www.shadertoy.com/view/Xltfzj
void main() {

	float Pi = 6.28318530718; // Pi*2
    
    // GAUSSIAN BLUR SETTINGS {{{
    float Directions = 16.0; // BLUR DIRECTIONS (Default 16.0 - More is better but slower)
    float Quality = 4.0; // BLUR QUALITY (Default 4.0 - More is better but slower)
    float Size = 8.0; // BLUR SIZE (Radius)
    // GAUSSIAN BLUR SETTINGS }}}
   
    //vec2 Radius = Size/iResolution.xy;
    vec2 Radius = Size/resolution.xy;
    
    // Normalized pixel coordinates (from 0 to 1)
    vec2 uv = texCoord/resolution.xy;
    // Pixel colour
    //vec4 Color = texture(iChannel0, uv);
	vec4 Color = texture2D(tex, texCoord);
    
    // Blur calculations
    for( float d=0.0; d<Pi; d+=Pi/Directions)
    {
		for(float i=1.0/Quality; i<=1.0; i+=1.0/Quality)
        {
			//Color += texture( iChannel0, uv+vec2(cos(d),sin(d))*Radius*i);		
			Color += texture(tex, texCoord+vec2(cos(d),sin(d))*Radius*i);		
        }
    }
    
    // Output to screen
    Color /= Quality * Directions - 15.0;

    Color.r *= 0.1;
    Color.g *= 0.1;
    Color.b *= 0.1;

	gl_FragColor = Color;

}