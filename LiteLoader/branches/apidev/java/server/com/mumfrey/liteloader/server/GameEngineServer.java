package com.mumfrey.liteloader.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;

import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.common.LoadingProgress;

/**
 *
 * @author Adam Mummery-Smith
 */
public class GameEngineServer implements GameEngine<DummyClient, MinecraftServer>
{
	/**
	 * 
	 */
	private final MinecraftServer engine = MinecraftServer.getServer();
	
	/**
	 * 
	 */
	private final DummyClient client = new DummyClient();

	private IResourceManager resourceManager;

	/**
	 * Registered resource packs 
	 */
	private final Map<String, IResourcePack> registeredResourcePacks = new HashMap<String, IResourcePack>();

	/**
	 * True while initialising mods if we need to do a resource manager reload once the process is completed
	 */
	private boolean pendingResourceReload;

	@Override
	public Profiler getProfiler()
	{
		return this.engine.theProfiler;
	}

	@Override
	public void refreshResources(boolean force)
	{
		if (this.pendingResourceReload || force)
		{
			LoadingProgress.setMessage("Reloading Resources...");
			this.pendingResourceReload = false;
//			this.engine.refreshResources();
		}
	}

	@Override
	public boolean isClient()
	{
		return false;
	}

	@Override
	public boolean isServer()
	{
		return true;
	}

	@Override
	public boolean isInGame()
	{
		return true;
	}
	
	@Override
	public boolean isRunning()
	{
		return this.engine.isServerRunning();
	}
	
	@Override
	public boolean isSinglePlayer()
	{
		return false;
	}

	@Override
	public DummyClient getClient()
	{
		return this.client;
	}

	@Override
	public MinecraftServer getServer()
	{
		return this.engine;
	}
	
	@Override
	public IResourceManager getResourceManager()
	{
		return this.resourceManager;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#registerResourcePack(net.minecraft.client.resources.IResourcePack)
	 */
	@Override
	public boolean registerResourcePack(IResourcePack resourcePack)
	{
		if (!this.registeredResourcePacks.containsKey(resourcePack.getPackName()))
		{
			this.pendingResourceReload = true;
			this.registeredResourcePacks.put(resourcePack.getPackName(), resourcePack);
			return true;
		}
		
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.common.GameEngine#unRegisterResourcePack(net.minecraft.client.resources.IResourcePack)
	 */
	@Override
	public boolean unRegisterResourcePack(IResourcePack resourcePack)
	{
		if (this.registeredResourcePacks.containsValue(resourcePack))
		{
			this.pendingResourceReload = true;
			this.registeredResourcePacks.remove(resourcePack.getPackName());
			return true;
		}
		
		return false;
	}
	
	@Override
	public List<KeyBinding> getKeyBindings()
	{
		throw new RuntimeException("Minecraft Server does not support key bindings");
	}
	
	@Override
	public void setKeyBindings(List<KeyBinding> keyBindings)
	{
		throw new RuntimeException("Minecraft Server does not support key bindings");
	}
}
