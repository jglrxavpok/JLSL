package org.jglrxavpok.jlsl;

import java.io.PrintWriter;
import java.util.List;

import org.jglrxavpok.jlsl.fragments.*;

public interface CodeEncoder
{

	public void createSourceCode(List<CodeFragment> in, PrintWriter out);
}
