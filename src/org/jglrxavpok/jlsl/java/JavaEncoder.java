package org.jglrxavpok.jlsl.java;

import java.io.*;
import java.util.*;

import org.jglrxavpok.jlsl.*;
import org.jglrxavpok.jlsl.fragments.*;
import org.jglrxavpok.jlsl.fragments.MethodCallFragment.InvokeTypes;

public class JavaEncoder extends CodeEncoder
{
	public static boolean		   DEBUG		= true;

	private int					 indentation;
	private NewClassFragment		currentClass;
	private int					 currentLine;
	private Stack<String>		   stack;
	private Stack<String>		   typesStack;
	private HashMap<String, String> name2type;
	private HashMap<Object, String> constants;
	private ArrayList<String>	   initialized;
	private StartOfMethodFragment   currentMethod;
	private boolean				 allowedToPrint;
	private PrintWriter			 output;
	private Stack<CodeFragment>	 waiting;
	private Stack<String>		   newInstances;
	public boolean				  interpreting = false;
	private HashMap<String, String> imports;

	private String				  classPackage;

	private String				  className;

	public JavaEncoder(int glslversion)
	{
		imports = new HashMap<>();
		stack = new Stack<String>();
		typesStack = new Stack<String>();
		initialized = new ArrayList<String>();
		name2type = new HashMap<String, String>();
		constants = new HashMap<Object, String>();
		waiting = new Stack<CodeFragment>();
		newInstances = new Stack<String>();

		init();
	}

	public void init()
	{

	}

	private String toJava(String type)
	{
		if(type == null) return "";
		String copy = type;
		String end = "";
		while(copy.contains("[]"))
		{
			copy = copy.replaceFirst("\\[\\]", "");
			end += "[]";
		}
		type = copy;
		if(type.startsWith("java.lang.")) type = type.replaceFirst("java.lang.", "");
		if(type.contains(".") && !type.startsWith("this.") && !this.name2type.containsKey(type))
		{
			String withoutPackage = type.substring(type.lastIndexOf(".") + 1);
			if(imports.containsKey(withoutPackage))
			{
				String fullName = imports.get(withoutPackage);
				if(fullName.equals(type))
				{
					return withoutPackage + end;
				}
			}
			else
			{
				imports.put(withoutPackage, type);
				return withoutPackage + end;
			}
		}
		return type + end;
	}

	private String getEndOfLine(int currentLine)
	{
		String s = "";
		// if(currentLine % 2 == 0)
		{
			s = " //Line #" + currentLine;
		}
		return s;
	}

	@Override
	public void onRequestResult(ArrayList<CodeFragment> fragments)
	{
	}

	@Override
	public void createSourceCode(List<CodeFragment> in, PrintWriter out)
	{
		this.interpreting = true;
		interpret(in);
		this.interpreting = false;
		this.output = out;
		this.allowedToPrint = true;
		println("package " + classPackage + ";\n");
		Iterator<String> it = imports.values().iterator();
		while(it.hasNext())
		{
			String importName = it.next();
			println("import " + importName + ";");
		}
		for(int index = 0; index < in.size(); index++ )
		{
			CodeFragment fragment = in.get(index);
			this.output = out;
			this.allowedToPrint = !fragment.forbiddenToPrint;
			if(!waiting.isEmpty())
			{
				handleCodeFragment(waiting.pop(), index, in, out);
			}
			handleCodeFragment(fragment, index, in, out);
		}
		println("}");
		out.flush();
	}

