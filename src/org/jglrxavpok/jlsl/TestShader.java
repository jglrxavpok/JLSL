package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.JLSL.Extensions;
import org.jglrxavpok.jlsl.JLSL.Uniform;

@Extensions({"GL_ARB_explicit_uniform_location"})
public class TestShader extends FragmentShader
{

	@Uniform
	private Vec2 screenSize;
	
	public static final double PI = 3.141592653589793D;
	
	@Override
	public void main()
	{
		Vec4 v = new Vec4(gl_FragCoord.x/screenSize.x,gl_FragCoord.y/screenSize.y,gl_FragCoord.z,gl_FragCoord.w);
		v = normalizer(v, v.length());
		gl_FragColor = v;
	}

	private Vec4 normalizer(Vec4 v, double l)
	{
		double x1 = v.x/l;
		double y1 = v.y/l;
		double z1 = v.z/l;
		double w1 = v.w/l;
		return new Vec4(x1,y1,z1,w1);
	}

}
