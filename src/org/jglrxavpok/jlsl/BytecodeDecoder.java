package org.jglrxavpok.jlsl;

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.BASTORE;
import static org.objectweb.asm.Opcodes.BIPUSH;
import static org.objectweb.asm.Opcodes.CASTORE;
import static org.objectweb.asm.Opcodes.DADD;
import static org.objectweb.asm.Opcodes.DASTORE;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DCONST_1;
import static org.objectweb.asm.Opcodes.DDIV;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DMUL;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.DSUB;
import static org.objectweb.asm.Opcodes.FADD;
import static org.objectweb.asm.Opcodes.FASTORE;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FCONST_1;
import static org.objectweb.asm.Opcodes.FCONST_2;
import static org.objectweb.asm.Opcodes.FDIV;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FMUL;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.FSUB;
import static org.objectweb.asm.Opcodes.F_APPEND;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IADD;
import static org.objectweb.asm.Opcodes.IASTORE;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ICONST_2;
import static org.objectweb.asm.Opcodes.ICONST_3;
import static org.objectweb.asm.Opcodes.ICONST_4;
import static org.objectweb.asm.Opcodes.ICONST_5;
import static org.objectweb.asm.Opcodes.IDIV;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.IMUL;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.ISUB;
import static org.objectweb.asm.Opcodes.LADD;
import static org.objectweb.asm.Opcodes.LASTORE;
import static org.objectweb.asm.Opcodes.LDIV;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LMUL;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.LSUB;
import static org.objectweb.asm.Opcodes.NEWARRAY;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.SASTORE;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.jglrxavpok.jlsl.GLSL.Attribute;
import org.jglrxavpok.jlsl.GLSL.In;
import org.jglrxavpok.jlsl.GLSL.Layout;
import org.jglrxavpok.jlsl.GLSL.Out;
import org.jglrxavpok.jlsl.GLSL.Substitute;
import org.jglrxavpok.jlsl.GLSL.Uniform;
import org.jglrxavpok.jlsl.GLSL.Varying;
import org.jglrxavpok.jlsl.fragments.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

public class BytecodeDecoder implements CodeDecoder
{

