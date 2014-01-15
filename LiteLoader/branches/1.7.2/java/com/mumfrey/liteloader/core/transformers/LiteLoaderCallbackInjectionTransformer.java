package com.mumfrey.liteloader.core.transformers;

import org.objectweb.asm.Type;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.core.transformers.Callback.CallBackType;

/**
 * Transformer which injects method calls in place of the old profiler hook
 * 
 * @author Adam Mummery-Smith
 */
public final class LiteLoaderCallbackInjectionTransformer extends CallbackInjectionTransformer
{
	/**
	 * Add mappings
	 */
	@Override
	protected void addMappings()
	{
		this.addMappings(Obf.MCP); // @MCPONLY
		this.addMappings(Obf.SRG);
		this.addMappings(Obf.OBF);
	}

	private void addMappings(int type)
	{
		this.addProfilerCallbackMapping(type, Obf.Minecraft,      Obf.runGameLoop,           "()V",     Obf.startSection,    "tick",         new Callback("onTimerUpdate"));
		this.addProfilerCallbackMapping(type, Obf.Minecraft,      Obf.runGameLoop,           "()V",     Obf.endStartSection, "gameRenderer", new Callback("onRender"));
		this.addProfilerCallbackMapping(type, Obf.Minecraft,      Obf.runTick,               "()V",     Obf.endStartSection, "animateTick",  new Callback("onAnimateTick"));
		this.addProfilerCallbackMapping(type, Obf.Minecraft,      Obf.runGameLoop,           "()V",     Obf.endSection,      "",             new Callback("onTick")); // ref 2
		this.addProfilerCallbackMapping(type, Obf.EntityRenderer, Obf.updateCameraAndRender, "(F)V",    Obf.endSection,      "",             new Callback("preRenderGUI")); // ref 1
		this.addProfilerCallbackMapping(type, Obf.EntityRenderer, Obf.updateCameraAndRender, "(F)V",    Obf.endSection,      "",             new Callback("postRenderHUDandGUI")); // ref 2
		this.addProfilerCallbackMapping(type, Obf.EntityRenderer, Obf.updateCameraAndRender, "(F)V",    Obf.endStartSection, "gui",          new Callback("onRenderHUD"));
		this.addProfilerCallbackMapping(type, Obf.EntityRenderer, Obf.renderWorld,           "(FJ)V",   Obf.endStartSection, "frustrum",     new Callback("onSetupCameraTransform"));
		this.addProfilerCallbackMapping(type, Obf.EntityRenderer, Obf.renderWorld,           "(FJ)V",   Obf.endStartSection, "litParticles", new Callback("postRenderEntities"));
		this.addProfilerCallbackMapping(type, Obf.EntityRenderer, Obf.renderWorld,           "(FJ)V",   Obf.endSection,      "",             new Callback("postRender"));
		this.addProfilerCallbackMapping(type, Obf.GuiIngame,      Obf.renderGameOverlay,     "(FZII)V", Obf.startSection,    "chat",         new Callback("onRenderChat"));
		this.addProfilerCallbackMapping(type, Obf.GuiIngame,      Obf.renderGameOverlay,     "(FZII)V", Obf.endSection,      "",             new Callback("postRenderChat")); // ref 10
		
		String integratedServerCtorDescriptor = CallbackInjectionTransformer.generateDescriptor(type, Type.VOID_TYPE, Obf.Minecraft, String.class, String.class, Obf.WorldSettings);
		String initPlayerConnectionDescriptor = CallbackInjectionTransformer.generateDescriptor(type, Type.VOID_TYPE, Obf.NetworkManager, Obf.EntityPlayerMP);
		String playerLoggedInOutDescriptor    = CallbackInjectionTransformer.generateDescriptor(type, Type.VOID_TYPE, Obf.EntityPlayerMP);
		String spawnPlayerDescriptor          = CallbackInjectionTransformer.generateDescriptor(type, Obf.EntityPlayerMP, Obf.GameProfile);
		String respawnPlayerDescriptor        = CallbackInjectionTransformer.generateDescriptor(type, Obf.EntityPlayerMP, Obf.EntityPlayerMP, Type.INT_TYPE, Type.BOOLEAN_TYPE);
		
		this.addCallbackMapping(type, Obf.IntegratedServer,           Obf.constructor,                  integratedServerCtorDescriptor, CallBackType.RETURN, new Callback("IntegratedServerCtor"));
		this.addCallbackMapping(type, Obf.ServerConfigurationManager, Obf.initializeConnectionToPlayer, initPlayerConnectionDescriptor, CallBackType.RETURN, new Callback("onInitializePlayerConnection", false));
		this.addCallbackMapping(type, Obf.ServerConfigurationManager, Obf.playerLoggedIn,               playerLoggedInOutDescriptor,    CallBackType.RETURN, new Callback("onPlayerLogin",                false));
		this.addCallbackMapping(type, Obf.ServerConfigurationManager, Obf.playerLoggedOut,              playerLoggedInOutDescriptor,    CallBackType.RETURN, new Callback("onPlayerLogout",               false));
		this.addCallbackMapping(type, Obf.ServerConfigurationManager, Obf.spawnPlayer,                  spawnPlayerDescriptor,          CallBackType.RETURN, new Callback("onSpawnPlayer",                true));
		this.addCallbackMapping(type, Obf.ServerConfigurationManager, Obf.respawnPlayer,                respawnPlayerDescriptor,        CallBackType.RETURN, new Callback("onRespawnPlayer",              true));
	}
	
	/**
	 * @param type
	 * @param className
	 * @param methodName
	 * @param methodSignature
	 * @param invokeMethod
	 * @param section
	 * @param callback
	 */
	private void addProfilerCallbackMapping(int type, Obf className, Obf methodName, String methodSignature, Obf invokeMethod, String section, Callback callback)
	{
		this.addProfilerCallbackMapping(className.names[type], methodName.names[type], methodSignature, invokeMethod.names[type], section, callback);
	}
	
	/**
	 * @param type
	 * @param className
	 * @param methodName
	 * @param methodSignature
	 * @param callbackType
	 * @param callback
	 */
	private void addCallbackMapping(int type, Obf className, Obf methodName, String methodSignature, Callback.CallBackType callbackType, Callback callback)
	{
		this.addCallbackMapping(className.names[type], methodName.names[type], methodSignature, callbackType, callback);
	}
}
