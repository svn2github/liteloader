package com.mumfrey.liteloader.client.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.integrated.IntegratedServer;

import com.mumfrey.liteloader.client.ClientEvents;
import com.mumfrey.liteloader.client.ClientPluginChannelsClient;
import com.mumfrey.liteloader.client.GameEngineClient;
import com.mumfrey.liteloader.client.LiteLoaderModPanelManager;
import com.mumfrey.liteloader.client.gui.startup.LoadingBar;
import com.mumfrey.liteloader.common.GameEngine;
import com.mumfrey.liteloader.core.ClientPluginChannels;
import com.mumfrey.liteloader.core.Events;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.ServerPluginChannels;
import com.mumfrey.liteloader.interfaces.ModPanelManager;
import com.mumfrey.liteloader.interfaces.ObjectFactory;
import com.mumfrey.liteloader.launch.LoaderEnvironment;
import com.mumfrey.liteloader.launch.LoaderProperties;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.permissions.PermissionsManagerServer;

/**
 * Factory for lifetime loader objects for the client side
 * 
 * @author Adam Mummery-Smith
 */
class ObjectFactoryClient implements ObjectFactory<Minecraft, IntegratedServer>
{
	private LoaderEnvironment environment;
	
	private LoaderProperties properties;
	
	private ClientEvents clientEvents;

	private GameEngineClient engine;
	
	private ModPanelManager<GuiScreen> modPanelManager;
	
	private ClientPluginChannelsClient clientPluginChannels;
	
	private ServerPluginChannels serverPluginChannels;

	ObjectFactoryClient(LoaderEnvironment environment, LoaderProperties properties)
	{
		this.environment = environment;
		this.properties = properties;
	}
	
	@Override
	public Events<Minecraft, IntegratedServer> getEventBroker()
	{
		if (this.clientEvents == null)
		{
			this.clientEvents = new ClientEvents(LiteLoader.getInstance(), (GameEngineClient)this.getGameEngine(), this.properties);
		}
		
		return this.clientEvents;
	}
	
	@Override
	public GameEngine<Minecraft, IntegratedServer> getGameEngine()
	{
		if (this.engine == null)
		{
			this.engine = new GameEngineClient();
		}	
		
		return this.engine;
	}
	
	@Override
	public Profiler getProfiler()
	{
		return this.getGameEngine().getProfiler();
	}
	
	@Override
	public ModPanelManager<GuiScreen> getModPanelManager()
	{
		if (this.modPanelManager == null)
		{
			this.modPanelManager = new LiteLoaderModPanelManager(this.getGameEngine(), this.environment, this.properties);
		}
		
		return this.modPanelManager;
	}
	
	@Override
	public ClientPluginChannels getClientPluginChannels()
	{
		if (this.clientPluginChannels == null)
		{
			this.clientPluginChannels = new ClientPluginChannelsClient();
		}	

		return this.clientPluginChannels;
	}
	
	@Override
	public ServerPluginChannels getServerPluginChannels()
	{
		if (this.serverPluginChannels == null)
		{
			this.serverPluginChannels = new ServerPluginChannels();
		}	

		return this.serverPluginChannels;
	}
	
	@Override
	public PermissionsManagerClient getClientPermissionManager()
	{
		return PermissionsManagerClient.getInstance();
	}
	
	@Override
	public PermissionsManagerServer getServerPermissionManager()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@SuppressWarnings("unused")
	@Override
	public void preBeginGame()
	{
		new LoadingBar();
	}
}
