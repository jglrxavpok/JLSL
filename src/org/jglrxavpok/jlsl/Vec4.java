package org.jglrxavpok.jlsl;

public class Vec4
{
	public double x;
	public double y;
	public double z;
	public double w;

	public Vec4(double x, double y, double z, double w)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	
	public double length()
	{
		double dx = x;
		double dy = y;
		double dz = z;
		double dw = w;
		return Math.sqrt(dx*dx+dy*dy+dz*dz+dw*dw);
	}
}
