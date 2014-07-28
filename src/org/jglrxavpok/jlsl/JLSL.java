package org.jglrxavpok.jlsl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class JLSL
{

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Extensions
	{
		String[] value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Layout
	{
		int pos();
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
}
