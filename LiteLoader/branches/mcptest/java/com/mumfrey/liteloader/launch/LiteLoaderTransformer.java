package com.mumfrey.liteloader.launch;

import java.util.Iterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.mumfrey.liteloader.core.runtime.Obf;

import net.minecraft.launchwrapper.IClassTransformer;

public class LiteLoaderTransformer implements IClassTransformer
{
	private static final String LITELOADER_TWEAKER_CLASS = LiteLoaderTweaker.class.getName().replace('.', '/');
	
	private static final String METHOD_PRE_BEGIN_GAME = "preBeginGame";
	private static final String METHOD_INIT = "init";
	private static final String METHOD_POSTINIT = "postInit";
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (Obf.MinecraftMain.name.equals(name))
		{
			return this.transformMain(basicClass);
		}
		
		if ((Obf.Minecraft.name.equals(name) || Obf.Minecraft.obf.equals(name)))
		{
			return this.transformMinecraft(basicClass);
		}
		
		return basicClass;
	}

	private byte[] transformMain(byte[] basicClass)
	{
		ClassNode classNode = this.readClass(basicClass);

		for (MethodNode method : classNode.methods)
		{
			if ("main".equals(method.name))
			{
				method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, LiteLoaderTransformer.LITELOADER_TWEAKER_CLASS, LiteLoaderTransformer.METHOD_PRE_BEGIN_GAME, "()V"));
			}
		}
		
		return this.writeClass(classNode);
	}

	private byte[] transformMinecraft(byte[] basicClass)
	{
		ClassNode classNode = this.readClass(basicClass);

		for (MethodNode method : classNode.methods)
		{
			if (Obf.startGame.obf.equals(method.name) || Obf.startGame.srg.equals(method.name) || Obf.startGame.name.equals(method.name))
			{
				this.transformStartGame(method);
			}
		}
		
		return this.writeClass(classNode);
	}

	private void transformStartGame(MethodNode method)
	{
		AbstractInsnNode lastInsn = null;
		
		Iterator<AbstractInsnNode> iter = method.instructions.iterator();
		while (iter.hasNext())
		{
			AbstractInsnNode insn = iter.next();
			if (insn instanceof TypeInsnNode && insn.getOpcode() == Opcodes.NEW && lastInsn != null)
			{
				TypeInsnNode typeNode = (TypeInsnNode)insn;
				if (Obf.EntityRenderer.obf.equals(typeNode.desc) || Obf.EntityRenderer.ref.equals(typeNode.desc))
				{
					InsnList insns = new InsnList();
					insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LiteLoaderTransformer.LITELOADER_TWEAKER_CLASS, LiteLoaderTransformer.METHOD_INIT, "()V"));
					insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LiteLoaderTransformer.LITELOADER_TWEAKER_CLASS, LiteLoaderTransformer.METHOD_POSTINIT, "()V"));
					method.instructions.insertBefore(lastInsn, insns);
					return;
				}
			}
			
			lastInsn = insn;
		}
	}

	/**
	 * @param basicClass
	 * @return
	 */
	private ClassNode readClass(byte[] basicClass)
	{
		ClassReader classReader = new ClassReader(basicClass);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
		return classNode;
	}

	/**
	 * @param classNode
	 * @return
	 */
	private byte[] writeClass(ClassNode classNode)
	{
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}
}
