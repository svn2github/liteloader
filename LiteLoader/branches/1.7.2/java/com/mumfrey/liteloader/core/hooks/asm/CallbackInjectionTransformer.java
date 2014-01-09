package com.mumfrey.liteloader.core.hooks.asm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.mumfrey.liteloader.core.hooks.asm.CallbackInjectionTransformer.Callback;

import net.minecraft.launchwrapper.IClassTransformer;

/**
 * Transformer which injects method calls in place of the old profiler hook
 * 
 * @author Adam Mummery-Smith
 */
public class CallbackInjectionTransformer implements IClassTransformer
{
	/**
	 * A callback method
	 * 
	 * @author Adam Mummery-Smith
	 */
	public class Callback
	{
		public final String callbackClass;
		public final String callbackMethod;
		public int refNumber;
		
		public Callback(String callbackMethod)
		{
			this(callbackMethod, "com/mumfrey/liteloader/core/hooks/asm/ASMHookProxy");
		}
		
		public Callback(String callbackMethod, String callbackClass)
		{
			this.callbackClass = callbackClass;
			this.callbackMethod = callbackMethod;
		}
		
		private Callback(String callbackMethod, String callbackClass, int refNumber)
		{
			this(callbackMethod, callbackClass);
			this.refNumber = refNumber;
		}
		
		public Callback getNextCallback()
		{
			return new Callback(this.callbackMethod, this.callbackClass, this.refNumber++);
		}
	}
	
	private static final String profilerClass = "net/minecraft/profiler/Profiler";

	// TODO Obfuscation 1.7.2
	private static final String profilerClassObf = "ov";
	
	private Map<String, Map<String, Callback>> mappings = new HashMap<String, Map<String, Callback>>();
	
	public CallbackInjectionTransformer()
	{
		this.addMappings(
			"net.minecraft.client.Minecraft",
			"net.minecraft.client.renderer.EntityRenderer",
			"net.minecraft.client.gui.GuiIngame",
			"runGameLoop",
			"runTick",
			"updateCameraAndRender",
			"renderWorld",
			"renderGameOverlay",
			"startSection",
			"endSection",
			"endStartSection"
		);
		
		// TODO Obfuscation 1.7.2
		this.addMappings(
			"net.minecraft.client.Minecraft",
			"net.minecraft.client.renderer.EntityRenderer",
			"net.minecraft.client.gui.GuiIngame",
			"func_71411_J", // runGameLoop
			"func_71407_l", // runTick,
			"func_78480_b", // updateCameraAndRender
			"func_78471_a", // renderWorld
			"func_73830_a", // renderGameOverlay
			"func_76320_a", // startSection
			"func_76319_b", // endSection
			"func_76318_c"  // endStartSection
		);
		
		// TODO Obfuscation 1.7.2
		this.addMappings(
			"azd", // Minecraft
			"bll", // EntityRenderer
			"bah", // GuiIngame
			"ad",  // runGameLoop
			"o",   // runTick
			"b",   // updateCameraAndRender
			"a",   // renderWorld
			"a",   // renderGameOverlay
			"a",   // startSection
			"b",   // endSection
			"c"    // endStartSection
		);
	}

	/**
	 * @param clsMinecraft
	 * @param clsEntityRenderer
	 * @param clsGuiIngame
	 * @param runGameLoop
	 * @param runTick
	 * @param updateCameraAndRender
	 * @param renderWorld
	 * @param renderGameOverlay
	 * @param startSection
	 * @param endSection
	 * @param endStartSection
	 */
	private void addMappings(String clsMinecraft, String clsEntityRenderer, String clsGuiIngame, String runGameLoop, String runTick, String updateCameraAndRender, String renderWorld, String renderGameOverlay, String startSection, String endSection, String endStartSection)
	{
		this.addMapping(clsMinecraft,      runGameLoop,           "()V",     startSection,    "tick",         new Callback("onTimerUpdate"));
		this.addMapping(clsMinecraft,      runGameLoop,           "()V",     endStartSection, "gameRenderer", new Callback("onRender"));
		this.addMapping(clsMinecraft,      runTick,               "()V",     endStartSection, "animateTick",  new Callback("onAnimateTick"));
		this.addMapping(clsMinecraft,      runGameLoop,           "()V",     endSection,      "",             new Callback("onTick")); // ref 2
		this.addMapping(clsEntityRenderer, updateCameraAndRender, "(F)V",    endSection,      "",             new Callback("preRenderGUI")); // ref 1
		this.addMapping(clsEntityRenderer, renderWorld,           "(FJ)V",   endStartSection, "frustrum",     new Callback("onSetupCameraTransform"));
		this.addMapping(clsEntityRenderer, renderWorld,           "(FJ)V",   endStartSection, "litParticles", new Callback("postRenderEntities"));
		this.addMapping(clsEntityRenderer, renderWorld,           "(FJ)V",   endSection,      "",             new Callback("postRender"));
		this.addMapping(clsEntityRenderer, updateCameraAndRender, "(F)V",    endStartSection, "gui",          new Callback("onRenderHUD"));
		this.addMapping(clsGuiIngame,      renderGameOverlay,     "(FZII)V", startSection,    "chat",         new Callback("onRenderChat"));
		this.addMapping(clsGuiIngame,      renderGameOverlay,     "(FZII)V", endSection,      "",             new Callback("postRenderChat")); // ref 10
		this.addMapping(clsEntityRenderer, updateCameraAndRender, "(F)V",    endSection,      "",             new Callback("postRenderHUDandGUI")); // ref 2
	}
	
