package com.mumfrey.liteloader.launch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;

public class LiteLoaderTransformer implements IClassTransformer
{
	private static final String LITELOADER_TWEAKER_CLASS = LiteLoaderTweaker.class.getName().replace('.', '/');
	
	private static final String METHOD_PRE_BEGIN_GAME = "preBeginGame";

	private static final String METHOD_INIT = "init";
	
	private static final String METHOD_POSTINIT = "postInit";

	private static final String classMappingRenderLightningBolt = "net.minecraft.client.renderer.entity.RenderLightningBolt";
	
	private static final String classMappingMain = "net.minecraft.client.main.Main";
	
	// TODO Obfuscation 1.7.2
	private static final String classMappingRenderLightningBoltObf = "bny";
	
	private static boolean postInit = false;
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (classMappingMain.equals(name))
		{
			return this.transformMain(basicClass);
		}
		
		if ((classMappingRenderLightningBolt.equals(name) || classMappingRenderLightningBoltObf.equals(name)) && !LiteLoaderTransformer.postInit)
		{
			return this.transformRenderLightningBolt(basicClass);
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

	private byte[] transformRenderLightningBolt(byte[] basicClass)
	{
		ClassNode classNode = this.readClass(basicClass);

		for (MethodNode method : classNode.methods)
		{
			if ("<init>".equals(method.name))
			{
				method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, LiteLoaderTransformer.LITELOADER_TWEAKER_CLASS, LiteLoaderTransformer.METHOD_POSTINIT, "()V"));
				method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, LiteLoaderTransformer.LITELOADER_TWEAKER_CLASS, LiteLoaderTransformer.METHOD_INIT, "()V"));
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
