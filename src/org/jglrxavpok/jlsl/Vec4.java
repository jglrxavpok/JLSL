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
	

	public Vec4 normalize()
	{
		double l = length();
		double x1 = x/l;
		double y1 = y/l;
		double z1 = z/l;
		double w1 = w/l;
		return new Vec4(x1,y1,z1,w1);
	}
}
