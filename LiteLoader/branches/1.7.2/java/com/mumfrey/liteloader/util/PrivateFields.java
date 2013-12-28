package com.mumfrey.liteloader.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.network.NetworkManager;
import net.minecraft.profiler.IPlayerUsage;
import net.minecraft.profiler.PlayerUsageSnooper;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.Timer;

/**
 * Wrapper for obf/mcp reflection-accessed private fields, mainly added to centralise the locations I have to update the obfuscated field names
 * 
 * @author Adam Mummery-Smith
 *
 * @param <P> Parent class type, the type of the class that owns the field
 * @param <T> Field type, the type of the field value
 * 
 * TODO Obfuscation - updated 1.7.2
 */
@SuppressWarnings("rawtypes")
public class PrivateFields<P, T>
{
	/**
	 * Class to which this field belongs
	 */
	public final Class<P> parentClass;

	/**
	 * MCP name for this field
	 */
	public final String mcpName;

	/**
	 * Real (obfuscated) name for this field
	 */
	public final String name;
	
	public final String seargeName;
	
	/**
	 * Name used to access the field, determined at init
	 */
	private final String fieldName;
	
	/**
	 * Creates a new private field entry
	 * 
	 * @param owner
	 * @param mcpName
	 * @param name
	 */
	private PrivateFields(Class<P> owner, String mcpName, String name, String seargeName)
	{
		this.parentClass = owner;
		this.mcpName     = mcpName;
		this.name        = name;
		this.seargeName  = seargeName;
		
		this.fieldName = ModUtilities.getObfuscatedFieldName(mcpName, name, seargeName);
	}
	
	/**
	 * Get the current value of this field on the instance class supplied
	 * 
	 * @param instance Class to get the value of
	 * @return field value or null if errors occur
	 */
	@SuppressWarnings("unchecked")
	public T get(P instance)
	{
		try
		{
			Field field = this.parentClass.getDeclaredField(this.fieldName);
			field.setAccessible(true);
			return (T)field.get(instance);
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	/**
	 * Set the value of this field on the instance class supplied
	 * 
	 * @param instance Object to set the value of the field on
	 * @param value value to set
	 * @return value
	 */
	public T set(P instance, T value)
	{
		try
		{
			Field field = this.parentClass.getDeclaredField(this.fieldName);
			field.setAccessible(true);
			field.set(instance, value);
		}
		catch (Exception ex) { }
		
		return value;
	}
	
	/**
	 * Set the value of this FINAL field on the instance class supplied
	 * 
	 * @param instance Object to set the value of the field on
	 * @param value value to set
	 * @return value
	 */
	public T setFinal(P instance, T value)
	{
		try
		{
			Field modifiers = Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			
			Field field = this.parentClass.getDeclaredField(this.fieldName);
			modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			field.setAccessible(true);
			field.set(instance, value);
		}
		catch (Exception ex) { }
		
		return value;
	}

	public static final PrivateFields<Minecraft, Timer>                       minecraftTimer = new PrivateFields<Minecraft, Timer>                (Minecraft.class,          "timer",                "Q",  "field_71428_T");   // Minecraft/timer
	public static final PrivateFields<Minecraft, Profiler>                 minecraftProfiler = new PrivateFields<Minecraft, Profiler>             (Minecraft.class,          "mcProfiler",           "A",  "field_71424_I");   // Minecraft/mcProfiler
	public static final PrivateFields<Minecraft, List<IResourcePack>>    defaultResourcePacks = new PrivateFields<Minecraft, List<IResourcePack>> (Minecraft.class,          "defaultResourcePacks", "ap", "field_110449_ao"); // Minecraft/defaultResourcePacks
	public static final PrivateFields<Minecraft, Boolean>                      gameIsRunning = new PrivateFields<Minecraft, Boolean>              (Minecraft.class,          "running",              "B",  "field_71425_J");   // Minecraft/running
	public static final PrivateFields<RenderManager, Map>                    entityRenderMap = new PrivateFields<RenderManager, Map>              (RenderManager.class,      "entityRenderMap",      "q",  "field_78729_o");   // RenderManager/entityRenderMap
	public static final PrivateFields<PlayerUsageSnooper, IPlayerUsage> playerStatsCollector = new PrivateFields<PlayerUsageSnooper, IPlayerUsage>(PlayerUsageSnooper.class, "playerStatsCollector", "d",  "field_76478_d");   // PlayerUsageSnooper/playerStatsCollector
	public static final PrivateFields<SimpleReloadableResourceManager, List<IResourceManagerReloadListener>> reloadListeners = new PrivateFields<SimpleReloadableResourceManager, List<IResourceManagerReloadListener>>(SimpleReloadableResourceManager.class, "reloadListeners", "d", "field_110546_b");   // SimpleReloadableResourceManager/reloadListeners
	public static final PrivateFields<NetHandlerLoginClient, NetworkManager>     netManager = new PrivateFields<NetHandlerLoginClient, NetworkManager>(NetHandlerLoginClient.class, "field_147393_d", "d", "field_147393_d");   // NetHandlerLoginClient/field_147393_d
}

