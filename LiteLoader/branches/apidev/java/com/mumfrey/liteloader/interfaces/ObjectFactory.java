package com.mumfrey.liteloader.interfaces;

import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;

import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.core.ClientPluginChannels;
import com.mumfrey.liteloader.core.Events;
import com.mumfrey.liteloader.core.ServerPluginChannels;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.permissions.PermissionsManagerServer;

/**
 * Factory for generating loader managament objects based on the environment
 * 
 * @author Adam Mummery-Smith
 *
 * @param <TClient> Type of the client runtime, "Minecraft" on client and null on the server
 * @param <TServer> Type of the server runtime, "IntegratedServer" on the client, "MinecraftServer" on the server 
 * @param <TGuiScreen> GuiScreen class, must be generic because we don't have GuiScreen on the server side
 */
public interface ObjectFactory<TClient, TServer extends MinecraftServer>
{
	public abstract Events<TClient, TServer> getEventBroker();
	
	public abstract GameEngine<TClient, TServer> getGameEngine();
	
	public abstract Profiler getProfiler();
	
	public abstract ModPanelManager<?> getModPanelManager();
	
	public abstract ClientPluginChannels getClientPluginChannels();
	
	public abstract ServerPluginChannels getServerPluginChannels();

	public abstract PermissionsManagerClient getClientPermissionManager();

	public abstract PermissionsManagerServer getServerPermissionManager();

	public abstract void preBeginGame();
}
