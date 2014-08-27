package org.jglrxavpok.jlsl.fragments;

import java.util.*;

public abstract class CodeFragment
{

	public boolean				  forbiddenToPrint = false;
	private ArrayList<CodeFragment> children		 = new ArrayList<CodeFragment>();

	public void addChild(CodeFragment fragment)
	{
		children.add(fragment);
	}

	public ArrayList<CodeFragment> getChildren()
	{
		return children;
	}
}