	private void handleCodeFragment(CodeFragment fragment, int index, List<CodeFragment> in, PrintWriter out)
	{
		if(fragment.getClass() == NewClassFragment.class)
		{
			handleClassFragment((NewClassFragment)fragment, in, index, out);
			currentClass = (NewClassFragment)fragment;
		}
		else if(fragment.getClass() == FieldFragment.class)
		{
			handleFieldFragment((FieldFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == StartOfMethodFragment.class)
		{
			handleStartOfMethodFragment((StartOfMethodFragment)fragment, in, index, out);
			this.currentMethod = (StartOfMethodFragment)fragment;
		}
		else if(fragment.getClass() == EndOfMethodFragment.class)
		{
			handleEndOfMethodFragment((EndOfMethodFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == LineNumberFragment.class)
		{
			currentLine = ((LineNumberFragment)fragment).line;
		}
		else if(fragment.getClass() == NewArrayFragment.class)
		{
			handleNewArrayFragment((NewArrayFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == NewMultiArrayFragment.class)
		{
			handleNewMultiArrayFragment((NewMultiArrayFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == PutFieldFragment.class)
		{
			handlePutFieldFragment((PutFieldFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == GetFieldFragment.class)
		{
			handleGetFieldFragment((GetFieldFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == IntPushFragment.class)
		{
			handleBiPushFragment((IntPushFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == NewPrimitiveArrayFragment.class)
		{
			handleNewPrimitiveArrayFragment((NewPrimitiveArrayFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == LoadVariableFragment.class)
		{
			handleLoadVariableFragment((LoadVariableFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == StoreVariableFragment.class)
		{
			handleStoreVariableFragment((StoreVariableFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == LdcFragment.class)
		{
			handleLdcFragment((LdcFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == LoadConstantFragment.class)
		{
			handleLoadConstantFragment((LoadConstantFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == ReturnValueFragment.class)
		{
			handleReturnValueFragment((ReturnValueFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == AddFragment.class)
		{
			handleAddFragment((AddFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == SubFragment.class)
		{
			handleSubFragment((SubFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == MulFragment.class)
		{
			handleMulFragment((MulFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == DivFragment.class)
		{
			handleDivFragment((DivFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == ArrayOfArrayLoadFragment.class)
		{
			handleArrayOfArrayLoadFragment((ArrayOfArrayLoadFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == ArrayStoreFragment.class)
		{
			handleArrayStoreFragment((ArrayStoreFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == IfStatementFragment.class)
		{
			handleIfStatementFragment((IfStatementFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == EndOfBlockFragment.class)
		{
			handleEndOfBlockFragment((EndOfBlockFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == ElseStatementFragment.class)
		{
			handleElseStatementFragment((ElseStatementFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == MethodCallFragment.class)
		{
			handleMethodCallFragment((MethodCallFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == ModFragment.class)
		{
			handleModFragment((ModFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == CastFragment.class)
		{
			handleCastFragment((CastFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == LeftShiftFragment.class)
		{
			handleLeftShiftFragment((LeftShiftFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == RightShiftFragment.class)
		{
			handleRightShiftFragment((RightShiftFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == AndFragment.class)
		{
			handleAndFragment((AndFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == OrFragment.class)
		{
			handleOrFragment((OrFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == XorFragment.class)
		{
			handleXorFragment((XorFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == IfNotStatementFragment.class)
		{
			handleIfNotStatementFragment((IfNotStatementFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == PopFragment.class)
		{
			handlePopFragment((PopFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == ReturnFragment.class)
		{
			handleReturnFragment((ReturnFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == DuplicateFragment.class)
		{
			handleDuplicateFragment((DuplicateFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == NewInstanceFragment.class)
		{
			handleNewInstanceFragment((NewInstanceFragment)fragment, in, index, out);
		}

		else if(fragment.getClass() == EqualCheckFragment.class)
		{
			handleEqualCheckFragment((EqualCheckFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == NotEqualCheckFragment.class)
		{
			handleNotEqualCheckFragment((NotEqualCheckFragment)fragment, in, index, out);
		}
		else if(fragment.getClass() == CompareFragment.class)
		{
			handleCompareFragment((CompareFragment)fragment, in, index, out);
		}
	}

	private void handleCompareFragment(CompareFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String right = stack.pop();
		String left = stack.pop();
		stack.push(left + " " + (fragment.inferior ? "<" : ">") + " " + right);
	}

	private void handleNotEqualCheckFragment(NotEqualCheckFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String right = stack.pop();
		String left = stack.pop();
		stack.push("(" + left + " " + "!=" + " " + right + ")");
	}

	private void handleEqualCheckFragment(EqualCheckFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String right = stack.pop();
		String left = stack.pop();
		stack.push("(" + left + " " + "==" + " " + right + ")");
	}

	private void handleNewInstanceFragment(NewInstanceFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		newInstances.push(fragment.type);
	}

	private void handleDuplicateFragment(DuplicateFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		if(!newInstances.isEmpty()) return;
		if(fragment.wait > 0)
		{
			waiting.add(fragment);
			fragment.wait-- ;
		}
		else
		{
			String a = stack.pop();
			stack.push(a);
			stack.push(a);
		}
	}

	private void interpret(List<CodeFragment> in)
	{
		Stack<String> copy = stack;
		Stack<String> tmpstack = new Stack<String>();
		stack = tmpstack;
		StartOfMethodFragment currentMethod = null;
		PrintWriter nullPrinter = new PrintWriter(new StringWriter());
		for(int i = 0; i < in.size(); i++ )
		{
			boolean dontHandle = false;
			CodeFragment fragment = in.get(i);
			if(fragment.getClass() == StartOfMethodFragment.class)
			{
				currentMethod = (StartOfMethodFragment)fragment;
			}
			else if(fragment.getClass() == FieldFragment.class)
			{
				FieldFragment fieldFrag = (FieldFragment)fragment;
			}
			else if(fragment.getClass() == StoreVariableFragment.class)
			{
				StoreVariableFragment storeFrag = (StoreVariableFragment)fragment;
				String type = storeFrag.variableType;
			}
			else if(fragment.getClass() == PutFieldFragment.class)
			{
				PutFieldFragment storeFrag = (PutFieldFragment)fragment;
				if(currentMethod != null && currentMethod.name.equals("<init>"))
				{
					for(int ii = 0; ii < in.size(); ii++ )
					{
						CodeFragment fragment1 = in.get(ii);
						if(fragment1.getClass() == FieldFragment.class)
						{
							FieldFragment fieldFrag = (FieldFragment)fragment1;
							if(fieldFrag.name.equals(storeFrag.fieldName) && fieldFrag.type.equals(storeFrag.fieldType) && !(fieldFrag.access.isFinal() && fieldFrag.initialValue != null))
							{
								fieldFrag.initialValue = stack.peek();
								dontHandle = true;
								storeFrag.forbiddenToPrint = true;
								break;
							}
						}
					}
				}
			}
			if(!dontHandle)
			{
				this.output = nullPrinter;
				this.allowedToPrint = !fragment.forbiddenToPrint;
				if(!waiting.isEmpty())
				{
					handleCodeFragment(waiting.pop(), i, in, nullPrinter);
				}
				handleCodeFragment(fragment, i, in, nullPrinter);
			}
		}

		waiting.clear();
		currentLine = 0;
		indentation = 0;
		initialized.clear();
		name2type.clear();
		currentClass = null;
		typesStack.clear();
		constants.clear();
		stack = copy;
	}

	private void println()
	{
		println("");
	}

	private void println(String s)
	{
		if(allowedToPrint) output.println(s);
	}

	private void handleReturnFragment(ReturnFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		if(in.size() <= index + 1 || in.get(index + 1).getClass() == EndOfMethodFragment.class)
		{
			;
		}
		else
		{
			println(getIndent() + "return;" + getEndOfLine(currentLine));
		}
	}

	private void handlePopFragment(PopFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		println(getIndent() + stack.pop() + ";" + getEndOfLine(currentLine));
	}

	private int countChar(String str, char c)
	{
		int nbr = 0;
		for(int i = 0; i < str.length(); i++ )
			if(str.charAt(i) == c) nbr++ ;
		return nbr;
	}

	private void handleIfNotStatementFragment(IfNotStatementFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String condition = stack.pop();
		println(getIndent() + "if(!" + condition + ")" + getEndOfLine(currentLine));
		println(getIndent() + "{");
		indentation++ ;
	}

	private void handleXorFragment(XorFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String b = stack.pop();
		String a = stack.pop();
		stack.push("(" + a + " || " + b + ")");
	}

	private void handleOrFragment(OrFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String b = stack.pop();
		String a = stack.pop();
		stack.push("(" + a + " " + (fragment.isDouble ? "||" : "|") + " " + b + ")");
	}

	private void handleAndFragment(AndFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String b = stack.pop();
		String a = stack.pop();
		stack.push("(" + a + " " + (fragment.isDouble ? "&&" : "&") + " " + b + ")");
	}

	private void handleRightShiftFragment(RightShiftFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String b = stack.pop();
		String a = stack.pop();
		stack.push(a + ">>" + (!fragment.signed ? ">" : "") + b);
	}

	private void handleLeftShiftFragment(LeftShiftFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String b = stack.pop();
		String a = stack.pop();
		stack.push(a + "<<" + (!fragment.signed ? "<" : "") + b);
	}

	private void handleCastFragment(CastFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String toCast = stack.pop();
		String withoutPreviousCast = toCast;

		String previousType = null;
		if(withoutPreviousCast.startsWith("("))
		{
			previousType = withoutPreviousCast.substring(1, withoutPreviousCast.indexOf(")") - 1);
		}
		else
			previousType = toJava(currentMethod.varName2TypeMap.get(withoutPreviousCast));
		if(previousType.equals(toJava(fragment.to)))
		{
			if(DEBUG) System.out.println("GLSLEncoder > Cancelling cast for " + toCast);
		}
		else
			stack.push("(" + toJava(fragment.to) + ")" + toCast);
	}

	private void handleModFragment(ModFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String a = stack.pop();
		String b = stack.pop();
		stack.push("(" + b + " % " + a + ")");
	}

	private void handleMethodCallFragment(MethodCallFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String s = "";
		String n = fragment.methodName;
		boolean isConstructor = false;
		if(n.equals("<init>"))
		{
			n = "new " + toJava(fragment.methodOwner);
			isConstructor = true;
			if(!newInstances.isEmpty()) newInstances.pop();
		}
		String key = fragment.methodName;
		if(toJava(fragment.methodOwner) != null && !toJava(fragment.methodOwner).equals("null") && !toJava(fragment.methodOwner).trim().equals("")) key = toJava(fragment.methodOwner) + "." + key;
		if(fragment.invokeType == InvokeTypes.SPECIAL && currentMethod.name.equals("<init>") && fragment.methodOwner.equals(currentClass.superclass))
		{
			this.allowedToPrint = false;
		}

		s += n + "(";
		ArrayList<String> args = new ArrayList<String>();
		for(@SuppressWarnings("unused")
		String type : fragment.argumentsTypes)
		{
			String arg = stack.pop();
			if(arg.startsWith("(") && arg.endsWith(")") && countChar(arg, '(') == countChar(arg, ')'))
			{
				arg = arg.substring(1, arg.length() - 1);
			}
			args.add(arg);
		}
		String argsStr = "";
		for(int i = 0; i < args.size(); i++ )
		{
			if(i != 0) argsStr += ", ";
			argsStr += args.get(args.size() - 1 - i);
		}
		s += argsStr;
		s += ")";
		boolean ownerBefore = false;
		boolean parenthesis = true;
		int ownerPosition = 0;
		boolean actsAsField = false;
		if(fragment.invokeType == InvokeTypes.VIRTUAL)
		{
			String owner = stack.pop();
			if(owner.equals(currentClass.className) || owner.equals("this"))
			{
				owner = null;
			}
			else
			{
				if(owner.startsWith("(") && owner.endsWith(")") && countChar(owner, '(') == countChar(owner, ')'))
				{
					owner = owner.substring(1, owner.length() - 1);
				}
			}
			if(!ownerBefore)
			{
				if(actsAsField)
				{
					if(n.length() >= 1)
						s = (owner != null ? owner : "") + "." + n;
					else
						s = (owner != null ? owner : "");
					if(argsStr.length() > 0)
					{
						s += " = " + argsStr;
					}
				}
				else
					s = (owner != null ? owner + "." : "") + n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : "");
			}
			else
				s = (owner != null ? owner : "") + n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : "");
			if(fragment.returnType.equals("void"))
			{
				println(getIndent() + s + ";" + getEndOfLine(currentLine));
			}
			else
				stack.push("(" + s + ")");
		}
		else if(fragment.invokeType == InvokeTypes.STATIC)
		{
			String ownership = "";
			String owner = toJava(fragment.methodOwner);
			if(owner != null && !owner.trim().equals("") && !owner.equals("null")) ownership = owner + (n.length() > 0 ? "." : "");
			stack.push(ownership + n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : ""));
		}
		else
		{
			stack.push(n + (parenthesis ? "(" : "") + argsStr + (parenthesis ? ")" : ""));

			if(fragment.returnType.equals("void") && !fragment.methodName.equals("<init>")) println(getIndent() + stack.pop() + ";");
		}

	}

	private void handleElseStatementFragment(ElseStatementFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		println(getIndent() + "else" + getEndOfLine(currentLine));
		println(getIndent() + "{");
		indentation++ ;
	}

	private void handleEndOfBlockFragment(EndOfBlockFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		indentation-- ;
		println(getIndent() + "}");
	}

	private void handleIfStatementFragment(IfStatementFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String condition = stack.pop();
		println(getIndent() + "if(" + condition + ")" + getEndOfLine(currentLine));
		println(getIndent() + "{");
		indentation++ ;
	}

	private void handleArrayStoreFragment(ArrayStoreFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String result = "";
		String toAdd = "";
		for(int i = 0; i < 2; i++ )
		{
			String lastType = typesStack.pop();
			String copy = lastType;
			int dimensions = 0;
			if(copy != null) while(copy.indexOf("[]") >= 0)
			{
				copy = copy.substring(copy.indexOf("[]") + 2);
				dimensions++ ;
			}
			String val = stack.pop();
			String arrayIndex = "";
			for(int dim = 0; dim < dimensions; dim++ )
			{
				arrayIndex = "[" + stack.pop() + "]" + arrayIndex;
			}
			String name = stack.pop();
			if(i == 1)
				result = val + toAdd + " = " + result;
			else if(i == 0)
			{
				result = val + result;
				toAdd = "[" + name + "]";
			}
		}
		println(getIndent() + result + ";" + getEndOfLine(currentLine));
	}

	private void handleArrayOfArrayLoadFragment(ArrayOfArrayLoadFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String value = stack.pop();
		String name = stack.pop();
		stack.push(name + "[" + value + "]");
		if(name2type.containsKey(name + "[" + value + "]"))
		{
			name2type.put(name + "[" + value + "]", name.substring(0, name.indexOf("[")));
		}
		typesStack.push(name2type.get(name + "[" + value + "]"));
	}

	private void handleDivFragment(DivFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String a = stack.pop();
		String b = stack.pop();
		stack.push("(" + b + "/" + a + ")");
	}

	private void handleMulFragment(MulFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String a = stack.pop();
		String b = stack.pop();
		stack.push("(" + b + "*" + a + ")");
	}

	private void handleSubFragment(SubFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String a = stack.pop();
		String b = stack.pop();
		stack.push("(" + b + "-" + a + ")");
	}

	private void handleAddFragment(AddFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String a = stack.pop();
		String b = stack.pop();
		stack.push("(" + b + "+" + a + ")");
	}

	private void handleReturnValueFragment(ReturnValueFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		println(getIndent() + "return" + " " + stack.pop() + ";" + getEndOfLine(currentLine));
	}

	private void handleLoadConstantFragment(LoadConstantFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		stack.push(fragment.value + "");
	}

	private void handleLdcFragment(LdcFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		if(constants.containsKey(fragment.value))
			stack.push("" + constants.get(fragment.value));
		else if(fragment.value instanceof String)
			stack.push("\"" + fragment.value + "\"");
		else if(fragment.value instanceof Number)
			stack.push("" + fragment.value);
		else if(DEBUG) System.out.println("GLSLEncoder > Invalid value: " + fragment.value + " of type " + fragment.value.getClass().getCanonicalName());
	}

	private void handleStoreVariableFragment(StoreVariableFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String value = stack.pop();
		if(value.startsWith("(") && value.endsWith(")") && countChar(value, '(') == countChar(value, ')'))
		{
			value = value.substring(1, value.length() - 1);
		}
		if(value.equals(fragment.variableName + "+1"))
		{
			println(getIndent() + fragment.variableName + "++;" + getEndOfLine(currentLine));
			return;
		}
		else if(value.equals(fragment.variableName + "-1"))
		{
			println(getIndent() + fragment.variableName + "--;" + getEndOfLine(currentLine));
			return;
		}
		String javaType = toJava(currentMethod.varName2TypeMap.get(fragment.variableName));
		if(javaType.equals("boolean"))
		{
			if(value.equals("0"))
				value = "false";
			else if(value.equals("1")) value = "true";
		}
		else if(javaType.equals("char"))
		{
			try
			{
				value = "'" + Character.valueOf((char)Integer.parseInt(value)) + "'";
			}
			catch(Exception e)
			{
				;
			}
		}
		if(initialized.contains(fragment.variableName))
		{
			println(getIndent() + fragment.variableName + " = " + value + ";" + getEndOfLine(currentLine));
		}
		else
		{
			initialized.add(fragment.variableName);
			println(getIndent() + toJava(currentMethod.varName2TypeMap.get(fragment.variableName)) + " " + fragment.variableName + " = " + value + ";" + getEndOfLine(currentLine));
		}
	}

	private void handleLoadVariableFragment(LoadVariableFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		stack.push(fragment.variableName);
	}

	private void handleNewPrimitiveArrayFragment(NewPrimitiveArrayFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String dimension = "[" + stack.pop() + "]";
		stack.push(fragment.type + dimension);
	}

	private void handleBiPushFragment(IntPushFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		stack.push(fragment.value + "");
	}

	private void handleGetFieldFragment(GetFieldFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String owner = toJava(stack.pop());
		String ownership = owner + ".";
		stack.push(ownership + fragment.fieldName);
		typesStack.push(fragment.fieldType);
	}

	private void handlePutFieldFragment(PutFieldFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String value = stack.pop();
		if(value.startsWith("(") && value.endsWith(")") && countChar(value, '(') == countChar(value, ')'))
		{
			value = value.substring(1, value.length() - 1);
		}
		if(value.equals(fragment.fieldName + "+1"))
		{
			println(getIndent() + fragment.fieldName + "++;" + getEndOfLine(currentLine));
			return;
		}
		else if(value.equals(fragment.fieldName + "-1"))
		{
			println(getIndent() + fragment.fieldName + "--;" + getEndOfLine(currentLine));
			return;
		}
		String javaType = toJava(currentMethod.varName2TypeMap.get(fragment.fieldName));
		if(javaType.equals("boolean"))
		{
			if(value.equals("0"))
				value = "false";
			else if(value.equals("1")) value = "true";
		}
		String owner = stack.pop();
		String ownership = owner + ".";
		for(int i = 0; i < index; i++ )
		{
			CodeFragment frag = in.get(i);
			if(frag.getClass() == FieldFragment.class)
			{
				FieldFragment fieldFrag = (FieldFragment)frag;
				if(fieldFrag.access.isFinal() && fieldFrag.name.equals(fragment.fieldName))
				{
					return;
				}
			}
		}
		println(getIndent() + ownership + fragment.fieldName + " " + "=" + " " + value + ";" + getEndOfLine(currentLine));
	}

	private void handleNewMultiArrayFragment(NewMultiArrayFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String s = "";
		ArrayList<String> list = new ArrayList<String>();
		for(int dim = 0; dim < fragment.dimensions; dim++ )
		{
			list.add(stack.pop());
		}
		for(int dim = 0; dim < fragment.dimensions; dim++ )
		{
			s += "[" + list.get(list.size() - dim - 1) + "]";
		}
		stack.push(toJava(fragment.type) + s);
	}

	private void handleNewArrayFragment(NewArrayFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String s = "[" + stack.pop() + "]";
		stack.push(toJava(fragment.type) + s);
	}

	private void handleEndOfMethodFragment(EndOfMethodFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		indentation-- ;
		println(getIndent() + "}");
	}

	private void handleStartOfMethodFragment(StartOfMethodFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		if(fragment.name.equals("<init>"))
		{
			String n = className;
			initialized.clear();
			println();
			String args = "";
			for(int i = 0; i < fragment.argumentsNames.size(); i++ )
			{
				String s = toJava(fragment.argumentsTypes.get(i)) + " " + fragment.argumentsNames.get(i);
				if(i != 0) args += ", ";
				args += s;
			}
			String accessStr = "";
			if(fragment.access.isPublic())
			{
				accessStr = "public";
			}
			else if(fragment.access.isProtected())
			{
				accessStr = "protected";
			}
			else if(fragment.access.isPrivate())
			{
				accessStr = "private";
			}
			println(getIndent() + accessStr + " " + n + "(" + args + ")\n" + getIndent() + "{");
		}
		else
		{
			initialized.clear();
			println();
			String args = "";
			for(int i = 0; i < fragment.argumentsNames.size(); i++ )
			{
				String s = toJava(fragment.argumentsTypes.get(i)) + " " + fragment.argumentsNames.get(i);
				if(i != 0) args += ", ";
				args += s;
			}
			println(getIndent() + toJava(fragment.returnType) + " " + fragment.name + "(" + args + ")\n" + getIndent() + "{");
		}
		indentation++ ;
	}

	private void handleFieldFragment(FieldFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String storageType = null;
		for(CodeFragment child : fragment.getChildren())
		{
			if(child instanceof AnnotationFragment)
			{
				AnnotationFragment annot = (AnnotationFragment)child;
				println(getIndent() + "@" + toJava(annot.name));
			}
		}
		String str = "";
		if(fragment.access.isPublic())
		{
			str += "public ";
		}
		else if(fragment.access.isPrivate())
		{
			str += "private ";
		}
		else if(fragment.access.isProtected())
		{
			str += "protected ";
		}
		if(fragment.access.isStatic())
		{
			str += "static ";
		}
		if(fragment.access.isFinal())
		{
			str += "final ";
		}
		str += toJava(fragment.type) + " ";
		str += fragment.name;
		if(fragment.initialValue != null) str += " = " + fragment.initialValue;
		println(getIndent() + str + ";");
	}

	@SuppressWarnings("unchecked")
	private void handleClassFragment(NewClassFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		println("// Original class name: " + fragment.className + " compiled from " + fragment.sourceFile + " and of version " + fragment.classVersion);
		classPackage = fragment.className.substring(0, fragment.className.lastIndexOf("."));
		className = fragment.className.substring(fragment.className.lastIndexOf(".") + 1);
		for(CodeFragment child : fragment.getChildren())
		{
			if(child instanceof AnnotationFragment)
			{
				AnnotationFragment annotFragment = (AnnotationFragment)child;
				println("@" + toJava(annotFragment.name));
			}
		}
		String hierarchy = "";
		if(fragment.superclass != null && !fragment.superclass.equals(Object.class.getCanonicalName())) hierarchy += " extends " + toJava(fragment.superclass);
		String access = "";
		if(fragment.access.isPublic())
		{
			access += "public ";
		}
		else if(fragment.access.isProtected())
		{
			access += "protected ";
		}
		else if(fragment.access.isPrivate())
		{
			access += "private ";
		}
		println(access + "class " + className + hierarchy);
		println("{");
		indentation++ ;
	}

	private String getIndent()
	{
		String s = "";
		for(int i = 0; i < indentation; i++ )
			s += "    ";
		return s;
	}

}
