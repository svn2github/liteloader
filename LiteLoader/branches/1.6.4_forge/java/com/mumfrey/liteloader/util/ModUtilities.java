package com.mumfrey.liteloader.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.IntHashMap;

import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.PluginChannels;
import com.mumfrey.liteloader.launch.LiteLoaderTweaker;

public abstract class ModUtilities
{
	/**
	 * Collection of packets we have already overridden, so that duplicate registrations can generate a warning
	 */
	private static Set<Integer> overriddenPackets = new HashSet<Integer>();
	
	/**
	 * True if FML is being used, in which case we use searge names instead of raw field/method names
	 */
	private static boolean fmlDetected = false;
	
	static
	{
		// Check for FML
		ModUtilities.fmlDetected = ModUtilities.fmlIsPresent();
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
	 * Register a packet override
	 * 
	 * @param packetId
	 * @param newPacket
	 */
	@SuppressWarnings("unchecked")
	public static boolean registerPacketOverride(int packetId, Class<? extends Packet> newPacket)
	{
		if (packetId == 250)
		{
			throw new RuntimeException("Cannot override packet 250, register a plugin channel listener instead");
		}
		
		if (overriddenPackets.contains(Integer.valueOf(packetId)))
		{
			LiteLoader.getLogger().warning(String.format("Packet with ID %s was already overridden by another mod, one or mods may not function correctly", packetId));
		}
		
		try
		{
			IntHashMap packetIdToClassMap = Packet.packetIdToClassMap;
			PrivateFields.StaticFields.packetClassToIdMap.get();
			Map<Class<? extends Packet>, Integer> packetClassToIdMap = PrivateFields.StaticFields.packetClassToIdMap.get();
			
			packetIdToClassMap.removeObject(packetId);
			packetIdToClassMap.addKey(packetId, newPacket);
			packetClassToIdMap.put(newPacket, Integer.valueOf(packetId));
			
			return true;
		}
		catch (Exception ex)
		{
			LiteLoader.getLogger().warning("Error registering packet override for packet id " + packetId + ": " + ex.getMessage());
			return false;
		}
	}
	
	/**
	 * Send a plugin channel (custom payload) packet to the server
	 * 
	 * @param channel Channel to send the data
	 * @param data
	 * 
	 * @deprecated User PluginChannels.sendMessage(channel, data) instead.
	 */
	@Deprecated
	public static void sendPluginChannelMessage(String channel, byte[] data)
	{
		PluginChannels.sendMessage(channel, data);
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
		return deobfuscated ? fieldName : (ModUtilities.fmlDetected ? seargeFieldName : obfuscatedFieldName);
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