	public static final boolean DEBUG = false;
	private static String tab = " ";
	private static String tab4 = "    ";

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
		if(translations.containsKey(type)){ return translations.get(type) + (isArray ? "[]" : ""); }
		String[] types = typesFromDesc(type, 0);
		if(types.length != 0) return types[0];
		return type;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleClass(Object data, List<CodeFragment> out)
	{
		try
		{
			ClassReader reader;
			if(data instanceof byte[])
			{
				reader = new ClassReader((byte[]) data);
			}
			else if(data instanceof InputStream)
			{
				reader = new ClassReader((InputStream) data);
			}
			else if(data instanceof Class<?>)
			{
				reader = new ClassReader(((Class<?>) data).getResourceAsStream(((Class<?>) data).getSimpleName() + ".class"));
			}
			else throw new JLSLException("Invalid type: " + data.getClass().getCanonicalName());
			ClassNode classNode = new ClassNode();
			reader.accept(classNode, 0);
			if(DEBUG) reader.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);

			NewClassFragment classFragment = new NewClassFragment();
			classFragment.className = classNode.name.replace("/", ".").replace("$", ".");
			classFragment.superclass = classNode.superName.replace("/", ".").replace("$", ".");
			classFragment.access = new AccessPolicy(classNode.access);
			List<String> interfaces = classNode.interfaces;
			classFragment.interfaces = interfaces.toArray(new String[0]);

			out.add(classFragment);
			List<MethodNode> methodNodes = classNode.methods;
			List<FieldNode> fieldNodes = classNode.fields;
			ArrayList<String> initialized = new ArrayList<String>();

			List<AnnotationNode> list = classNode.visibleAnnotations;
			if(list != null) 
				for(AnnotationNode annotNode : list)
    			{
    				AnnotationFragment annotFragment = createFromNode(annotNode);
    				classFragment.addChild(annotFragment);
    			}
			for(FieldNode field : fieldNodes)
			{
				String name = field.name;
				String type = typesFromDesc(field.desc)[0];
				FieldFragment fieldFragment = new FieldFragment();
				fieldFragment.name = name;
				fieldFragment.type = type;
				fieldFragment.access = new AccessPolicy(field.access);
				List<AnnotationNode> annotations = field.visibleAnnotations;
				if(annotations != null)
    				for(AnnotationNode annotNode : annotations)
    				{
    					AnnotationFragment annotFragment = createFromNode(annotNode);
    					fieldFragment.addChild(annotFragment);
    				}
				out.add(fieldFragment);
			}
			Stack<String> toStore = new Stack<String>();
			Collections.sort(methodNodes, new Comparator<MethodNode>()
			{

				@Override
				public int compare(MethodNode arg0, MethodNode arg1)
				{
					if(arg0.name.equals("main")) return 1;
					if(arg1.name.equals("main")) return -1;
					return 0;
				}

			});

			for(MethodNode node : methodNodes)
			{
				List<LocalVariableNode> localVariables = node.localVariables;
				StartOfMethodFragment startOfMethodFragment = new StartOfMethodFragment();
				startOfMethodFragment.name = node.name;
				startOfMethodFragment.returnType = typesFromDesc(node.desc.substring(node.desc.indexOf(")")+1))[0];
				for(LocalVariableNode var : localVariables)
				{
					startOfMethodFragment.varNameMap.put(var.index, var.name);
					startOfMethodFragment.varTypeMap.put(var.index, typesFromDesc(var.desc)[0]);
				}
				String[] argsTypes = typesFromDesc(node.desc.substring(node.desc.indexOf('(') + 1, node.desc.indexOf(')')));
				int argIndex = 0;
				for(String argType : argsTypes)
				{
					startOfMethodFragment.argumentsTypes.add(argType);
					startOfMethodFragment.argumentsNames.add(startOfMethodFragment.varNameMap.get(argIndex + 1));
					argIndex++;
				}
				List<AnnotationNode> annots = node.visibleAnnotations;
				if(node.visibleAnnotations != null)
					for(AnnotationNode annotNode : annots)
					{
						startOfMethodFragment.addChild(createFromNode(annotNode));
					}
				out.add(startOfMethodFragment);
				handleMethodNode(node, toStore, initialized, startOfMethodFragment.varTypeMap, startOfMethodFragment.varNameMap, out);
				EndOfMethodFragment endOfMethodFragment = new EndOfMethodFragment();
				endOfMethodFragment.name = startOfMethodFragment.name;
				endOfMethodFragment.argumentsNames = startOfMethodFragment.argumentsNames;
				endOfMethodFragment.argumentsTypes = startOfMethodFragment.argumentsTypes;
				endOfMethodFragment.returnType = startOfMethodFragment.returnType;
				endOfMethodFragment.varNameMap = startOfMethodFragment.varNameMap;
				endOfMethodFragment.varTypeMap = startOfMethodFragment.varTypeMap;
				endOfMethodFragment.getChildren().addAll(startOfMethodFragment.getChildren());
				out.add(endOfMethodFragment);
			}
			return;
		}
		catch(Exception e)
		{
			throw new JLSLException(e);
		}
	}

	private AnnotationFragment createFromNode(AnnotationNode annotNode)
	{
		AnnotationFragment annotFragment = new AnnotationFragment();
		annotFragment.name = typesFromDesc(annotNode.desc)[0].replace("/", ".").replace("$", ".");
		List<Object> values = annotNode.values;
		if(values != null)
		for(int index = 0; index < values.size(); index += 2)
		{
			String key = (String) values.get(index);
			Object value = values.get(index + 1);
			annotFragment.values.put(key, value);
		}
		return annotFragment;
	}

	@SuppressWarnings("unchecked")
	private static void handleMethodNode(MethodNode node, Stack<String> toStore, ArrayList<String> initialized, HashMap<Integer, String> varTypeMap, HashMap<Integer, String> varNameMap, List<CodeFragment> out)
	{
		int currentLine = 0;
		int lineJustJumped = 0;
		StringBuffer buffer = new StringBuffer(); // TODO: delete
		HashMap<String, String> varNameTypeMap = new HashMap<String, String>();
		if(!node.name.equals("<init>"))
		{
			String returnType = node.desc.substring(node.desc.indexOf(')') + 1);
			buffer.append("\n" + translateToJLSL(typesFromDesc(returnType)[0]) + tab + node.name + "(");
			String[] argsTypes = typesFromDesc(node.desc.substring(node.desc.indexOf('(') + 1, node.desc.indexOf(')')));
			int argIndex = 0;
			for(String argType : argsTypes)
			{
				if(argIndex != 0) buffer.append(", ");
				buffer.append(translateToJLSL(argType) + tab + varNameMap.get(argIndex + 1));
				argIndex++;
			}
			buffer.append(")\n{" + getEndOfLine(currentLine) + "\n");
		}
		Stack<String> typesStack = new Stack<String>();
		Stack<LabelNode> toJump = new Stack<LabelNode>();
		InsnList instructions = node.instructions;
		List<Label> hasJumpedTo = new ArrayList<Label>();
		Label currentLabel = null;
		Label lastLabel = null;
		int lastFrameType = 0;
		boolean hasReachedAGoto = false;
		for(int index = 0; index < instructions.size(); index++)
		{
			AbstractInsnNode ainsnNode = instructions.get(index);
			if(DEBUG) System.err.println(ainsnNode);
			if(ainsnNode.getType() == AbstractInsnNode.INSN)
			{
				InsnNode insnNode = (InsnNode) ainsnNode;
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

				else if(ainsnNode.getOpcode() == LRETURN || ainsnNode.getOpcode() == DRETURN || ainsnNode.getOpcode() == FRETURN || ainsnNode.getOpcode() == IRETURN || ainsnNode.getOpcode() == ARETURN)
				{
					buffer.append(tab4 + "return " + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
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

				else if(ainsnNode.getOpcode() == AASTORE || ainsnNode.getOpcode() == IASTORE || ainsnNode.getOpcode() == BASTORE || ainsnNode.getOpcode() == LASTORE || ainsnNode.getOpcode() == SASTORE || ainsnNode.getOpcode() == FASTORE || ainsnNode.getOpcode() == DASTORE || ainsnNode.getOpcode() == CASTORE)
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
						String val = toStore.pop();
						String arrayIndex = "";
						for(int dim = 0; dim < dimensions; dim++)
						{
							arrayIndex = "[" + toStore.pop() + "]" + arrayIndex;
						}
						String name = toStore.pop();
						if(i == 1)
							result = val + toAdd + " = " + result;
						else if(i == 0)
						{
							result = val + result;
							toAdd = "[" + name + "]";
						}
					}
					buffer.append(tab4 + result + ";" + getEndOfLine(currentLine) + "\n");
				}
				else if(ainsnNode.getOpcode() == AALOAD)
				{
					String val = toStore.pop();
					String name = toStore.pop();
					toStore.push(name + "[" + val + "]");
					if(varNameTypeMap.containsKey(name + "[" + val + "]"))
					{
						varNameTypeMap.put(name + "[" + val + "]", name.substring(0, name.indexOf("[")));
					}
					typesStack.push(varNameTypeMap.get(name + "[" + val + "]"));
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.LABEL)
			{
				lastLabel = currentLabel;
				LabelNode labelNode = (LabelNode) ainsnNode;
				LabelNode toJumpNode = null;
				currentLabel = labelNode.getLabel();
				while(!toJump.isEmpty())
				{
					toJumpNode = toJump.peek();
					if(labelNode.getLabel().equals(toJumpNode.getLabel()))
					{
						tab4 = tab4.replaceFirst("    ", "");
						if(!hasJumpedTo.contains(currentLabel))
							buffer.append(tab4 + "}\n");
						else tab4 += "    ";
						toJump.pop();
						hasJumpedTo.add(currentLabel);
					}
					else break;
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.FRAME)
			{
				FrameNode frameNode = (FrameNode) ainsnNode;
				if(frameNode.type == F_APPEND)
				{
					tab4 = tab4.replaceFirst("    ", "");
					buffer.append(tab4 + "}\n" + tab4 + "else\n" + tab4 + "{\n");
					tab4 += "    ";
				}
				else if(frameNode.type == F_SAME && lastFrameType == F_APPEND)
				{
					if((!hasJumpedTo.contains(lastLabel) && !hasJumpedTo.contains(currentLabel)) || (hasReachedAGoto && !hasJumpedTo.contains(currentLabel)))
					{
						tab4 = tab4.replaceFirst("    ", "");
						buffer.append(tab4 + "}\n");
						hasReachedAGoto = false;
					}
					else if(hasJumpedTo.contains(currentLabel) && !hasJumpedTo.contains(lastLabel) && !hasReachedAGoto)
					{
						tab4 = tab4.replaceFirst("    ", "");
						buffer.append(tab4 + "}\n");
					}
				}
				else if(frameNode.type == F_SAME && lastFrameType == F_SAME)
				{
					if(!hasJumpedTo.contains(lastLabel) && !hasJumpedTo.contains(currentLabel))
					{
						tab4 = tab4.replaceFirst("    ", "");
						buffer.append(tab4 + "}\n");
						buffer.append(tab4 + "else\n" + tab4 + "{\n");
						tab4 += "    ";
					}
				}
				lastFrameType = frameNode.type;
			}
			else if(ainsnNode.getType() == AbstractInsnNode.JUMP_INSN)
			{
				JumpInsnNode jumpNode = (JumpInsnNode) ainsnNode;
				if(jumpNode.getOpcode() == IFEQ)
				{
					String var = toStore.pop();
					buffer.append(tab4 + "if(" + var + ")\n" + tab4 + "{\n");
					toJump.push(jumpNode.label);
					tab4 += "    ";
				}
				else if(jumpNode.getOpcode() == GOTO)
				{
					toJump.push(jumpNode.label);
					tab4 = tab4.replaceFirst("    ", "");
					if(lastFrameType == F_APPEND) buffer.append(tab4 + "}\n");
					hasReachedAGoto = true;
					tab4 += "    ";
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.LDC_INSN)
			{
				LdcInsnNode ldc = (LdcInsnNode) ainsnNode;
				toStore.push("" + ldc.cst);
			}
			else if(ainsnNode.getType() == AbstractInsnNode.VAR_INSN)
			{
				VarInsnNode varNode = (VarInsnNode) ainsnNode;
				int operand = varNode.var;
				if(ainsnNode.getOpcode() == ISTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL("int") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
					}
				}
				else if(ainsnNode.getOpcode() == DSTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL("double") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
					}
				}
				else if(ainsnNode.getOpcode() == LSTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL("long") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
					}
				}
				else if(ainsnNode.getOpcode() == FSTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL("float") + tab + varNameMap.get(operand) + " = " + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
						initialized.add(varNameMap.get(operand));
					}
					else
					{
						buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
					}
				}
				else if(ainsnNode.getOpcode() == ASTORE)
				{
					if(!initialized.contains(varNameMap.get(operand)))
					{
						buffer.append(tab4 + translateToJLSL(varTypeMap.get(operand)) + " " + varNameMap.get(operand) + " = " + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
						initialized.add(varNameMap.get(operand));
					}
					else buffer.append(tab4 + varNameMap.get(operand) + " = " + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
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
				FieldInsnNode fieldNode = (FieldInsnNode) ainsnNode;
				if(fieldNode.getOpcode() == PUTFIELD)
				{
					String val = toStore.pop();
					String owner = toStore.pop();
					PutFieldFragment putFieldFrag = new PutFieldFragment();
					putFieldFrag.value = val;
					putFieldFrag.fieldName = fieldNode.name;
					putFieldFrag.owner = owner;
					out.add(putFieldFrag);
				}
				else if(fieldNode.getOpcode() == GETFIELD)
				{
					String ownership = toStore.pop();
					if(ownership.equals("this"))
						ownership = "";
					else ownership += ".";
					toStore.push(ownership + fieldNode.name);
					typesStack.push(typesFromDesc(fieldNode.desc)[0]);
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.INT_INSN)
			{
				IntInsnNode intNode = (IntInsnNode) ainsnNode;
				int operand = intNode.operand;
				if(intNode.getOpcode() == BIPUSH)
				{
					toStore.push("" + operand);
				}
				else if(intNode.getOpcode() == NEWARRAY)
				{
					String type = translateToJLSL(Printer.TYPES[operand]);
					String s = type + toStore.pop();
					toStore.push(s);
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.TYPE_INSN)
			{
				TypeInsnNode typeNode = (TypeInsnNode) ainsnNode;
				String operand = typeNode.desc;
				if(typeNode.getOpcode() == ANEWARRAY)
				{
					String s = translateToJLSL(operand.replace("/", ".")) + "[" + toStore.pop() + "]";
					toStore.push(s);
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.MULTIANEWARRAY_INSN)
			{
				MultiANewArrayInsnNode multiArrayNode = (MultiANewArrayInsnNode) ainsnNode;
				String operand = multiArrayNode.desc;
				String desc = translateToJLSL(translateToJLSL(operand).replace("[]", ""));
				String s = desc;
				if(desc.length() == 1) s = typesFromDesc(desc)[0];
				ArrayList<String> list = new ArrayList<String>();
				for(int dim = 0; dim < multiArrayNode.dims; dim++)
				{
					list.add(toStore.pop());
				}
				for(int dim = 0; dim < multiArrayNode.dims; dim++)
				{
					s += "[" + list.get(list.size() - dim - 1) + "]";
				}
				toStore.push(s);
			}
			else if(ainsnNode.getType() == AbstractInsnNode.LINE)
			{
				LineNumberNode lineNode = (LineNumberNode) ainsnNode;
				LineNumberFragment lineNumberFragment = new LineNumberFragment();
				lineNumberFragment.line = lineNode.line;
//				if(toStore.size() > 0) if(toStore.peek().contains("(") && toStore.peek().contains(")"))
//				{
//					buffer.append(tab4 + toStore.pop() + ";" + getEndOfLine(currentLine) + "\n");
//				} TODO
				out.add(lineNumberFragment);
				currentLine = lineNode.line;
			}
			else if(ainsnNode.getType() == AbstractInsnNode.METHOD_INSN)
			{
				MethodInsnNode methodNode = (MethodInsnNode) ainsnNode;
				if(methodNode.getOpcode() == INVOKESPECIAL)
				{
					String desc = methodNode.desc;
					String margs = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));
					String[] margsArray = typesFromDesc(margs);
					ArrayList<String> argsList = new ArrayList<String>();
					for(int i = 0; i < margsArray.length; i++)
					{
						argsList.add(toStore.pop());
					}
					margs = null;
					for(String s : argsList)
					{
						if(margs == null)
							margs = s;
						else margs = s + ", " + margs;
					}
					String n = methodNode.name;
					if(margs == null) margs = "";
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
					for(int i = 0; i < margsArray.length; i++)
					{
						argsList.add(toStore.pop());
					}
					margs = null;
					for(String s : argsList)
					{
						if(margs == null)
							margs = s;
						else margs = s + ", " + margs;
					}
					String n = methodNode.name;
					AnnotationNode substituteAnnotation = getAnnotNode(methodNode.owner, n, methodNode.desc, Substitute.class.getCanonicalName());
					boolean ownerBefore = false;
					boolean parenthesis = true;
					if(substituteAnnotation != null)
					{
						List<Object> values = substituteAnnotation.values;
						for(int i = 0; i < values.size(); i += 2)
						{
							String name = (String) values.get(i);
							if(name.equals("value"))
							{
								n = (String) values.get(i + 1);
							}
							else if(name.equals("ownerBefore"))
							{
								ownerBefore = (Boolean) values.get(i + 1);
							}
							else if(name.equals("usesParenthesis"))
							{
								parenthesis = (Boolean) values.get(i + 1);
							}
						}
					}
					if(methodNode.name.equals("<init>"))
					{
						n = "";
					}
					if(margs == null)
						margs = "";
					else if(parenthesis) margs = ", " + margs;
					if(!ownerBefore)
					{
						toStore.push(n + (parenthesis ? "(" : "") + toStore.pop() + margs + (parenthesis ? ")" : ""));
					}
					else
					{
						toStore.push(toStore.pop() + n + (parenthesis ? "(" : "") + margs + (parenthesis ? ")" : ""));
					}
				}
			}
		}
		// System.err.println(toJump.size());
		// while(toJump.size() > 0)
		// {
		// tab4 = tab4.replaceFirst("    ", "");
		// buffer.append(tab4+"}\n");
		// toJump.pop();
		// }
	}

	private static String getEndOfLine(int currentLine)
	{
		String s = "";
		// if(currentLine % 5 == 0)
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

	private static String[] typesFromDesc(String desc)
	{
		return typesFromDesc(desc, 0);
	}

	private static boolean isSupported(String desc)
	{
		String[] types = typesFromDesc(desc);
		for(String type : types)
		{
			if(type.endsWith("[]")) type = type.replace("[]", "");
			type = translateToJLSL(type);
			if(!type.equals("int") && !type.equals("float") && !type.equals("double") && !type.equals("boolean") && !type.equals("vec2") && !type.equals("vec3") && !type.equals("vec4") && !type.equals("mat3") && !type.equals("mat2") && !type.equals("mat4")){ return false; }
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private static AnnotationNode getAnnotNode(String methodClass, String methodName, String methodDesc, String annotationClass)
	{
		try
		{
			ClassReader reader = new ClassReader(BytecodeDecoder.class.getResourceAsStream("/" + methodClass.replace(".", "/") + ".class"));
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
							if(annot.desc.replace("$", "/").equals("L" + annotationClass.replace(".", "/") + ";")){ return annot; }
						}
					else return null;
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
