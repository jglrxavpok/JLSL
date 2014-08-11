package org.jglrxavpok.jlsl;

import static org.objectweb.asm.Opcodes.*;

import java.io.*;
import java.util.*;

import org.jglrxavpok.jlsl.fragments.*;
import org.jglrxavpok.jlsl.fragments.MethodCallFragment.InvokeTypes;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.*;

public class BytecodeDecoder extends CodeDecoder
{

	public static boolean DEBUG = true;

	public BytecodeDecoder()
	{
		
	}
	
	public BytecodeDecoder addInstructionsFromInterfaces(boolean add)
	{
		this.instructionsFromInterfaces = add;
		return this;
	}
	
	private boolean instructionsFromInterfaces = false;
	
	@SuppressWarnings("unchecked")
	@Override
	public void handleClass(Object data, List<CodeFragment> out)
	{
		try
		{
			if(data == null)
				return;
			ClassReader reader;
			if(data instanceof byte[])
			{
				reader = new ClassReader((byte[]) data);
			}
			else if(data instanceof InputStream)
			{
				reader = new ClassReader((InputStream) data);
			}
			else if(data instanceof String)
			{
				handleClass(Class.forName((String) data), out);
				return;
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
			if(classNode.sourceFile != null)
				classFragment.sourceFile = classNode.sourceFile;
			classFragment.classVersion = classNode.version;
			
			List<String> interfaces = classNode.interfaces;
			classFragment.interfaces = interfaces.toArray(new String[0]);

			if(instructionsFromInterfaces)
    			for(String interfaceInst : interfaces)
    			{
    				ArrayList<CodeFragment> fragments = new ArrayList<CodeFragment>();
    				handleClass(interfaceInst.replace("/", "."), fragments);
    				
    				out.addAll(fragments);
    			}
			out.add(classFragment);
			List<MethodNode> methodNodes = classNode.methods;
			List<FieldNode> fieldNodes = classNode.fields;
			
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
				fieldFragment.initialValue = field.value;
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
			
			Collections.sort(methodNodes, new Comparator<MethodNode>()
			{
				@Override
				public int compare(MethodNode arg0, MethodNode arg1)
				{
					if(arg0.name.equals("main")) return 1;
					if(arg1.name.equals("main")) return -1;
					return 0;
				}
			}); // TODO: Better the method sorting

			for(MethodNode node : methodNodes)
			{
				List<LocalVariableNode> localVariables = node.localVariables;
				StartOfMethodFragment startOfMethodFragment = new StartOfMethodFragment();
				startOfMethodFragment.access = new AccessPolicy(node.access);
				startOfMethodFragment.name = node.name;
				startOfMethodFragment.owner = classNode.name.replace("/", ".");
				startOfMethodFragment.returnType = typesFromDesc(node.desc.substring(node.desc.indexOf(")")+1))[0];
				ArrayList<String> localNames = new ArrayList<String>();
				for(LocalVariableNode var : localVariables)
				{
					startOfMethodFragment.varNameMap.put(var.index, var.name);
					startOfMethodFragment.varTypeMap.put(var.index, typesFromDesc(var.desc)[0]);
					startOfMethodFragment.varName2TypeMap.put(var.name, typesFromDesc(var.desc)[0]);
					if(var.index == 0 && !startOfMethodFragment.access.isStatic())
					{
						;
					}
					else
					{
						localNames.add(var.name);
					}
				}
				String[] argsTypes = typesFromDesc(node.desc.substring(node.desc.indexOf('(') + 1, node.desc.indexOf(')')));
				int argIndex = 0;
				for(String argType : argsTypes)
				{
					startOfMethodFragment.argumentsTypes.add(argType);
					String name = localNames.isEmpty() ? "var"+argIndex : localNames.get(argIndex);
					startOfMethodFragment.argumentsNames.add(name);
					argIndex++;
				}
				List<AnnotationNode> annots = node.visibleAnnotations;
				if(node.visibleAnnotations != null)
					for(AnnotationNode annotNode : annots)
					{
						startOfMethodFragment.addChild(createFromNode(annotNode));
					}
				out.add(startOfMethodFragment);
				handleMethodNode(node, startOfMethodFragment.varTypeMap, startOfMethodFragment.varNameMap, out);
				EndOfMethodFragment endOfMethodFragment = new EndOfMethodFragment();
				endOfMethodFragment.access = startOfMethodFragment.access;
				endOfMethodFragment.name = startOfMethodFragment.name;
				endOfMethodFragment.owner = startOfMethodFragment.owner;
				endOfMethodFragment.argumentsNames = startOfMethodFragment.argumentsNames;
				endOfMethodFragment.argumentsTypes = startOfMethodFragment.argumentsTypes;
				endOfMethodFragment.returnType = startOfMethodFragment.returnType;
				endOfMethodFragment.varNameMap = startOfMethodFragment.varNameMap;
				endOfMethodFragment.varTypeMap = startOfMethodFragment.varTypeMap;
				endOfMethodFragment.varName2TypeMap = startOfMethodFragment.varName2TypeMap;
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

	@SuppressWarnings("unchecked")
	private static AnnotationFragment createFromNode(AnnotationNode annotNode)
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

	private static void handleMethodNode(MethodNode node, HashMap<Integer, String> varTypeMap, HashMap<Integer, String> varNameMap, List<CodeFragment> out)
	{
		int lastFrameType = 0;
		int frames = 0;
		int framesToSkip = 0;
		Stack<LabelNode> toJump = new Stack<LabelNode>();
		Stack<Label> gotos = new Stack<Label>();
		Stack<Label> ifs = new Stack<Label>();
		InsnList instructions = node.instructions;
		Label currentLabel = null;
		for(int index = 0; index < instructions.size(); index++)
		{
			AbstractInsnNode ainsnNode = instructions.get(index);
			if(ainsnNode.getType() == AbstractInsnNode.INSN)
			{
				InsnNode insnNode = (InsnNode) ainsnNode;
				if(insnNode.getOpcode() == ICONST_0)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = 0;
					out.add(loadConstantFragment);
				}
				else if(insnNode.getOpcode() == ICONST_1)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = 1;
					out.add(loadConstantFragment);
				}
				else if(insnNode.getOpcode() == ICONST_2)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = 2;
					out.add(loadConstantFragment);
				}
				else if(insnNode.getOpcode() == ICONST_3)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = 3;
					out.add(loadConstantFragment);
				}
				else if(insnNode.getOpcode() == ICONST_4)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = 4;
					out.add(loadConstantFragment);
				}
				else if(insnNode.getOpcode() == ICONST_5)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = 5;
					out.add(loadConstantFragment);
				}

				else if(insnNode.getOpcode() == DCONST_0)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = 0.0;
					out.add(loadConstantFragment);
				}
				else if(insnNode.getOpcode() == DCONST_1)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = 1.0;
					out.add(loadConstantFragment);
				}

				else if(insnNode.getOpcode() == FCONST_0)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = 0.f;
					out.add(loadConstantFragment);
				}
				else if(insnNode.getOpcode() == FCONST_1)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = 1.f;
					out.add(loadConstantFragment);
				}
				else if(insnNode.getOpcode() == FCONST_2)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = 2.f;
					out.add(loadConstantFragment);
				}
				else if(insnNode.getOpcode() == ACONST_NULL)
				{
					LoadConstantFragment loadConstantFragment = new LoadConstantFragment();
					loadConstantFragment.value = null;
					out.add(loadConstantFragment);
				}

				else if(ainsnNode.getOpcode() == LRETURN || ainsnNode.getOpcode() == DRETURN || ainsnNode.getOpcode() == FRETURN || ainsnNode.getOpcode() == IRETURN || ainsnNode.getOpcode() == ARETURN)
				{
					ReturnValueFragment returnFrag = new ReturnValueFragment();
					out.add(returnFrag);
				}
				else if(ainsnNode.getOpcode() == LADD || ainsnNode.getOpcode() == DADD || ainsnNode.getOpcode() == FADD || ainsnNode.getOpcode() == IADD)
				{
					out.add(new AddFragment());
				}
				else if(ainsnNode.getOpcode() == LSUB || ainsnNode.getOpcode() == DSUB || ainsnNode.getOpcode() == FSUB || ainsnNode.getOpcode() == ISUB)
				{
					out.add(new SubFragment());
				}
				else if(ainsnNode.getOpcode() == LMUL || ainsnNode.getOpcode() == DMUL || ainsnNode.getOpcode() == FMUL || ainsnNode.getOpcode() == IMUL)
				{
					out.add(new MulFragment());
				}
				else if(ainsnNode.getOpcode() == LDIV || ainsnNode.getOpcode() == DDIV || ainsnNode.getOpcode() == FDIV || ainsnNode.getOpcode() == IDIV)
				{
					out.add(new DivFragment());
				}
				else if(ainsnNode.getOpcode() == DREM || ainsnNode.getOpcode() == IREM || ainsnNode.getOpcode() == FREM || ainsnNode.getOpcode() == LREM)
				{
					out.add(new ModFragment());
				}
				
				else if(ainsnNode.getOpcode() == D2I)
				{
					CastFragment cast = new CastFragment();
					cast.from = "double";
					cast.to = "int";
					out.add(cast);
				}
				else if(ainsnNode.getOpcode() == I2D)
				{
					CastFragment cast = new CastFragment();
					cast.from = "int";
					cast.to = "double";
					out.add(cast);
				}
				else if(ainsnNode.getOpcode() == I2F)
				{
					CastFragment cast = new CastFragment();
					cast.from = "int";
					cast.to = "float";
					out.add(cast);
				}
				else if(ainsnNode.getOpcode() == I2L)
				{
					CastFragment cast = new CastFragment();
					cast.from = "int";
					cast.to = "long";
					out.add(cast);
				}
				else if(ainsnNode.getOpcode() == I2C)
				{
					CastFragment cast = new CastFragment();
					cast.from = "int";
					cast.to = "char";
					out.add(cast);
				}
				else if(ainsnNode.getOpcode() == F2I)
				{
					CastFragment cast = new CastFragment();
					cast.from = "float";
					cast.to = "int";
					out.add(cast);
				}
				else if(ainsnNode.getOpcode() == F2D)
				{
					CastFragment cast = new CastFragment();
					cast.from = "float";
					cast.to = "double";
					out.add(cast);
				}
				else if(ainsnNode.getOpcode() == F2L)
				{
					CastFragment cast = new CastFragment();
					cast.from = "float";
					cast.to = "long";
					out.add(cast);
				}
				else if(ainsnNode.getOpcode() == L2D)
				{
					CastFragment cast = new CastFragment();
					cast.from = "long";
					cast.to = "double";
					out.add(cast);
				}
				else if(ainsnNode.getOpcode() == L2I)
				{
					CastFragment cast = new CastFragment();
					cast.from = "long";
					cast.to = "int";
					out.add(cast);
				}
				else if(ainsnNode.getOpcode() == L2F)
				{
					CastFragment cast = new CastFragment();
					cast.from = "long";
					cast.to = "float";
					out.add(cast);
				}
				
				else if(ainsnNode.getOpcode() == ISHR)
				{
					RightShiftFragment shrFragment = new RightShiftFragment();
					shrFragment.signed = true;
					shrFragment.type = "int";
					out.add(shrFragment);
				}
				else if(ainsnNode.getOpcode() == IUSHR)
				{
					RightShiftFragment shrFragment = new RightShiftFragment();
					shrFragment.signed = false;
					shrFragment.type = "int";
					out.add(shrFragment);
				}
				else if(ainsnNode.getOpcode() == LSHR)
				{
					RightShiftFragment shrFragment = new RightShiftFragment();
					shrFragment.signed = true;
					shrFragment.type = "long";
					out.add(shrFragment);
				}
				else if(ainsnNode.getOpcode() == LUSHR)
				{
					RightShiftFragment shrFragment = new RightShiftFragment();
					shrFragment.signed = false;
					shrFragment.type = "long";
					out.add(shrFragment);
				}

				else if(ainsnNode.getOpcode() == ISHL)
				{
					LeftShiftFragment shlFragment = new LeftShiftFragment();
					shlFragment.signed = true;
					shlFragment.type = "int";
					out.add(shlFragment);
				}
				else if(ainsnNode.getOpcode() == LSHL)
				{
					LeftShiftFragment shlFragment = new LeftShiftFragment();
					shlFragment.type = "long";
					shlFragment.signed = true;
					out.add(shlFragment);
				}
				
				else if(ainsnNode.getOpcode() == IAND)
				{
					AndFragment andFragment = new AndFragment();
					andFragment.type = "int";
					out.add(andFragment);
				}
				else if(ainsnNode.getOpcode() == LAND)
				{
					AndFragment andFragment = new AndFragment();
					andFragment.type = "long";
					out.add(andFragment);
				}
				
				else if(ainsnNode.getOpcode() == IOR)
				{
					OrFragment orFragment = new OrFragment();
					orFragment.type = "int";
					out.add(orFragment);
				}
				else if(ainsnNode.getOpcode() == LOR)
				{
					OrFragment orFragment = new OrFragment();
					orFragment.type = "long";
					out.add(orFragment);
				}
				
				else if(ainsnNode.getOpcode() == IXOR)
				{
					XorFragment xorFragment = new XorFragment();
					xorFragment.type = "int";
					out.add(xorFragment);
				}
				else if(ainsnNode.getOpcode() == LXOR)
				{
					XorFragment xorFragment = new XorFragment();
					xorFragment.type = "long";
					out.add(xorFragment);
				}
				
				else if(ainsnNode.getOpcode() == POP)
				{
					PopFragment popFrag = new PopFragment();
					out.add(popFrag);
				}
				
				else if(ainsnNode.getOpcode() == RETURN)
				{
					ReturnFragment returnFrag = new ReturnFragment();
					out.add(returnFrag);
				}
				
				else if(ainsnNode.getOpcode() == DUP)
				{
					DuplicateFragment duplicate = new DuplicateFragment();
					duplicate.wait = 1;
					out.add(duplicate);
				}
				else if(ainsnNode.getOpcode() == DUP2_X1)
				{
					DuplicateFragment duplicate = new DuplicateFragment();
					duplicate.wait = 1;
					out.add(duplicate);
				}
				
				else if(ainsnNode.getOpcode() == DCMPG || ainsnNode.getOpcode() == FCMPG)
				{
					CompareFragment compareFrag = new CompareFragment();
					compareFrag.inferior = true;
					out.add(compareFrag);
				}
				else if(ainsnNode.getOpcode() == DCMPL || ainsnNode.getOpcode() == FCMPL)
				{
					CompareFragment compareFrag = new CompareFragment();
					compareFrag.inferior = false;
					out.add(compareFrag);
				}
				
				else if(ainsnNode.getOpcode() == AASTORE || ainsnNode.getOpcode() == IASTORE || ainsnNode.getOpcode() == BASTORE || ainsnNode.getOpcode() == LASTORE || ainsnNode.getOpcode() == SASTORE || ainsnNode.getOpcode() == FASTORE || ainsnNode.getOpcode() == DASTORE || ainsnNode.getOpcode() == CASTORE)
				{
					ArrayStoreFragment storeFrag = new ArrayStoreFragment();
					out.add(storeFrag);
				}
				else if(ainsnNode.getOpcode() == AALOAD)
				{
					ArrayOfArrayLoadFragment loadFrag = new ArrayOfArrayLoadFragment();
					out.add(loadFrag);
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.LABEL)
			{
				LabelNode labelNode = (LabelNode) ainsnNode;
				currentLabel = labelNode.getLabel();
				while(!toJump.isEmpty())
				{
					if(labelNode.getLabel().equals(toJump.peek().getLabel()))
					{
						while(!toJump.isEmpty() && toJump.pop().getLabel().equals(labelNode.getLabel()))
						{
							if(!gotos.isEmpty())
							{
								while(gotos.contains(currentLabel))
								{
									if(frames > 0)
									{
                						EndOfBlockFragment endOfBlockFrag = new EndOfBlockFragment();
                						out.add(endOfBlockFrag);
                						frames--;
									}
            						gotos.remove(currentLabel);
								}
							}
						}
						break;
					}
					else
						break;
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.FRAME)
			{
				FrameNode frameNode = (FrameNode) ainsnNode;
				if(framesToSkip > 0)
					framesToSkip--;
				else
				{
					if(frames == 0)
					{
						;
					}
					else
					{
						boolean a = (!ifs.isEmpty() && ifs.contains(currentLabel));
						boolean b = (gotos.isEmpty() || !gotos.contains(currentLabel));
						int nbr = 0;
						if(a || b)
						{
							while(ifs.contains(currentLabel))
							{
    							nbr++;
    							ifs.remove(currentLabel);
							}
						}

						for(int j = 0;j<nbr;j++)
						{
							EndOfBlockFragment end = new EndOfBlockFragment();
    						out.add(end);
    						frames--;
						}
					}
				}
				lastFrameType = frameNode.type;
			}
			else if(ainsnNode.getType() == AbstractInsnNode.JUMP_INSN)
			{
				JumpInsnNode jumpNode = (JumpInsnNode) ainsnNode;
				if(jumpNode.getOpcode() == IFEQ)
				{
					if(instructions.get(index-1).getOpcode() == ILOAD 
						&& instructions.get(index+1).getOpcode() == ILOAD
						&& instructions.get(index+2).getOpcode() == IFEQ
						&& instructions.get(index+3).getOpcode() == ICONST_1
						&& instructions.get(index+4).getOpcode() == GOTO
						&& instructions.get(index+5).getType() == AbstractInsnNode.LABEL
						&& instructions.get(index+6).getType() == AbstractInsnNode.FRAME
						&& instructions.get(index+7).getOpcode() == ICONST_0
						&& instructions.get(index+8).getType() == AbstractInsnNode.LABEL
						&& instructions.get(index+9).getType() == AbstractInsnNode.FRAME
						&& instructions.get(index+10).getOpcode() == ISTORE
						)
					{
						int operand = ((VarInsnNode)instructions.get(index+1)).var;
						LoadVariableFragment loadFrag = new LoadVariableFragment();
						loadFrag.variableName = varNameMap.get(operand);
						loadFrag.variableIndex = operand;
						out.add(loadFrag);
						
						AndFragment andFrag = new AndFragment();
						andFrag.isDouble = true;
						out.add(andFrag);
						
						int operand1 = ((VarInsnNode)instructions.get(index+10)).var;
						StoreVariableFragment storeFrag = new StoreVariableFragment();
						storeFrag.variableName = varNameMap.get(operand1);
						storeFrag.variableIndex = operand1;
						storeFrag.variableType = "int";
						out.add(storeFrag);
						index+=10;
					}
					else
					{
    					IfStatementFragment ifFrag = new IfStatementFragment();
    					frames++;
    					ifs.push(jumpNode.label.getLabel());
    					ifFrag.toJump = jumpNode.label.getLabel().toString();
    					out.add(ifFrag);
    					toJump.push(jumpNode.label);
					}
				}
				else if(jumpNode.getOpcode() == IF_ICMPEQ
						&& instructions.get(index+1).getOpcode() == ICONST_1
						&& (instructions.get(index+2).getOpcode() == IRETURN || instructions.get(index+2).getOpcode() == ISTORE)
						&& instructions.get(index+3).getType() == AbstractInsnNode.LABEL
						&& instructions.get(index+4).getType() == AbstractInsnNode.FRAME
						&& instructions.get(index+5).getOpcode() == ICONST_0
						&& (instructions.get(index+6).getOpcode() == IRETURN || instructions.get(index+2).getOpcode() == ISTORE))
				{
					NotEqualCheckFragment notEqualFrag = new NotEqualCheckFragment();
					out.add(notEqualFrag);
					index+=5;
				}
				else if(jumpNode.getOpcode() == IF_ICMPNE
						&& instructions.get(index+1).getOpcode() == ICONST_1
						&& (instructions.get(index+2).getOpcode() == IRETURN || instructions.get(index+2).getOpcode() == ISTORE)
						&& instructions.get(index+3).getType() == AbstractInsnNode.LABEL
						&& instructions.get(index+4).getType() == AbstractInsnNode.FRAME
						&& instructions.get(index+5).getOpcode() == ICONST_0
						&& (instructions.get(index+6).getOpcode() == IRETURN || instructions.get(index+2).getOpcode() == ISTORE)
						
						)
				{
					EqualCheckFragment equalFrag = new EqualCheckFragment();
					out.add(equalFrag);
					index+=5;
				}
				else if(jumpNode.getOpcode() == IFNE)
				{
					if(instructions.get(index-1).getOpcode() == ILOAD 
    					&& instructions.get(index+1).getOpcode() == ILOAD
    					&& instructions.get(index+2).getOpcode() == IFNE
    					&& instructions.get(index+3).getOpcode() == ICONST_0
    					&& instructions.get(index+4).getOpcode() == GOTO
    					&& instructions.get(index+5).getType() == AbstractInsnNode.LABEL
    					&& instructions.get(index+6).getType() == AbstractInsnNode.FRAME
    					&& instructions.get(index+7).getOpcode() == ICONST_1
    					&& instructions.get(index+8).getType() == AbstractInsnNode.LABEL
    					&& instructions.get(index+9).getType() == AbstractInsnNode.FRAME
    					&& instructions.get(index+10).getOpcode() == ISTORE
					)
					{
						int operand = ((VarInsnNode)instructions.get(index+1)).var;
						LoadVariableFragment loadFrag = new LoadVariableFragment();
						loadFrag.variableName = varNameMap.get(operand);
						loadFrag.variableIndex = operand;
						out.add(loadFrag);
						
						OrFragment orFrag = new OrFragment();
						orFrag.isDouble = true;
						out.add(orFrag);
						
						int operand1 = ((VarInsnNode)instructions.get(index+10)).var;
						StoreVariableFragment storeFrag = new StoreVariableFragment();
						storeFrag.variableName = varNameMap.get(operand1);
						storeFrag.variableIndex = operand1;
						storeFrag.variableType = "int";
						out.add(storeFrag);
						index+=10;
					}
					else
					{
    					IfNotStatementFragment ifFrag = new IfNotStatementFragment();
    					frames++;
    					ifs.push(jumpNode.label.getLabel());
    					ifFrag.toJump = jumpNode.label.getLabel().toString();
    					out.add(ifFrag);
    					toJump.push(jumpNode.label);
					}
				}
				else if(jumpNode.getOpcode() == IFGE)
				{
					IfStatementFragment ifFrag = new IfStatementFragment();
					frames++;
					ifs.push(jumpNode.label.getLabel());
					ifFrag.toJump = jumpNode.label.getLabel().toString();
					out.add(ifFrag);
					toJump.push(jumpNode.label);
				}
				else if(jumpNode.getOpcode() == IFLE)
				{
					IfStatementFragment ifFrag = new IfStatementFragment();
					frames++;
					ifs.push(jumpNode.label.getLabel());
					ifFrag.toJump = jumpNode.label.getLabel().toString();
					out.add(ifFrag);
					toJump.push(jumpNode.label);
				}
				else if(jumpNode.getOpcode() == GOTO)
				{
					toJump.push(jumpNode.label);
					gotos.push(jumpNode.label.getLabel());
					if(instructions.get(index-1) instanceof LineNumberNode && lastFrameType == F_SAME)
					{
						EndOfBlockFragment end = new EndOfBlockFragment();
						frames--;
						out.add(end);
					}
					
					EndOfBlockFragment end = new EndOfBlockFragment();
					frames--;
					out.add(end);

					ElseStatementFragment elseFrag = new ElseStatementFragment();
					frames++;
					out.add(elseFrag);
					framesToSkip=1;
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.LDC_INSN)
			{
				LdcInsnNode ldc = (LdcInsnNode) ainsnNode;
				LdcFragment ldcFragment = new LdcFragment();
				ldcFragment.value = ldc.cst;
				out.add(ldcFragment);
			}
			else if(ainsnNode.getType() == AbstractInsnNode.VAR_INSN)
			{
				VarInsnNode varNode = (VarInsnNode) ainsnNode;
				int operand = varNode.var;
				if(ainsnNode.getOpcode() == ISTORE)
				{
					StoreVariableFragment storeFrag = new StoreVariableFragment();
					storeFrag.variableName = varNameMap.get(operand);
					storeFrag.variableIndex = operand;
					storeFrag.variableType = "int";
					out.add(storeFrag);
				}
				else if(ainsnNode.getOpcode() == DSTORE)
				{
					StoreVariableFragment storeFrag = new StoreVariableFragment();
					storeFrag.variableName = varNameMap.get(operand);
					storeFrag.variableIndex = operand;
					storeFrag.variableType = "double";
					out.add(storeFrag);
				}
				else if(ainsnNode.getOpcode() == LSTORE)
				{
					StoreVariableFragment storeFrag = new StoreVariableFragment();
					storeFrag.variableName = varNameMap.get(operand);
					storeFrag.variableIndex = operand;
					storeFrag.variableType = "long";
					out.add(storeFrag);
				}
				else if(ainsnNode.getOpcode() == FSTORE)
				{
					StoreVariableFragment storeFrag = new StoreVariableFragment();
					storeFrag.variableName = varNameMap.get(operand);
					storeFrag.variableIndex = operand;
					storeFrag.variableType = "float";
					out.add(storeFrag);
				}
				else if(ainsnNode.getOpcode() == ASTORE)
				{
					StoreVariableFragment storeFrag = new StoreVariableFragment();
					storeFrag.variableName = varNameMap.get(operand);
					storeFrag.variableIndex = operand;
					storeFrag.variableType = varTypeMap.get(operand);
					out.add(storeFrag);
				}
				else if(ainsnNode.getOpcode() == FLOAD || ainsnNode.getOpcode() == LLOAD || ainsnNode.getOpcode() == ILOAD || ainsnNode.getOpcode() == DLOAD
					|| ainsnNode.getOpcode() == ALOAD)
				{
					LoadVariableFragment loadFrag = new LoadVariableFragment();
					loadFrag.variableName = varNameMap.get(operand);
					loadFrag.variableIndex = operand;
					out.add(loadFrag);
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.FIELD_INSN)
			{
				FieldInsnNode fieldNode = (FieldInsnNode) ainsnNode;
				if(fieldNode.getOpcode() == PUTFIELD)
				{
					PutFieldFragment putFieldFrag = new PutFieldFragment();
					putFieldFrag.fieldType = typesFromDesc(fieldNode.desc)[0];
					putFieldFrag.fieldName = fieldNode.name;
					out.add(putFieldFrag);
				}
				else if(fieldNode.getOpcode() == GETFIELD)
				{
					GetFieldFragment getFieldFrag = new GetFieldFragment();
					getFieldFrag.fieldType = typesFromDesc(fieldNode.desc)[0];
					getFieldFrag.fieldName = fieldNode.name;
					out.add(getFieldFrag);
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.INT_INSN)
			{
				IntInsnNode intNode = (IntInsnNode) ainsnNode;
				int operand = intNode.operand;
				if(intNode.getOpcode() == BIPUSH)
				{
					IntPushFragment pushFrag = new IntPushFragment();
					pushFrag.value = operand;
					out.add(pushFrag);
				}
				else if(intNode.getOpcode() == SIPUSH)
				{
					IntPushFragment pushFrag = new IntPushFragment();
					pushFrag.value = operand;
					out.add(pushFrag);
				}
				else if(intNode.getOpcode() == NEWARRAY)
				{
					NewPrimitiveArrayFragment arrayFrag = new NewPrimitiveArrayFragment();
					arrayFrag.type = Printer.TYPES[operand];
					out.add(arrayFrag);
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.TYPE_INSN)
			{
				TypeInsnNode typeNode = (TypeInsnNode) ainsnNode;
				String operand = typeNode.desc;
				if(typeNode.getOpcode() == ANEWARRAY)
				{
					NewArrayFragment newArray = new NewArrayFragment();
					newArray.type = operand.replace("/", ".");
					out.add(newArray);
				}
				else if(ainsnNode.getOpcode() == CHECKCAST)
				{
					CastFragment cast = new CastFragment();
					cast.from = "java.lang.Object";
					cast.to = operand.replace("/", ".");
					out.add(cast);
				}
				else if(ainsnNode.getOpcode() == NEW)
				{
					NewInstanceFragment newFrag = new NewInstanceFragment();
					newFrag.type = operand.replace("/", ".");
					out.add(newFrag);
				}
			}
			else if(ainsnNode.getType() == AbstractInsnNode.MULTIANEWARRAY_INSN)
			{
				MultiANewArrayInsnNode multiArrayNode = (MultiANewArrayInsnNode) ainsnNode;
				NewMultiArrayFragment multiFrag = new NewMultiArrayFragment();
				multiFrag.type = typesFromDesc(multiArrayNode.desc)[0].replace("[]", "");
				multiFrag.dimensions = multiArrayNode.dims;
				out.add(multiFrag);
			}
			else if(ainsnNode.getType() == AbstractInsnNode.LINE)
			{
				LineNumberNode lineNode = (LineNumberNode) ainsnNode;
				LineNumberFragment lineNumberFragment = new LineNumberFragment();
				lineNumberFragment.line = lineNode.line;
				out.add(lineNumberFragment);
			}
			else if(ainsnNode.getType() == AbstractInsnNode.METHOD_INSN)
			{
				MethodInsnNode methodNode = (MethodInsnNode) ainsnNode;
				if(methodNode.getOpcode() == INVOKESTATIC)
				{
					String desc = methodNode.desc;
					String margs = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));
					String[] margsArray = typesFromDesc(margs);
					String n = methodNode.name;
					MethodCallFragment methodFragment = new MethodCallFragment();
					methodFragment.invokeType = InvokeTypes.STATIC;
					methodFragment.methodName = n;
					methodFragment.methodOwner = methodNode.owner.replace("/", ".");
					methodFragment.argumentsTypes = margsArray;
					methodFragment.returnType = typesFromDesc(desc.substring(desc.indexOf(")")+1))[0];
					out.add(methodFragment);
				}
				else if(methodNode.getOpcode() == INVOKESPECIAL)
				{
					String desc = methodNode.desc;
					String margs = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));
					String[] margsArray = typesFromDesc(margs);
					String n = methodNode.name;
					MethodCallFragment methodFragment = new MethodCallFragment();
					methodFragment.invokeType = InvokeTypes.SPECIAL;
					methodFragment.methodName = n;
					methodFragment.methodOwner = methodNode.owner.replace("/", ".");
					methodFragment.argumentsTypes = margsArray;
					methodFragment.returnType = typesFromDesc(desc.substring(desc.indexOf(")")+1))[0];
					out.add(methodFragment);
					addAnnotFragments(methodNode.owner, n, methodNode.desc, methodFragment);
				}
				else if(methodNode.getOpcode() == INVOKEVIRTUAL)
				{
					String desc = methodNode.desc;
					String margs = desc.substring(desc.indexOf('(') + 1, desc.indexOf(')'));
					String[] margsArray = typesFromDesc(margs);
					String n = methodNode.name;
					MethodCallFragment methodFragment = new MethodCallFragment();
					methodFragment.invokeType = InvokeTypes.VIRTUAL;
					methodFragment.methodName = n;
					methodFragment.methodOwner = methodNode.owner.replace("/", ".");
					methodFragment.argumentsTypes = margsArray;
					methodFragment.returnType = typesFromDesc(desc.substring(desc.indexOf(")")+1))[0];
					out.add(methodFragment);
					addAnnotFragments(methodNode.owner, n, methodNode.desc, methodFragment);
				}
			}
		}
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
				else if(c == 'S')
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

	@SuppressWarnings("unchecked")
	private static void addAnnotFragments(String methodClass, String methodName, String methodDesc, MethodCallFragment fragment)
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
							fragment.addChild(createFromNode(annot));
						}
					else return;
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
}
