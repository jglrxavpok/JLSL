package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.JLSL.Uniform;

//@Extensions({"GL_ARB_explicit_uniform_location"})
public class TestShader extends FragmentShader
{

	@Uniform
	private Vec2 screenSize;
	
	public static final double PI = 3.141592653589793D;
	
	@Override
	public void main()
	{
		Vec4 v = new Vec4(gl_FragCoord.x/screenSize.x,gl_FragCoord.y/screenSize.y,gl_FragCoord.z,gl_FragCoord.w);
		gl_FragColor = v;
	}

}
