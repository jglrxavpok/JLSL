package org.jglrxavpok.jlsl;

public class Vec3
{

	public double x;
	public double y;
	public double z;

	public Vec3(double x, double y, double z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public double length()
	{
		double dx = x;
		double dy = y;
		double dz = z;
		return Math.sqrt(dx*dx+dy*dy+dz*dz);
	}
	
	public Vec3 normalize()
	{
		double l = length();
		double x1 = x/l;
		double y1 = y/l;
		double z1 = z/l;
		return new Vec3(x1,y1,z1);
	}
}