	/**
	 * @param className
	 * @param methodName
	 * @param methodSignature
	 * @param invokeName
	 * @param section
	 * @param callback
	 */
	private void addMapping(String className, String methodName, String methodSignature, String invokeName, String section, Callback callback)
	{
		if (!this.mappings.containsKey(className))
		{
			this.mappings.put(className, new HashMap<String, Callback>());
		}
		
		String invokeDesc = section == null || section.length() == 0 ? "()V" : "(Ljava/lang/String;)V";
		String signature = CallbackInjectionTransformer.generateSignature(className, methodName, methodSignature, invokeName, invokeDesc, section);
		this.mappings.get(className).put(signature, callback);
	}

	/* (non-Javadoc)
	 * @see net.minecraft.launchwrapper.IClassTransformer#transform(java.lang.String, java.lang.String, byte[])
	 */
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (basicClass != null && this.mappings.containsKey(transformedName))
		{
			return this.injectCallbacks(basicClass, this.mappings.get(transformedName));
		}
		
		return basicClass;
	}

	/**
	 * @param basicClass
	 * @param mapping
	 * @return
	 */
	private byte[] injectCallbacks(byte[] basicClass, Map<String, Callback> mapping)
	{
		ClassNode classNode = this.readClass(basicClass);

		for (MethodNode method : classNode.methods)
		{
			String section = null;
			Map<MethodInsnNode, Callback> injectionNodes = new HashMap<MethodInsnNode, Callback>();
			
			Iterator<AbstractInsnNode> iter = method.instructions.iterator();
			AbstractInsnNode lastInsn = null;
			while (iter.hasNext())
			{
				AbstractInsnNode insn = iter.next();
				if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL)
				{
					MethodInsnNode invokeNode = (MethodInsnNode)insn;
					if (profilerClass.equals(invokeNode.owner) || profilerClassObf.equals(invokeNode.owner))
					{
						section = "";
						if (lastInsn instanceof LdcInsnNode)
						{
							section = ((LdcInsnNode)lastInsn).cst.toString();
						}

						String signature = CallbackInjectionTransformer.generateSignature(classNode.name, method.name, method.desc, invokeNode.name, invokeNode.desc, section);
						
						if (mapping.containsKey(signature))
						{
//							System.out.println("   Adding callback for " + mapping.get(signature).callbackMethod + " at " + signature + " index is " + mapping.get(signature).refNumber);
							injectionNodes.put(invokeNode, mapping.get(signature).getNextCallback());
						}
					}
				}
				
				lastInsn = insn;
			}
			
			for (Entry<MethodInsnNode, Callback> node : injectionNodes.entrySet())
			{
				Callback callback = node.getValue();
				method.instructions.insert(node.getKey(), new MethodInsnNode(Opcodes.INVOKESTATIC, callback.callbackClass, callback.callbackMethod, "(I)V"));
				method.instructions.insert(node.getKey(), new LdcInsnNode(callback.refNumber++));
			}
		}
		
		return this.writeClass(classNode);
	}

	/**
	 * @param className
	 * @param methodName
	 * @param methodSignature
	 * @param invokeName
	 * @param invokeSig
	 * @param section
	 * @return
	 */
	private static String generateSignature(String className, String methodName, String methodSignature, String invokeName, String invokeSig, String section)
	{
		return String.format("%s::%s%s@%s%s/%s", className.replace('.', '/'), methodName, methodSignature, invokeName, invokeSig, section);
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
