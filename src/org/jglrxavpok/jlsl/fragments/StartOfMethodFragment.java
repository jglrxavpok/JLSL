package org.jglrxavpok.jlsl.fragments;

import java.util.*;

public class StartOfMethodFragment extends CodeFragment
{
	public AccessPolicy access;
	public String name;
	public String owner;
	public String returnType;
	public ArrayList<String> argumentsTypes = new ArrayList<String>();
	public ArrayList<String> argumentsNames = new ArrayList<String>();
	public HashMap<Integer, String> varNameMap = new HashMap<Integer, String>();
	public HashMap<Integer, String> varTypeMap = new HashMap<Integer, String>();
	public HashMap<String, String> varName2TypeMap = new HashMap<String, String>();
}
