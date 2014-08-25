package org.jglrxavpok.jlsl;

import java.io.PrintWriter;
import java.util.*;

import org.jglrxavpok.jlsl.fragments.*;

public abstract class CodeEncoder
{
	public JLSLContext context = null;
	
	public abstract void createSourceCode(List<CodeFragment> in, PrintWriter out);

	public void onRequestResult(ArrayList<CodeFragment> fragments)
	{
		
	}
}
