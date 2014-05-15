package com.mumfrey.liteloader.core.transformers;

import java.util.Iterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.launch.LiteLoaderTweaker;
import com.mumfrey.liteloader.transformers.ClassOverlayTransformer;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

public class MinecraftOverlayTransformer extends ClassOverlayTransformer
{
	private static final String overlayClassName = "com.mumfrey.liteloader.core.overlays.MinecraftOverlay";

	private static final String LITELOADER_TWEAKER_CLASS = LiteLoaderTweaker.class.getName().replace('.', '/');

	private static final String METHOD_INIT = "init";
	private static final String METHOD_POSTINIT = "postInit";
	
	private static boolean injected = false;

	public MinecraftOverlayTransformer()
	{
		super(MinecraftOverlayTransformer.overlayClassName);
	}
	
	@Override
	protected void postOverlayTransform(String transformedName, ClassNode targetClass, ClassNode overlayClass)
	{
		if ((Obf.Minecraft.name.equals(transformedName) || Obf.Minecraft.obf.equals(transformedName)))
		{
			for (MethodNode method : targetClass.methods)
			{
				if (Obf.startGame.obf.equals(method.name) || Obf.startGame.srg.equals(method.name) || Obf.startGame.name.equals(method.name))
				{
					this.transformStartGame(method);
				}
			}
		}
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
					LiteLoaderLogger.info("MinecraftOverlayTransformer found INIT injection point, this is good.");
					MinecraftOverlayTransformer.injected = true;
					
					InsnList insns = new InsnList();
					insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MinecraftOverlayTransformer.LITELOADER_TWEAKER_CLASS, MinecraftOverlayTransformer.METHOD_INIT, "()V"));
					insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, MinecraftOverlayTransformer.LITELOADER_TWEAKER_CLASS, MinecraftOverlayTransformer.METHOD_POSTINIT, "()V"));
					method.instructions.insertBefore(lastInsn, insns);
					return;
				}
			}
			
			lastInsn = insn;
		}

		LiteLoaderLogger.severe("MinecraftOverlayTransformer failed to find the INIT injection point, the game will probably crash pretty soon.");
	}
	
	public static boolean isInjected()
	{
		return MinecraftOverlayTransformer.injected;
	}
}
