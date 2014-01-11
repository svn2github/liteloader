package com.mumfrey.liteloader.core.transformers;

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

import com.mumfrey.liteloader.core.runtime.Obf;

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
			this(callbackMethod, Obf.InjectedCallbackProxy.ref);
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
	
	private Map<String, Map<String, Callback>> mappings = new HashMap<String, Map<String, Callback>>();
	
	public CallbackInjectionTransformer()
	{
		this.addMappings(Obf.MCP); // @MCPONLY
		this.addMappings(Obf.SRG);
		this.addMappings(Obf.OBF);
	}

	protected void addMappings(int type)
	{
		this.addMapping(Obf.Minecraft.names[type],      Obf.runGameLoop.names[type],           "()V",     Obf.startSection.names[type],    "tick",         new Callback("onTimerUpdate"));
		this.addMapping(Obf.Minecraft.names[type],      Obf.runGameLoop.names[type],           "()V",     Obf.endStartSection.names[type], "gameRenderer", new Callback("onRender"));
		this.addMapping(Obf.Minecraft.names[type],      Obf.runTick.names[type],               "()V",     Obf.endStartSection.names[type], "animateTick",  new Callback("onAnimateTick"));
		this.addMapping(Obf.Minecraft.names[type],      Obf.runGameLoop.names[type],           "()V",     Obf.endSection.names[type],      "",             new Callback("onTick")); // ref 2
		this.addMapping(Obf.EntityRenderer.names[type], Obf.updateCameraAndRender.names[type], "(F)V",    Obf.endSection.names[type],      "",             new Callback("preRenderGUI")); // ref 1
		this.addMapping(Obf.EntityRenderer.names[type], Obf.updateCameraAndRender.names[type], "(F)V",    Obf.endSection.names[type],      "",             new Callback("postRenderHUDandGUI")); // ref 2
		this.addMapping(Obf.EntityRenderer.names[type], Obf.updateCameraAndRender.names[type], "(F)V",    Obf.endStartSection.names[type], "gui",          new Callback("onRenderHUD"));
		this.addMapping(Obf.EntityRenderer.names[type], Obf.renderWorld.names[type],           "(FJ)V",   Obf.endStartSection.names[type], "frustrum",     new Callback("onSetupCameraTransform"));
		this.addMapping(Obf.EntityRenderer.names[type], Obf.renderWorld.names[type],           "(FJ)V",   Obf.endStartSection.names[type], "litParticles", new Callback("postRenderEntities"));
		this.addMapping(Obf.EntityRenderer.names[type], Obf.renderWorld.names[type],           "(FJ)V",   Obf.endSection.names[type],      "",             new Callback("postRender"));
		this.addMapping(Obf.GuiIngame.names[type],      Obf.renderGameOverlay.names[type],     "(FZII)V", Obf.startSection.names[type],    "chat",         new Callback("onRenderChat"));
		this.addMapping(Obf.GuiIngame.names[type],      Obf.renderGameOverlay.names[type],     "(FZII)V", Obf.endSection.names[type],      "",             new Callback("postRenderChat")); // ref 10
	}
	
	/**
	 * @param className
	 * @param methodName
	 * @param methodSignature
	 * @param invokeName
	 * @param section
	 * @param callback
	 */
	protected void addMapping(String className, String methodName, String methodSignature, String invokeName, String section, Callback callback)
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
					if (Obf.Profiler.ref.equals(invokeNode.owner) || Obf.Profiler.obf.equals(invokeNode.owner))
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
	protected ClassNode readClass(byte[] basicClass)
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
	protected byte[] writeClass(ClassNode classNode)
	{
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		return writer.toByteArray();
	}
}
