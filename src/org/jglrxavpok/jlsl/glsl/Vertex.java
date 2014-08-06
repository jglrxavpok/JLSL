package org.jglrxavpok.jlsl.glsl;

public class Vertex
{

	private Vec3 pos = new Vec3(1,1,1);
	
	private Vec2 texCoords = new Vec2(0,0);
	
	public double test()
	{
		// return pos.x += 1;  TODO: DUP2_X1
		pos.x++;
		return pos.x;
	}
}
