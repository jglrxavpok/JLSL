package org.jglrxavpok.jlsl.fragments;

import org.objectweb.asm.*;

public class AccessPolicy implements Opcodes
{

	private boolean isPublic;
	private boolean isProtected;
	private boolean isPrivate;
	private boolean isStatic;
	private boolean isAbstract;
	private boolean isFinal;

	public AccessPolicy(int access)
	{
		isPrivate = hasModifier(access, ACC_PRIVATE);
		isProtected = hasModifier(access, ACC_PROTECTED);
		isPublic = hasModifier(access, ACC_PUBLIC);
		isStatic = hasModifier(access, ACC_STATIC);
		isAbstract = hasModifier(access, ACC_ABSTRACT);
		isFinal = hasModifier(access, ACC_FINAL);
	}

	public boolean isAbstract()
	{
		return isAbstract;
	}

	public boolean isFinal()
	{
		return isFinal;
	}

	public boolean isPublic()
	{
		return isPublic;
	}

	public boolean isProtected()
	{
		return isProtected;
	}

	public boolean isPrivate()
	{
		return isPrivate;
	}

	public boolean isStatic()
	{
		return isStatic;
	}

	private boolean hasModifier(int i, int modifier)
	{
		return (i | modifier) == i;
	}
}
