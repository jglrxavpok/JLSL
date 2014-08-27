package org.jglrxavpok.jlsl.glsl;

public class Mat2
{
	private double[] data;

	public Mat2(Vec2 column1, Vec2 column2)
	{
		data = new double[2 * 2];
		data[0] = column1.x;
		data[1] = column1.y;

		data[2] = column2.x;
		data[3] = column2.y;
	}
}
