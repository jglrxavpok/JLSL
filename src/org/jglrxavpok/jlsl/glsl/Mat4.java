package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.glsl.GLSL.*;

public class Mat4
{

	private double[] data;
	
	public Mat4(Vec4 column1, Vec4 column2, Vec4 column3, Vec4 column4)
	{
		data = new double[4*4];
		data[0] = column1.x;
		data[1] = column1.y;
		data[2] = column1.z;
		data[3] = column1.w;
		
		data[4] = column2.x;
		data[5] = column2.y;
		data[6] = column2.z;
		data[7] = column2.w;
		
		data[8] = column3.x;
		data[9] = column3.y;
		data[10] = column3.z;
		data[11] = column3.w;
		
		data[12] = column4.x;
		data[13] = column4.y;
		data[14] = column4.z;
		data[15] = column4.w;
	}
	
	@Substitute(value="*", ownerBefore=true, usesParenthesis = false)
	public Vec4 mul(Vec4 m)
	{
		return null;
	}
	
	@Substitute(value="*", ownerBefore=true, usesParenthesis = false)
	public Mat4 mul(Mat4 m)
	{
		return null; // TODO: Implement
	}
}
