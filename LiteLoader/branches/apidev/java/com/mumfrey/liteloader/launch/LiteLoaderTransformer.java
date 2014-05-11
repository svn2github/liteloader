package com.mumfrey.liteloader.launch;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.mumfrey.liteloader.core.runtime.Obf;

public class LiteLoaderTransformer implements IClassTransformer
{
	private static final String LITELOADER_TWEAKER_CLASS = LiteLoaderTweaker.class.getName().replace('.', '/');
	
	private static final String METHOD_PRE_BEGIN_GAME = "preBeginGame";
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (Obf.MinecraftMain.name.equals(transformedName))
		{
			return this.transformMain(basicClass);
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
