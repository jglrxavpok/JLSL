package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.glsl.GLSL.*;


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
	
	public Vec4(double x, double y, Vec2 zw)
	{
		this.z = zw.x;
		this.w = zw.y;
		this.x = x;
		this.y = y;
	}
	
	public Vec4(double x, Vec2 yz, double w)
	{
		this.y = yz.x;
		this.z = yz.y;
		this.x = x;
		this.w = w;
	}
	
	public Vec4(Vec2 xy, double z, double w)
	{
		this.x = xy.x;
		this.y = xy.y;
		this.z = z;
		this.w = w;
	}

	public Vec4(Vec3 xyz, double w)
	{
		this.x = xyz.x;
		this.y = xyz.y;
		this.z = xyz.z;
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
	
	@Substitute(value = "+", usesParenthesis = false, ownerBefore = true)
	public Vec4 add(Vec4 v)
	{
		return new Vec4(x+v.x,y+v.y,z+v.z,w+v.w);
	}
	
	@Substitute(value = "-", usesParenthesis = false, ownerBefore = true)
	public Vec4 sub(Vec4 v)
	{
		return new Vec4(x-v.x,y-v.y,z-v.z,w-v.w);
	}

	@Substitute(value = "*", usesParenthesis = false, ownerBefore = true)
	public Vec4 mul(double d)
	{
		return new Vec4(x*d,y*d,z*d,w*d);
	}
}
