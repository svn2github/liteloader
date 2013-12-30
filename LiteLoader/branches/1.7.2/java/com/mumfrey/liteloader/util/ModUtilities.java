package com.mumfrey.liteloader.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import com.mumfrey.liteloader.core.LiteLoader;

/**
 * A small collection of useful functions for mods
 * 
 * @author Adam Mummery-Smith
 */
public abstract class ModUtilities
{
	/**
	 * True if FML is being used, in which case we use searge names instead of raw field/method names
	 */
	private static boolean fmlDetected = false;
	
	private static boolean seargeNames = false;
	
	static
	{
		// Check for FML
		ModUtilities.fmlDetected = ModUtilities.fmlIsPresent();

		try
		{
			Minecraft.class.getDeclaredField("running");
		}
		catch (SecurityException ex)
		{
		}
		catch (NoSuchFieldException ex)
		{
			ModUtilities.seargeNames = true;
		}
	}

	/**
	 * @return
	 */
	public static boolean fmlIsPresent()
	{
		if (ClientBrandRetriever.getClientModName().contains("fml")) return true;

		for (IClassTransformer transformer : Launch.classLoader.getTransformers())
			if (transformer.getClass().getName().contains("fml")) return true;

		return false;
	}
	
	/**
	 * Add a renderer map entry for the specified entity class
	 * 
	 * @param entityClass
	 * @param renderer
	 */
	@SuppressWarnings("unchecked")
	public static void addRenderer(Class<? extends Entity> entityClass, Render renderer)
	{
		Map<Class<? extends Entity>, Render> entityRenderMap = PrivateFields.entityRenderMap.get(RenderManager.instance);
		entityRenderMap.put(entityClass, renderer);
		renderer.setRenderManager(RenderManager.instance);
	}
	
	/**
	 * Abstraction helper function
	 * 
	 * @param fieldName Name of field to get, returned unmodified if in debug mode
	 * @return Obfuscated field name if present
	 */
	public static String getObfuscatedFieldName(String fieldName, String obfuscatedFieldName, String seargeFieldName)
	{
		boolean deobfuscated = Tessellator.class.getSimpleName().equals("Tessellator");
		return deobfuscated ? (ModUtilities.seargeNames ? seargeFieldName : fieldName) : (ModUtilities.fmlDetected ? seargeFieldName : obfuscatedFieldName);
	}

	/**
	 * Registers a keybind with the game settings class so that it is configurable in the "controls" screen
	 * 
	 * @param newBinding key binding to add
	 */
	public static void registerKey(KeyBinding newBinding)
	{
		LiteLoader.getInstance().registerModKey(newBinding);
	}
	
	/**
	 * Unregisters a registered keybind with the game settings class, thus removing it from the "controls" screen
	 * 
	 * @param removeBinding
	 */
	public static void unRegisterKey(KeyBinding removeBinding)
	{
		Minecraft mc = Minecraft.getMinecraft();
		
		if (mc == null || mc.gameSettings == null) return;
		
		LinkedList<KeyBinding> keyBindings = new LinkedList<KeyBinding>();
		keyBindings.addAll(Arrays.asList(mc.gameSettings.keyBindings));
		
		if (keyBindings.contains(removeBinding))
		{
			keyBindings.remove(removeBinding);
			mc.gameSettings.keyBindings = keyBindings.toArray(new KeyBinding[0]);
		}
	}
}
