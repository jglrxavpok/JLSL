package org.jglrxavpok.jlsl;

import org.jglrxavpok.jlsl.fragments.*;

@FunctionalInterface
public interface CodeFilter
{
	public CodeFragment filter(CodeFragment fragment);
}
