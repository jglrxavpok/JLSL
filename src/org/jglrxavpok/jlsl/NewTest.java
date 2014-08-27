package org.jglrxavpok.jlsl;

import java.io.*;

import org.jglrxavpok.jlsl.glsl.*;

public class NewTest
{

	public static void main(String[] args)
	{
		BytecodeDecoder decoder = new BytecodeDecoder();// .addInstructionsFromInterfaces(true);
		GLSLEncoder encoder = new GLSLEncoder(120);
		JLSLContext context = new JLSLContext(decoder, encoder);
		context.execute(TestShader.class, new PrintWriter(System.out));
	}

}
