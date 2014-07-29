package org.jglrxavpok.jlsl;

import static org.objectweb.asm.Opcodes.*;

import java.io.*;
import java.util.*;

import org.jglrxavpok.jlsl.GLSL.Attribute;
import org.jglrxavpok.jlsl.GLSL.In;
import org.jglrxavpok.jlsl.GLSL.Layout;
import org.jglrxavpok.jlsl.GLSL.Out;
import org.jglrxavpok.jlsl.GLSL.Substitute;
import org.jglrxavpok.jlsl.GLSL.Uniform;
import org.jglrxavpok.jlsl.GLSL.Varying;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.*;

public class JLSL
{

	public static final boolean			DEBUG		= true;
	private static final String			tab		  = " ";
	private static final String			tab4		 = "    ";

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

	private static String translateToJLSL(String type)
	{
		boolean isArray = false;
		if(type.endsWith("[]"))
		{
			type = type.replace("[]", "");
			isArray = true;
		}
		if(translations.containsKey(type))
		{
			return translations.get(type)+(isArray ? "[]" : "");
		}
		String[] types = typesFromDesc(type, 0);
		if(types.length != 0)
			return types[0];
		return type;
	}

	public static String translateToGLSL(byte[] shaderClass, int glslversion)
	{
		ClassReader reader = new ClassReader(shaderClass);
		return translateToGLSL(reader, glslversion);
	}

	public static String translateToGLSL(InputStream shaderClass, int glslversion) throws IOException
	{
		ClassReader reader = new ClassReader(shaderClass);
		return translateToGLSL(reader, glslversion);
	}

	public static String translateToGLSL(Class<? extends ShaderBase> shaderClass, int glslversion) throws IOException
	{
		ClassReader reader = new ClassReader(shaderClass.getResourceAsStream(shaderClass.getSimpleName() + ".class"));
		return translateToGLSL(reader, glslversion);
	}

