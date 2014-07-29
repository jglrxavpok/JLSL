package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.GLSL.Attribute;
import org.jglrxavpok.jlsl.GLSL.Uniform;

@GLSL.Extensions({"GL_ARB_explicit_uniform_location", "GL_ARB_arrays_of_arrays"})
public class TestShader extends FragmentShader
{

	@Uniform
	private Vec2 screenSize;
	
	@Attribute
	private Vec2[] list = new Vec2[70];
	
	@Uniform
	private Vec2[][] list2 = new Vec2[70][4];
	
	public static final double PI = 3.141592653589793D;
	
	@Override
	public void main()
	{
		Vec4 v = new Vec4(gl_FragCoord.x/screenSize.x,gl_FragCoord.y/screenSize.y,gl_FragCoord.z,gl_FragCoord.w);
		v = normalizer(v, v.length());
		Mat2 testMatrix = new Mat2(new Vec2(v.x, v.y), new Vec2(0,1));
		list[10] = list2[0][1];
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
