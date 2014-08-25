package org.jglrxavpok.jlsl.glsl;

import java.lang.annotation.*;

public class GLSL
{

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Extensions
	{
		String[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Layout
	{
		int location();
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Attribute
	{
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Uniform
	{
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Varying
	{
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface In
	{
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Out
	{
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface SwizzlingMethod // TODO
	{
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Substitute
	{
		String value();

		boolean usesParenthesis();
		
		boolean ownerBefore();
	}
}
