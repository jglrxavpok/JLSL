package org.jglrxavpok.jlsl;

import static org.objectweb.asm.Opcodes.*;

import java.io.*;
import java.lang.annotation.*;
import java.util.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.*;

public class JLSL
{

	public static final boolean			DEBUG		= false;
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
		if(translations.containsKey(type))
		{
			return translations.get(type);
		}
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
			for(FieldNode field : fieldNodes)
			{
				String name = field.name;
				String type = typesFromDesc(field.desc)[0];
				boolean isConstant = false;
				if(!isSupported(field.desc))
				{
					System.err.println("[WARNING] Type " + type + " is not supported by JLSL");
					continue;
				}
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
					buffer.append(storageType + tab + translateToJLSL(type) + tab + name + ";\n");
				}
				else
				{
					buffer.append("#define" + tab + name + tab + field.value + "\n");
				}
			}
			Stack<String> toStore = new Stack<String>();
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
				handleMethodNode(node, buffer, toStore, initialized, varTypeMap, varNameMap);
			}

			return buffer.toString();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private static void handleMethodNode(MethodNode node, StringBuffer buffer, Stack<String> toStore, ArrayList<String> initialized, HashMap<Integer, String> varTypeMap,
			HashMap<Integer, String> varNameMap)
	{
		if(node.name.equals("<init>")) return;
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
		buffer.append(")\n{\n");
			
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
					buffer.append(tab4 + "return " + toStore.pop() + ";\n");
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
						buffer.append(tab4 + translateToJLSL("int") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";\n");
					}
				}
				else if(ainsnNode.getOpcode() == DSTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL("double") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";\n");
					}
				}
				else if(ainsnNode.getOpcode() == LSTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL("long") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";\n");
					}
				}
				else if(ainsnNode.getOpcode() == FSTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL("float") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";\n");
					}
				}
				else if(ainsnNode.getOpcode() == ASTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL(varTypeMap.get(operand)) + " " + varNameMap.get(operand) + " = " + toStore.pop() + ";\n");
						initialized.add(varNameMap.get(operand));
					}
					else
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";\n");
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
					buffer.append(tab4 + (owner.equals("this") ? "" : (owner + ".")) + fieldNode.name + " = " + val + ";\n");
				}
				else if(fieldNode.getOpcode() == GETFIELD)
				{
					String ownership = toStore.pop();
					if(ownership.equals("this"))
						ownership = "";
					else
						ownership += ".";
					toStore.push(ownership + fieldNode.name);
				}
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
					if(methodNode.name.equals("<init>"))
					{
						n = "";
						toStore.push(translateToJLSL(typesFromDesc("L" + methodNode.owner + ";")[0]) + n + "(" + margs + ")");
					}
					else
						toStore.push(n + "(" + margs + ")");

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
					if(methodNode.name.equals("<init>"))
					{
						n = "";
					}
					if(margs == null)
						margs = "";
					else
						margs = ", " + margs;
					toStore.push(n + "(" + toStore.pop() + margs + ")");
				}
			}
		}
		buffer.append("}\n");
	}

	private static String[] typesFromDesc(String desc, int startPos)
	{
		boolean parsingObjectClass = false;
		ArrayList<String> types = new ArrayList<String>();
		String currentObjectClass = null;
		for(int i = startPos; i < desc.length(); i++ )
		{
			char c = desc.charAt(i);

			if(!parsingObjectClass)
			{
				if(c == 'L')
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
			else
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
		}
		if(parsingObjectClass)
		{
			types.add(currentObjectClass);
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
			type = translateToJLSL(type);
			if(!type.equals("int") && !type.equals("float") && !type.equals("double") && !type.equals("boolean") && !type.equals("vec2") && !type.equals("vec3")
					&& !type.equals("vec4"))
			{
				return false;
			}
		}
		return true;
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Extensions
	{
		String[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Layout
	{
		int pos();
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Attribute
	{
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Uniform
	{
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Varying
	{
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface In
	{
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface Out
	{
	}
}
