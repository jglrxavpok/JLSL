package org.jglrxavpok.jlsl;

import java.io.IOException;

import org.objectweb.asm.Opcodes;

public class TestBed implements Opcodes
{

	public static void main(String[] args)
	{
		try
		{
			System.err.println(JLSL.translateToGLSL(TestShader.class));
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

}
