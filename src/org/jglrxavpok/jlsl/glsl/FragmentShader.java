package org.jglrxavpok.jlsl.glsl;

public abstract class FragmentShader extends ShaderBase
{
	public Vec4 gl_FragColor;
	
	public Vec4 gl_FragCoord;
	
	public Vec4 texture(Sampler2D texture, Vec2 coords)
	{
		return new Vec4(0,0,0,0);
	}
	
	@Deprecated
	public Vec4 texture2D(Sampler2D texture, Vec2 coords)
	{
		return new Vec4(0,0,0,0);
	}
}
