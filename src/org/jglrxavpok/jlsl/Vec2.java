package org.jglrxavpok.jlsl;

public class Vec2
{

	public double x;
	public double y;

	public Vec2(double x, double y)
	{
		super();
		this.x = x;
		this.y = y;
	}
	
	public double length()
	{
		double dx = x;
		double dy = y;
		return Math.sqrt(dx*dx+dy*dy);
	}

	public Vec2 normalize()
	{
		double l = length();
		double x1 = x/l;
		double y1 = y/l;
		return new Vec2(x1,y1);
	}


}
