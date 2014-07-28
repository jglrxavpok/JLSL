package org.jglrxavpok.jlsl;

public class Mat3
{

	private double[] data;
	
	public Mat3(Vec3 column1, Vec3 column2, Vec3 column3)
	{
		data = new double[3*3];
		data[0] = column1.x;
		data[1] = column1.y;
		data[2] = column1.z;
		
		data[3] = column2.x;
		data[4] = column2.y;
		data[5] = column2.z;
		
		data[6] = column3.x;
		data[7] = column3.y;
		data[8] = column3.z;
	}
}
