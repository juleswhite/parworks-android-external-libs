precision mediump float;       	// Set the default precision to medium. We don't need as high of a precision in the fragment shader.
uniform vec3 u_LightPos;       	// The position of the light in eye space.
uniform sampler2D u_Texture;    // The input texture.
  
varying vec3 v_Position;		// Interpolated position for this fragment.
varying vec3 v_Normal;         	// Interpolated normal for this fragment.
varying vec4 v_Color;			// Color information for this fragment.
varying float v_hasTex;			// Texture existence information for this fragment.
varying vec2 v_TexCoordinate;   // Interpolated texture coordinate per fragment.
  
// The entry point for our fragment shader.
void main()                    		
{              
	if (v_hasTex < 0.5) {
		gl_FragColor = v_Color;
	} else {
		gl_FragColor = texture2D(u_Texture, v_TexCoordinate);
		gl_FragColor.a *= v_Color.a;
	}
}
