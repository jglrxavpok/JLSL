package org.jglrxavpok.jlsl;

import java.lang.annotation.*;
import java.util.*;

import org.jglrxavpok.jlsl.fragments.*;

public class ObfuscationFilter implements CodeFilter
{

	@Retention(RetentionPolicy.RUNTIME)
	public static @interface NonObfuscable
	{

	}

	private ArrayList<MethodEntry> entries = new ArrayList<>();

	private static class MethodEntry
	{
		public String   name;
		public String   owner;
		public String[] argumentTypes;
		public String   newName;

		public MethodEntry(String name, String owner, String[] argumentTypes, String newName)
		{
			this.name = name;
			this.owner = owner;
			this.argumentTypes = argumentTypes;
			this.newName = newName;
		}
	}

	@Override
	public CodeFragment filter(CodeFragment fragment)
	{
		if(fragment instanceof MethodCallFragment)
		{
			MethodCallFragment methodCallFrag = (MethodCallFragment)fragment;
			for(CodeFragment child : methodCallFrag.getChildren())
			{
				if(child instanceof AnnotationFragment)
				{
					AnnotationFragment annot = (AnnotationFragment)child;
					if(annot.name.equals(NonObfuscable.class.getCanonicalName())) return fragment;
				}
			}
			if(!methodCallFrag.methodOwner.equals(((Class<?>)JLSLContext.currentInstance.getCurrentObject()).getCanonicalName()))
			{
				return fragment;
			}
			MethodEntry entry = getMethodEntry(methodCallFrag.methodName, methodCallFrag.methodOwner, methodCallFrag.argumentsTypes);
			methodCallFrag.methodName = entry.newName;
		}
		else if(fragment instanceof StartOfMethodFragment)
		{
			StartOfMethodFragment startMethodCallFrag = (StartOfMethodFragment)fragment;
			for(CodeFragment child : startMethodCallFrag.getChildren())
			{
				if(child instanceof AnnotationFragment)
				{
					AnnotationFragment annot = (AnnotationFragment)child;
					if(annot.name.equals(NonObfuscable.class.getCanonicalName())) return fragment;
				}
			}
			if(!startMethodCallFrag.owner.equals(((Class<?>)JLSLContext.currentInstance.getCurrentObject()).getCanonicalName()))
			{
				return fragment;
			}
			MethodEntry entry = getMethodEntry(startMethodCallFrag.name, startMethodCallFrag.owner, startMethodCallFrag.argumentsTypes.toArray(new String[0]));
			startMethodCallFrag.name = entry.newName;
		}
		return fragment;
	}

	private MethodEntry getMethodEntry(String name, String owner, String[] argumentsTypes)
	{
		for(MethodEntry entry : entries)
		{
			if(entry.name.equals(name) && entry.owner.equals(owner) && Arrays.deepEquals(argumentsTypes, entry.argumentTypes)) return entry;
		}

		MethodEntry entry = new MethodEntry(name, owner, argumentsTypes, getNewName());
		entries.add(entry);
		return entry;
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
		System.out.println("created name " + s + last);
		return s + last;
	}

}
