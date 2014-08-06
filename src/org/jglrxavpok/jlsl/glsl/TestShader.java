package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.glsl.GLSL.Extensions;
import org.jglrxavpok.jlsl.glsl.GLSL.Uniform;

@Extensions({"GL_ARB_explicit_uniform_location", "GL_ARB_arrays_of_arrays"})
public class TestShader extends FragmentShader
{

	private Vertex vertex;
	
	private Vertex vertex1;
	
	@Uniform
	private Vec2 screenSize;
	
	@Uniform
	private Vec2[] list = new Vec2[70];
	
	@Uniform
	private Object[][][] list2 = new Object[70][4][5];
	
	public static final double PI = 3.141592653589793;
	
	@Override
	public void main()
	{
		Vec4 v = new Vec4(gl_FragCoord.x/screenSize.x,gl_FragCoord.y/screenSize.y,vertex.test(),vertex1.test());
		v = normalizer(v, v.length());
		Mat2 testMatrix = new Mat2(new Vec2(((int)v.x<<2), v.y) , new Vec2(PI,1));
		Vec2 test = (Vec2)list2[0][1][2];
		test = test.normalize();
		gl_FragColor = null;
		
		char charTest = 'a';
		boolean a = false;
		boolean c = true;
		boolean b = a & c;
		if(!(b | a & c))
		{
			;
		}
		vignette();
		charTest += 10;
		normalizer(v, charTest); // TODO: (see DUP)
		normalizer(v, charTest);
	}

	private void vignette()
	{
		gl_FragColor = new Vec4(gl_FragCoord.x/screenSize.x, gl_FragCoord.y/screenSize.y, 0, 1);
		gl_FragColor.z = 1;
		Vec4 v1 = gl_FragColor.sub(gl_FragCoord.sub(gl_FragColor));
		/*boolean b = false;
		if(b)
		{
			gl_FragColor.w = 1;
			gl_FragColor.x = 1;
			if(b)
			{
    			gl_FragColor.x = 0;
    			gl_FragColor.z = 0;
			}
			else
			{
				if(b)
				{
					gl_FragColor.z = 2;
					if(b)
						gl_FragColor.z = 9;
					gl_FragColor.z = 10;
				}
				
				gl_FragColor.z = 3;
			}
		}
		else
		{
			gl_FragColor.y = 1;
		}
		gl_FragColor.x = 2;*/
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
