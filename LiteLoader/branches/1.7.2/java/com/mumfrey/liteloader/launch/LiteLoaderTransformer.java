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
	private static final String classMappingRenderLightningBolt = "net.minecraft.client.renderer.entity.RenderLightningBolt";
	
	// TODO Obfuscation 1.7.2
	private static final String classMappingRenderLightningBoltObf = "bny";
	
	private static boolean postInit = false;
	
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if ((classMappingRenderLightningBolt.equals(name) || classMappingRenderLightningBoltObf.equals(name)) && !LiteLoaderTransformer.postInit)
		{
			return this.transformRenderLightningBolt(basicClass);
		}
		
		return basicClass;
	}

	private byte[] transformRenderLightningBolt(byte[] basicClass)
	{
		ClassReader classReader = new ClassReader(basicClass);
		ClassNode classNode = new ClassNode();
		classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

		for (MethodNode method : classNode.methods)
		{
			if ("<init>".equals(method.name))
			{
				method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mumfrey/liteloader/launch/LiteLoaderTweaker", "postInit", "()V"));
				method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mumfrey/liteloader/launch/LiteLoaderTweaker", "init", "()V"));
			}
		}
		
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}
}
