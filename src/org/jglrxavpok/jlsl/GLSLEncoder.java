package org.jglrxavpok.jlsl;

import java.io.*;
import java.util.*;

import org.jglrxavpok.jlsl.GLSL.Attribute;
import org.jglrxavpok.jlsl.GLSL.Extensions;
import org.jglrxavpok.jlsl.GLSL.In;
import org.jglrxavpok.jlsl.GLSL.Layout;
import org.jglrxavpok.jlsl.GLSL.Out;
import org.jglrxavpok.jlsl.GLSL.Uniform;
import org.jglrxavpok.jlsl.fragments.*;

public class GLSLEncoder implements CodeEncoder
{

	private static HashMap<String, String> translations = new HashMap<String, String>();

	static
	{
		setGLSLTranslation("double", "float"); // not every GPU has double
											   // precision;
		setGLSLTranslation(Vec2.class.getCanonicalName(), "vec2");
		setGLSLTranslation(Vec3.class.getCanonicalName(), "vec3");
		setGLSLTranslation(Vec4.class.getCanonicalName(), "vec4");
		setGLSLTranslation(Mat2.class.getCanonicalName(), "mat2");
		setGLSLTranslation(Mat3.class.getCanonicalName(), "mat3");
		setGLSLTranslation(Mat4.class.getCanonicalName(), "mat4");
	}

	public static void setGLSLTranslation(String javaType, String glslType)
	{
		translations.put(javaType, glslType);
	}

	public static void removeGLSLTranslation(String javaType)
	{
		translations.remove(javaType);
	}

	private static String toGLSL(String type)
	{
		String copy = type;
		String end = "";
		while(copy.contains("[]"))
		{
			copy = copy.replaceFirst("\\[\\]", "");
			end+="[]";
		}
		type = copy;
		if(translations.containsKey(type))
		{ 
			return translations.get(type) + end;
		}
		String[] types = typesFromDesc(type, 0);
		if(types.length != 0) return types[0]+end;
		return type+end;
	}
	
	private static String getEndOfLine(int currentLine)
	{
		String s = "";
//		if(currentLine % 2 == 0)
		{
			s = " //Line #" + currentLine;
		}
		return s;
	}

	private static String[] typesFromDesc(String desc, int startPos)
	{
		boolean parsingObjectClass = false;
		boolean parsingArrayClass = false;
		ArrayList<String> types = new ArrayList<String>();
		String currentObjectClass = null;
		String currentArrayClass = null;
		int dims = 1;
		for(int i = startPos; i < desc.length(); i++)
		{
			char c = desc.charAt(i);

			if(!parsingObjectClass && !parsingArrayClass)
			{
				if(c == '[')
				{
					parsingArrayClass = true;
					currentArrayClass = "";
				}
				else if(c == 'L')
				{
					parsingObjectClass = true;
					currentObjectClass = "";
				}
				else if(c == 'I')
				{
					types.add("int");
				}
				else if(c == 'D')
				{
					types.add("double");
				}
				else if(c == 'B')
				{
					types.add("byte");
				}
				else if(c == 'Z')
				{
					types.add("boolean");
				}
				else if(c == 'V')
				{
					types.add("void");
				}
				else if(c == 'J')
				{
					types.add("long");
				}
				else if(c == 'C')
				{
					types.add("char");
				}
				else if(c == 'F')
				{
					types.add("float");
				}
				else if(c == 'S') // TODO: To check
				{
					types.add("short");
				}
			}
			else if(parsingObjectClass)
			{
				if(c == '/')
					c = '.';
				else if(c == ';')
				{
					parsingObjectClass = false;
					types.add(currentObjectClass);
					continue;
				}
				currentObjectClass += c;
			}
			else if(parsingArrayClass)
			{
				if(c == '[')
				{
					dims++;
					continue;
				}
				if(c == '/') c = '.';
				if(c == 'L')
					continue;
				else if(c == ';')
				{
					parsingArrayClass = false;
					String dim = "";
					for(int ii = 0; ii < dims; ii++)
						dim += "[]";
					types.add(currentArrayClass + dim);
					dims = 1;
					continue;
				}
				currentArrayClass += c;
			}
		}
		if(parsingObjectClass)
		{
			types.add(currentObjectClass);
		}
		if(parsingArrayClass)
		{
			String dim = "";
			for(int ii = 0; ii < dims; ii++)
				dim += "[]";
			types.add(currentArrayClass + dim);
		}
		return types.toArray(new String[0]);
	}
	
