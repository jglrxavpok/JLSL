package org.jglrxavpok.jlsl;

import java.util.*;

import org.jglrxavpok.jlsl.fragments.*;

public class ObfuscationFilter implements CodeFilter
{

	private ArrayList<MethodEntry> entries = new ArrayList<>();

	private static class MethodEntry
	{
		public String   name;
		public String   owner;
		public String[] argumentTypes;

		public MethodEntry(String name, String owner, String[] argumentTypes)
		{
			this.name = name;
			this.owner = owner;
			this.argumentTypes = argumentTypes;
		}
	}

	@Override
	public CodeFragment filter(CodeFragment fragment)
	{
		if(fragment instanceof MethodCallFragment)
		{
			MethodCallFragment methodCallFrag = (MethodCallFragment)fragment;
			MethodEntry entry = getMethodEntry(methodCallFrag.methodName, methodCallFrag.methodOwner, methodCallFrag.argumentsTypes);
			methodCallFrag.methodName = entry.name;
		}
		else if(fragment instanceof StartOfMethodFragment)
		{
			StartOfMethodFragment startMethodCallFrag = (StartOfMethodFragment)fragment;
			MethodEntry entry = getMethodEntry(startMethodCallFrag.name, startMethodCallFrag.owner, startMethodCallFrag.argumentsTypes.toArray(new String[0]));
			startMethodCallFrag.name = entry.name;
		}
		return fragment;
	}

	private MethodEntry getMethodEntry(String name, String owner, String[] argumentsTypes)
	{
		for(MethodEntry entry : entries)
		{
			if(entry.name.equals(name) && entry.owner.equals(owner) && Arrays.deepEquals(argumentsTypes, entry.argumentTypes)) return entry;
		}

		return new MethodEntry(getNewName(), owner, argumentsTypes);
	}

	private int nbr;

	private String getNewName()
	{
		char last = (char)('a' + (nbr % 26));
		int nbr1 = nbr - 26;
		String s = "";
		while(nbr1 >= 0)
		{
			nbr1 -= 26;
			s = (char)('a' + (nbr1 % 26)) + s;
		}
		nbr++ ;
		return s + last;
	}

}
