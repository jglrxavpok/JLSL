package org.jglrxavpok.jlsl.glsl;

import org.jglrxavpok.jlsl.glsl.GLSL.Substitute;

public class Sampler2D
{
	public int id;
	
	@Substitute(value="", usesParenthesis = false, ownerBefore = true)
	public Sampler2D(int id)
	{
		this.id = id;
	}
}
