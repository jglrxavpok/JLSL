package org.jglrxavpok.jlsl;

import java.io.*;

import org.jglrxavpok.jlsl.glsl.*;

public class NewTest
{

	public static void main(String[] args)
	{
		JLSLContext context = new JLSLContext(new BytecodeDecoder(), new GLSLEncoder(120));
		context.execute(TestShader.class, new PrintWriter(System.out));
	}

}