	private int indentation;
	private int glslversion;
	private NewClassFragment currentClass;
	private ArrayList<String> extensions = new ArrayList<String>();
	private String tab = " ";
	private String tab4 = "    ";
	private int currentLine;
	private Stack<String> stack;
	private Stack<String> typesStack;
	private HashMap<String, String> name2type;
	private ArrayList<String> initialized;

	public GLSLEncoder(int glslversion)
	{
		this.glslversion = glslversion;
		stack = new Stack<String>();
		typesStack = new Stack<String>();
		initialized = new ArrayList<String>();
		name2type = new HashMap<String, String>();
	}

	@Override
	public void createSourceCode(List<CodeFragment> in, PrintWriter out)
	{
		out.print("#version " + glslversion + "\n");

		for(int index = 0; index < in.size(); index++)
		{
			CodeFragment fragment = in.get(index);
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
			else if(fragment.getClass() == BiPushFragment.class)
			{
				handleBiPushFragment((BiPushFragment)fragment, in, index, out);
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
			out.flush();
		}
	}

	private void handleElseStatementFragment(ElseStatementFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		out.println(getIndent()+"else");
		out.println(getIndent()+"{");
		indentation++;
	}

	private void handleEndOfBlockFragment(EndOfBlockFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		indentation--;
		out.println(getIndent()+"}");
	}

	private void handleIfStatementFragment(IfStatementFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String condition = stack.pop();
		out.println(getIndent()+"if("+condition+")");
		out.println(getIndent()+"{");
		indentation++;
	}

	private void handleArrayStoreFragment(ArrayStoreFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String result = "";
		String toAdd = "";
		for(int i = 0; i < 2; i++)
		{
			String lastType = typesStack.pop();
			String copy = lastType;
			int dimensions = 0;
			if(copy != null) while(copy.indexOf("[]") >= 0)
			{
				copy = copy.substring(copy.indexOf("[]") + 2);
				dimensions++;
			}
			String val = stack.pop();
			String arrayIndex = "";
			for(int dim = 0; dim < dimensions; dim++)
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
		out.println(getIndent()+ result + ";" + getEndOfLine(currentLine));
	}

	private void handleArrayOfArrayLoadFragment(ArrayOfArrayLoadFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String value = stack.pop();
		String name = stack.pop();
		stack.push(name+"["+value+"]");
		if(name2type.containsKey(name + "[" + value + "]"))
		{
			name2type.put(name + "[" + value + "]", name.substring(0, name.indexOf("[")));
		}
		typesStack.push(name2type.get(name+"["+value+"]"));
	}

	private void handleDivFragment(DivFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String a = stack.pop();
		String b = stack.pop();
		stack.push(b+"/"+a);
	}
	
	private void handleMulFragment(MulFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String a = stack.pop();
		String b = stack.pop();
		stack.push(b+"*"+a);
	}
	
	private void handleSubFragment(SubFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String a = stack.pop();
		String b = stack.pop();
		stack.push(b+"-"+a);
	}
	
	private void handleAddFragment(AddFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String a = stack.pop();
		String b = stack.pop();
		stack.push(b+"+"+a);
	}

	private void handleReturnValueFragment(ReturnValueFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		out.println(getIndent()+"return"+tab+stack.pop()+";"+getEndOfLine(currentLine));
	}

	private void handleLoadConstantFragment(LoadConstantFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		stack.push(fragment.value+"");
	}

	private void handleLdcFragment(LdcFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		stack.push(""+fragment.value);
	}

	private void handleStoreVariableFragment(StoreVariableFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		if(initialized.contains(fragment.variableName))
		{
			out.println(getIndent() + tab + fragment.variableName + " = " + stack.pop() + ";" + getEndOfLine(currentLine));
		}
		else
		{
			initialized.add(fragment.variableName);
			out.println(getIndent() + toGLSL(fragment.variableType) + tab + fragment.variableName + " = " + stack.pop() + ";" + getEndOfLine(currentLine));
		}
	}

	private void handleLoadVariableFragment(LoadVariableFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		stack.push(fragment.variableName);
	}

	private void handleNewPrimitiveArrayFragment(NewPrimitiveArrayFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String dimension = "["+stack.pop()+"]";
		stack.push(fragment.type+dimension);
	}

	private void handleBiPushFragment(BiPushFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		stack.push(fragment.value+"");
	}

	private void handleGetFieldFragment(GetFieldFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String owner = stack.pop();
		String ownership = owner+".";
		if(owner.equals("this"))
			ownership ="";
		stack.push(ownership+fragment.fieldName);
		typesStack.push(fragment.fieldType);
	}
	
	private void handlePutFieldFragment(PutFieldFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String value = stack.pop();
		String owner = stack.pop();
		String ownership = owner+".";
		if(owner.equals("this"))
			ownership ="";
		out.println(getIndent()+ownership+fragment.fieldName+tab+"="+tab+value+";"+getEndOfLine(currentLine));
	}

	private void handleNewMultiArrayFragment(NewMultiArrayFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String s = "";
		for(int i = 0;i<fragment.dimensions;i++)
			s+="["+fragment.sizes[i]+"]";
		stack.push(toGLSL(fragment.type)+s);
	}

	private void handleNewArrayFragment(NewArrayFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String s = "["+fragment.size+"]";
		stack.push(toGLSL(fragment.type)+s);
	}

	private void handleEndOfMethodFragment(EndOfMethodFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		if(fragment.name.equals("<init>"))
			return;
		out.println("}");
		indentation--;
	}

	private void handleStartOfMethodFragment(StartOfMethodFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		if(fragment.name.equals("<init>"))
			return;
		out.println();
		String args = "";
		for(int i = 0;i<fragment.argumentsNames.size();i++)
		{
			String s = toGLSL(fragment.argumentsTypes.get(i)) + tab + fragment.argumentsNames.get(i);
			if(i != 0)
				args+=", ";
			args+=s;
		}
		out.println(toGLSL(fragment.returnType)+tab+fragment.name+"("+args+")\n{");
		indentation++;
	}

	private void handleFieldFragment(FieldFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		String storageType = null;
		for(CodeFragment child : fragment.getChildren())
		{
			if(child instanceof AnnotationFragment)
			{
				AnnotationFragment annot = (AnnotationFragment)child;
    			if(annot.name.equals(Uniform.class.getCanonicalName()))
    			{
    				storageType = "uniform";
    			}
    			else if(annot.name.equals(Attribute.class.getCanonicalName()))
    			{
    				storageType = "attribute";
    				if(currentClass.superclass.equals(FragmentShader.class.getCanonicalName())){ throw new JLSLException("Attributes are not allowed in fragment shaders"); }
    			}
    			else if(annot.name.equals(In.class.getCanonicalName()))
    			{
    				storageType = "in";
    			}
    			else if(annot.name.equals(Out.class.getCanonicalName()))
    			{
    				storageType = "out";
    			}
    			else if(annot.name.equals(Out.class.getCanonicalName()))
    			{
    				storageType = "varying";
    			}
    
    			else if(annot.name.equals(Layout.class.getCanonicalName()))
    			{
    				int location = (Integer) annot.values.get("location");
    				
    				 if(glslversion > 430 || extensions.contains("GL_ARB_explicit_uniform_location"))
    					 out.print("layout(location = " + location + ") ");
    			}
			}
		}
		if(storageType == null)
		{
			storageType = "uniform";
		}
		if(fragment.access.isFinal())
		{
			out.println("#define"+tab+fragment.name+tab+"="+tab+fragment.initialValue+";");
		}
		else
			out.println(storageType+tab+toGLSL(fragment.type)+tab+fragment.name+";");
	}

	@SuppressWarnings("unchecked")
	private void handleClassFragment(NewClassFragment fragment, List<CodeFragment> in, int index, PrintWriter out)
	{
		out.println("// Original class name: "+fragment.className);
		for(CodeFragment child : fragment.getChildren())
		{
			if(child instanceof AnnotationFragment)
			{
				AnnotationFragment annotFragment = (AnnotationFragment)child;
				out.println();
				if(annotFragment.name.equals(Extensions.class.getCanonicalName()))
				{
					ArrayList<String> values = (ArrayList<String>) annotFragment.values.get("value");
					for(String extension : values)
						out.println("#extension "+extension+" : enable");
				}
			}
		}
	}
	
	private String getIndent()
	{
		String s = "";
		for(int i = 0;i<indentation;i++)
			s+=tab4;
		return s;
	}
	
	
}