	@SuppressWarnings("unchecked")
	public static String translateToGLSL(ClassReader reader, int glslversion) // TODO:
																			  // Organize
																			  // and
																			  // optimize
	{
		try
		{
			ClassNode classNode = new ClassNode();
			reader.accept(classNode, 0);
			if(DEBUG) reader.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);
			List<MethodNode> methodNodes = classNode.methods;
			List<FieldNode> fieldNodes = classNode.fields;
			ArrayList<String> extensions = new ArrayList<String>();
			ArrayList<String> initialized = new ArrayList<String>();
			int version = glslversion;
			StringBuffer buffer = new StringBuffer();
			buffer.append("#version " + version + "\n");

			List<AnnotationNode> list = classNode.visibleAnnotations;
			if(list != null) for(AnnotationNode annotNode : list)
			{
				List<Object> values = annotNode.values;
				for(int index = 0; index < values.size(); index += 2)
				{
					String name = (String)values.get(index);
					if(name.equals("value"))
					{
						List<String> val = (List<String>)values.get(index + 1);
						for(String extension : val)
						{
							extensions.add(extension);
						}
					}
				}
			}
			for(String extension : extensions)
			{
				buffer.append("#extension " + extension + " : enable\n");
			}
			HashMap<String, String> pending = new HashMap<String, String>();
			for(FieldNode field : fieldNodes)
			{
				boolean isPending = false;
				String name = field.name;
				String type = typesFromDesc(field.desc)[0];
				boolean isConstant = false;
				if(!isSupported(field.desc))
				{
					System.err.println("[WARNING] Type " + type + " is not supported by JLSL");
					continue;
				}
				isPending = type.endsWith("[]");
				isConstant = field.access == (ACC_FINAL | ACC_PUBLIC | ACC_STATIC);
				List<AnnotationNode> annotations = field.visibleAnnotations;
				String storageType = null;
				if(annotations != null) for(AnnotationNode annot : annotations)
				{
					if(annot.desc.replace("$", "/").equals("L" + Uniform.class.getCanonicalName().replace(".", "/") + ";"))
					{
						storageType = "uniform";
					}
					else if(annot.desc.replace("$", "/").equals("L" + Attribute.class.getCanonicalName().replace(".", "/") + ";"))
					{
						storageType = "attribute";
						if(classNode.superName.equals(FragmentShader.class.getCanonicalName().replace(".", "/")))
						{
							System.err.println("[ERROR] Attributes are not allowed in fragment shaders");
							return null;
						}
					}
					else if(annot.desc.replace("$", "/").equals("L" + In.class.getCanonicalName().replace(".", "/") + ";"))
					{
						storageType = "in";
					}
					else if(annot.desc.replace("$", "/").equals("L" + Out.class.getCanonicalName().replace(".", "/") + ";"))
					{
						storageType = "out";
					}
					else if(annot.desc.replace("$", "/").equals("L" + Varying.class.getCanonicalName().replace(".", "/") + ";"))
					{
						storageType = "varying";
					}

					else if(annot.desc.replace("$", "/").equals("L" + Layout.class.getCanonicalName().replace(".", "/") + ";"))
					{
						int nbr = 0;
						List<Object> values = annot.values;
						for(int index = 0; index < values.size(); index += 2)
						{
							String valName = (String)values.get(index);
							if(valName.equals("pos"))
							{
								nbr = (Integer)values.get(index + 1);
							}
						}
						if(version > 430 || extensions.contains("GL_ARB_explicit_uniform_location")) buffer.append("layout(location = " + nbr + ") ");
					}
				}
				if(storageType == null)
				{
					storageType = "uniform";
				}
				if(!isConstant)
				{
					if(!isPending)
						buffer.append(storageType + tab + translateToJLSL(type) + tab + name + ";\n");
					else
					{
						buffer.append(storageType + tab + "$pending_"+name+"$;\n");
						pending.put(name, null);
					}
				}
				else
				{
					buffer.append("#define" + tab + name + tab + field.value + "\n");
				}
			}
			Stack<String> toStore = new Stack<String>();
			Collections.sort(methodNodes, new Comparator<MethodNode>()
			{

				@Override
				public int compare(MethodNode arg0, MethodNode arg1)
				{
					if(arg0.name.equals("main"))
						return 1;
					if(arg1.name.equals("main"))
						return -1;
					return 0;
				}
				
			});
			
			
			for(MethodNode node : methodNodes)
			{
				List<LocalVariableNode> localVariables = node.localVariables;
				HashMap<Integer, String> varNameMap = new HashMap<Integer, String>();
				HashMap<Integer, String> varTypeMap = new HashMap<Integer, String>();
				for(LocalVariableNode var : localVariables)
				{
					varNameMap.put(var.index, var.name);
					varTypeMap.put(var.index, typesFromDesc(var.desc)[0]);
				}
				buffer.append(handleMethodNode(node, toStore, initialized, varTypeMap, varNameMap, pending));
			}

			String finalString = buffer.toString();
			Iterator<String> it = pending.keySet().iterator();
			while(it.hasNext())
			{
				String key = it.next();
				finalString = finalString.replace("$pending_"+key+"$", pending.get(key)+tab+key);
			}
			return finalString;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static StringBuffer handleMethodNode(MethodNode node, Stack<String> toStore, ArrayList<String> initialized, HashMap<Integer, String> varTypeMap,
			HashMap<Integer, String> varNameMap, HashMap<String, String> pending)
	{
		int currentLine = 0;
		HashMap<String, String> varNameTypeMap = new HashMap<String, String>();
		StringBuffer buffer = new StringBuffer();
		if(!node.name.equals("<init>"))
		{
    		String returnType = node.desc.substring(node.desc.indexOf(')') + 1);
    		buffer.append("\n" + translateToJLSL(typesFromDesc(returnType)[0]) + tab + node.name + "(");
    		String[] argsTypes = typesFromDesc(node.desc.substring(node.desc.indexOf('(')+1, node.desc.indexOf(')')));
    		int argIndex = 0;
    		for(String argType : argsTypes)
    		{
    			if(argIndex != 0)
    				buffer.append(", ");
    			buffer.append(translateToJLSL(argType)+tab+varNameMap.get(argIndex+1));
    			argIndex++;
    		}
    		buffer.append(")\n{"+getEndOfLine(currentLine)+"\n");
		}
		Stack<String> typesStack = new Stack<String>();
		InsnList instructions = node.instructions;

		for(int index = 0; index < instructions.size(); index++ )
		{
			AbstractInsnNode ainsnNode = instructions.get(index);
			if(DEBUG) System.err.println(ainsnNode);
			if(ainsnNode.getType() == AbstractInsnNode.INSN)
			{
				InsnNode insnNode = (InsnNode)ainsnNode;
				if(insnNode.getOpcode() == ICONST_0)
				{
					toStore.push("0");
				}
				else if(insnNode.getOpcode() == ICONST_1)
				{
					toStore.push("1");
				}
				else if(insnNode.getOpcode() == ICONST_2)
				{
					toStore.push("2");
				}
				else if(insnNode.getOpcode() == ICONST_3)
				{
					toStore.push("3");
				}
				else if(insnNode.getOpcode() == ICONST_4)
				{
					toStore.push("4");
				}
				else if(insnNode.getOpcode() == ICONST_5)
				{
					toStore.push("5");
				}

				else if(insnNode.getOpcode() == DCONST_0)
				{
					toStore.push("0");
				}
				else if(insnNode.getOpcode() == DCONST_1)
				{
					toStore.push("1");
				}

				else if(insnNode.getOpcode() == FCONST_0)
				{
					toStore.push("0.0");
				}
				else if(insnNode.getOpcode() == FCONST_1)
				{
					toStore.push("1.0");
				}
				else if(insnNode.getOpcode() == FCONST_2)
				{
					toStore.push("2.0");
				}

				else if(ainsnNode.getOpcode() == LRETURN || ainsnNode.getOpcode() == DRETURN || ainsnNode.getOpcode() == FRETURN || ainsnNode.getOpcode() == IRETURN
						|| ainsnNode.getOpcode() == ARETURN)
				{
					buffer.append(tab4 + "return " + toStore.pop() + ";"+getEndOfLine(currentLine)+"\n");
				}
				else if(ainsnNode.getOpcode() == LADD || ainsnNode.getOpcode() == DADD || ainsnNode.getOpcode() == FADD || ainsnNode.getOpcode() == IADD)
				{
					String a = toStore.pop();
					String b = toStore.pop();
					toStore.push(b + "+" + a);
				}
				else if(ainsnNode.getOpcode() == LSUB || ainsnNode.getOpcode() == DSUB || ainsnNode.getOpcode() == FSUB || ainsnNode.getOpcode() == ISUB)
				{
					String a = toStore.pop();
					String b = toStore.pop();
					toStore.push(b + "-" + a);
				}
				else if(ainsnNode.getOpcode() == LMUL || ainsnNode.getOpcode() == DMUL || ainsnNode.getOpcode() == FMUL || ainsnNode.getOpcode() == IMUL)
				{
					String a = toStore.pop();
					String b = toStore.pop();
					toStore.push(b + "*" + a);
				}
				else if(ainsnNode.getOpcode() == LDIV || ainsnNode.getOpcode() == DDIV || ainsnNode.getOpcode() == FDIV || ainsnNode.getOpcode() == IDIV)
				{
					String a = toStore.pop();
					String b = toStore.pop();
					toStore.push(b + "/" + a);
				}
				
				else if(ainsnNode.getOpcode() == AASTORE || ainsnNode.getOpcode() == IASTORE || ainsnNode.getOpcode() == BASTORE
						|| ainsnNode.getOpcode() == LASTORE || ainsnNode.getOpcode() == SASTORE || ainsnNode.getOpcode() == FASTORE
						|| ainsnNode.getOpcode() == DASTORE || ainsnNode.getOpcode() == CASTORE)
				{
					String result = "";
					String toAdd = "";
					for(int i = 0;i<2;i++)
					{
    					String lastType = typesStack.pop();
    					String copy = lastType;
    					int dimensions = 0;
    					if(copy != null)
        					while(copy.indexOf("[]") >= 0)
        					{
        						copy = copy.substring(copy.indexOf("[]")+2);
        						dimensions++;
        					}
    					String val = toStore.pop();
    					String arrayIndex = "";
    					for(int dim = 0;dim<dimensions;dim++)
    					{
    						arrayIndex = "["+toStore.pop()+"]" + arrayIndex;
    					}
    					String name = toStore.pop();
    					if(i == 1)
    						result = val+toAdd+" = "+result;
    					else if(i == 0)
    					{
    						result = val + result;
    						toAdd = "["+name+"]";
    					}
					}
					buffer.append(tab4+result+";"+getEndOfLine(currentLine)+"\n");
				}
				else if(ainsnNode.getOpcode() == AALOAD)
				{
					String val = toStore.pop();
					String name = toStore.pop();
					toStore.push(name+"["+val+"]");
					if(varNameTypeMap.containsKey(name+"["+val+"]"))
					{
						varNameTypeMap.put(name+"["+val+"]", name.substring(0, name.indexOf("[")));
					}
					typesStack.push(varNameTypeMap.get(name+"["+val+"]"));
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.LDC_INSN)
			{
				LdcInsnNode ldc = (LdcInsnNode)ainsnNode;
				toStore.push("" + ldc.cst);
			}
			else if(ainsnNode.getType() == AbstractInsnNode.VAR_INSN)
			{
				VarInsnNode varNode = (VarInsnNode)ainsnNode;
				int operand = varNode.var;
				if(ainsnNode.getOpcode() == ISTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL("int") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";"+getEndOfLine(currentLine)+"\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";"+getEndOfLine(currentLine)+"\n");
					}
				}
				else if(ainsnNode.getOpcode() == DSTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL("double") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";"+getEndOfLine(currentLine)+"\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";"+getEndOfLine(currentLine)+"\n");
					}
				}
				else if(ainsnNode.getOpcode() == LSTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL("long") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";"+getEndOfLine(currentLine)+"\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";"+getEndOfLine(currentLine)+"\n");
					}
				}
				else if(ainsnNode.getOpcode() == FSTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL("float") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";"+getEndOfLine(currentLine)+"\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";"+getEndOfLine(currentLine)+"\n");
					}
				}
				else if(ainsnNode.getOpcode() == ASTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL(varTypeMap.get(operand)) + " " + varNameMap.get(operand) + " = " + toStore.pop() + ";"+getEndOfLine(currentLine)+"\n");
						initialized.add(varNameMap.get(operand));
					}
					else
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";"+getEndOfLine(currentLine)+"\n");
				}
				else if(ainsnNode.getOpcode() == FLOAD)
				{
					toStore.push(varNameMap.get(operand));
				}
				else if(ainsnNode.getOpcode() == LLOAD)
				{
					toStore.push(varNameMap.get(operand));
				}
				else if(ainsnNode.getOpcode() == ILOAD)
				{
					toStore.push(varNameMap.get(operand));
				}
				else if(ainsnNode.getOpcode() == DLOAD)
				{
					toStore.push(varNameMap.get(operand));
				}
				else if(ainsnNode.getOpcode() == ALOAD)
				{
					toStore.push(varNameMap.get(operand));
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.FIELD_INSN)
			{
				FieldInsnNode fieldNode = (FieldInsnNode)ainsnNode;
				if(fieldNode.getOpcode() == PUTFIELD)
				{
					String val = toStore.pop();
					String owner = toStore.pop();
					if(pending.containsKey(fieldNode.name))
					{
						String s = pending.get(fieldNode.name);
						if(s == null)
							s = "";
						pending.put(fieldNode.name, s+val);
					}
					else
						buffer.append(tab4 + (owner.equals("this") ? "" : (owner + ".")) + fieldNode.name + " = " + val + ";"+getEndOfLine(currentLine)+"\n");
				}
				else if(fieldNode.getOpcode() == GETFIELD)
				{
					String ownership = toStore.pop();
					if(ownership.equals("this"))
						ownership = "";
					else
						ownership += ".";
					toStore.push(ownership + fieldNode.name);
					typesStack.push(typesFromDesc(fieldNode.desc)[0]);
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.INT_INSN)
			{
				IntInsnNode intNode = (IntInsnNode)ainsnNode;
				int operand = intNode.operand;
				if(intNode.getOpcode() == BIPUSH)
				{
					toStore.push(""+operand);
				}
				else if(intNode.getOpcode() == NEWARRAY)
				{
					String type = translateToJLSL(Printer.TYPES[operand]);
					String s = type+toStore.pop();
					toStore.push(s);
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.TYPE_INSN)
			{
				TypeInsnNode typeNode = (TypeInsnNode)ainsnNode;
				String operand = typeNode.desc;
				if(typeNode.getOpcode() == ANEWARRAY)
				{
					String s = translateToJLSL(operand.replace("/", "."))+"["+toStore.pop()+"]";
					toStore.push(s);
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.MULTIANEWARRAY_INSN)
			{
				MultiANewArrayInsnNode multiArrayNode = (MultiANewArrayInsnNode)ainsnNode;
				String operand = multiArrayNode.desc;
				String desc = translateToJLSL(translateToJLSL(operand).replace("[]", ""));
				String s = desc;
				if(desc.length() == 1)
					s = typesFromDesc(desc)[0];
				ArrayList<String> list = new ArrayList<String>();
				for(int dim = 0;dim<multiArrayNode.dims;dim++)
				{
					list.add(toStore.pop());
				}
				for(int dim = 0;dim<multiArrayNode.dims;dim++)
				{
					s+="["+list.get(list.size()-dim-1)+"]";
				}
				toStore.push(s);
			}
			else if(ainsnNode.getType() == AbstractInsnNode.LINE)
			{
				LineNumberNode lineNode = (LineNumberNode)ainsnNode;
				if(toStore.size() > 0)
				if(toStore.peek().contains("(") && toStore.peek().contains(")"))
				{
					buffer.append(tab4+toStore.pop()+";"+getEndOfLine(currentLine)+"\n");
				}
				currentLine = lineNode.line;
			}
			else if(ainsnNode.getType() == AbstractInsnNode.METHOD_INSN)
			{
				MethodInsnNode methodNode = (MethodInsnNode)ainsnNode;
				if(methodNode.getOpcode() == INVOKESPECIAL)
				{
					String desc = methodNode.desc;
					String margs = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));
					String[] margsArray = typesFromDesc(margs);
					ArrayList<String> argsList = new ArrayList<String>();
					for(int i = 0; i < margsArray.length; i++ )
					{
						argsList.add(toStore.pop());
					}
					margs = null;
					for(String s : argsList)
					{
						if(margs == null)
							margs = s;
						else
							margs = s + ", " + margs;
					}
					String n = methodNode.name;
					if(margs == null)
						margs = "";
					if(methodNode.name.equals("<init>"))
					{
						n = "";
						toStore.push(translateToJLSL(typesFromDesc("L" + methodNode.owner + ";")[0]) + n + "(" + margs + ")");
					}
					else
					{
						toStore.push(n + "(" + margs + ")");
					}
				}
				else if(methodNode.getOpcode() == INVOKEVIRTUAL)
				{
					String desc = methodNode.desc;
					String margs = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));
					String[] margsArray = typesFromDesc(margs);
					ArrayList<String> argsList = new ArrayList<String>();
					for(int i = 0; i < margsArray.length; i++ )
					{
						argsList.add(toStore.pop());
					}
					margs = null;
					for(String s : argsList)
					{
						if(margs == null)
							margs = s;
						else
							margs = s + ", " + margs;
					}
					String n = methodNode.name;
					AnnotationNode substituteAnnotation = getAnnotNode(methodNode.owner, n, methodNode.desc, Substitute.class.getCanonicalName());
					boolean ownerBefore = false;
					boolean parenthesis = true;
					if(substituteAnnotation != null)
					{
						List<Object> values = substituteAnnotation.values;
						for(int i = 0;i<values.size();i+=2)
						{
							String name = (String)values.get(i);
							if(name.equals("value"))
							{
								n = (String)values.get(i+1);
							}
							else if(name.equals("ownerBefore"))
							{
								ownerBefore = (Boolean)values.get(i+1);
							}
							else if(name.equals("usesParenthesis"))
							{
								parenthesis = (Boolean)values.get(i+1);
							}
						}
					}
					if(methodNode.name.equals("<init>"))
					{
						n = "";
					}
					if(margs == null)
						margs = "";
					else if(parenthesis)
						margs = ", " + margs;
					if(!ownerBefore)
					{
						toStore.push(n + (parenthesis ? "(" :"") + toStore.pop() + margs + (parenthesis ? ")" :""));
					}
					else
					{
						toStore.push(toStore.pop() + n + (parenthesis ? "(" :"")  + margs + (parenthesis ? ")" :""));
					}
				}
			}
		}
		if(!node.name.equals("<init>"))
		{
			buffer.append("}"+getEndOfLine(currentLine)+"\n");
		}
		else
			buffer.delete(0, buffer.length()-1);
		return buffer;
	}

	private static String getEndOfLine(int currentLine)
	{
		String s = "";
//		if(currentLine % 5 == 0)
		{
			s = " //Line #"+currentLine;
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
		for(int i = startPos; i < desc.length(); i++ )
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
				if(c == '/')
					c = '.';
				if(c == 'L')
					continue;
				else if(c == ';')
				{
					parsingArrayClass = false;
					String dim = "";
					for(int ii = 0;ii<dims;ii++)
						dim+="[]";
					types.add(currentArrayClass+dim);
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
			for(int ii = 0;ii<dims;ii++)
				dim+="[]";
			types.add(currentArrayClass+dim);
		}
		return types.toArray(new String[0]);
	}

	private static String[] typesFromDesc(String desc)
	{
		return typesFromDesc(desc, 0);
	}

	private static boolean isSupported(String desc)
	{
		String[] types = typesFromDesc(desc);
		for(String type : types)
		{
			if(type.endsWith("[]"))
				type = type.replace("[]", "");
			type = translateToJLSL(type);
			if(!type.equals("int") && !type.equals("float") && !type.equals("double") && !type.equals("boolean") && !type.equals("vec2") && !type.equals("vec3")
					&& !type.equals("vec4") && !type.equals("mat3") && !type.equals("mat2") && !type.equals("mat4"))
			{
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private static AnnotationNode getAnnotNode(String methodClass, String methodName, String methodDesc, String annotationClass)
	{
		try
		{
			ClassReader reader = new ClassReader(JLSL.class.getResourceAsStream("/"+methodClass.replace(".", "/")+".class"));
			ClassNode classNode = new ClassNode();
			reader.accept(classNode, 0);
			List<MethodNode> methodList = classNode.methods;
			for(MethodNode methodNode : methodList)
			{
				if(methodNode.name.equals(methodName) && methodNode.desc.equals(methodDesc))
				{
					List<AnnotationNode> annots = methodNode.visibleAnnotations;
					if(annots != null)
    					for(AnnotationNode annot : annots)
    					{
    						if(annot.desc.replace("$", "/").equals("L" + annotationClass.replace(".", "/") + ";"))
    						{
    							return annot;
    						}
    					}
					else
						return null;
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
		return null;
	}
}
