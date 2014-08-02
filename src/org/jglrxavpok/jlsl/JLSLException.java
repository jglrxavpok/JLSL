package org.jglrxavpok.jlsl;

public class JLSLException extends RuntimeException
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8533858789474579803L;

	public JLSLException(String message)
	{
		super(message);
	}

	public JLSLException(Exception e)
	{
		super(e);
	}
}
