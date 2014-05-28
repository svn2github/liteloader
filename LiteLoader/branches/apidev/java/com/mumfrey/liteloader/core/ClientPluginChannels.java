package com.mumfrey.liteloader.core;

import net.minecraft.network.INetHandler;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import com.mumfrey.liteloader.PluginChannelListener;
import com.mumfrey.liteloader.api.Listener;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * Handler for client plugin channels
 * 
 * @author Adam Mummery-Smith
 */
public abstract class ClientPluginChannels extends PluginChannels<PluginChannelListener>
{
	private static ClientPluginChannels instance;
	
	protected ClientPluginChannels()
	{
		ClientPluginChannels.instance = this;
	}

	protected static ClientPluginChannels getInstance()
	{
		return ClientPluginChannels.instance;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.InterfaceProvider#initProvider()
	 */
	@Override
	public void initProvider()
	{
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.InterfaceProvider#getListenerBaseType()
	 */
	@Override
	public Class<? extends Listener> getListenerBaseType()
	{
		return Listener.class;
	}

	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.api.InterfaceProvider#registerInterfaces(com.mumfrey.liteloader.core.InterfaceRegistrationDelegate)
	 */
	@Override
	public void registerInterfaces(InterfaceRegistrationDelegate delegate)
	{
		delegate.registerInterface(PluginChannelListener.class);
	}
	
	void addClientPluginChannelListener(PluginChannelListener pluginChannelListener)
	{
		super.addPluginChannelListener(pluginChannelListener);
	}

	/**
	 * Callback for the plugin channel hook
	 * 
	 * @param customPayload
	 */
	public abstract void onPluginChannelMessage(S3FPacketCustomPayload customPayload);

	/**
	 * @param channel
	 * @param data
	 */
	protected void onPluginChannelMessage(String channel, byte[] data)
	{
		if (PluginChannels.CHANNEL_REGISTER.equals(channel))
		{
			this.onRegisterPacketReceived(data);
		}
		else if (this.pluginChannels.containsKey(channel))
		{
			try
			{
				PermissionsManagerClient permissionsManager = LiteLoader.getClientPermissionsManager();
				if (permissionsManager != null)
				{
					permissionsManager.onCustomPayload(channel, data.length, data);
				}
			}
			catch (Exception ex) {}
			
			this.onModPacketReceived(channel, data, data.length);
		}
	}
	
	/**
	 * @param channel
	 * @param data
	 * @param length
	 */
	protected void onModPacketReceived(String channel, byte[] data, int length)
	{
		for (PluginChannelListener pluginChannelListener : this.pluginChannels.get(channel))
		{
			try
			{
				pluginChannelListener.onCustomPayload(channel, length, data);
				throw new RuntimeException();
			}
			catch (Exception ex)
			{
				int failCount = 1;
				if (this.faultingPluginChannelListeners.containsKey(pluginChannelListener))
					failCount = this.faultingPluginChannelListeners.get(pluginChannelListener).intValue() + 1;
				
				if (failCount >= PluginChannels.WARN_FAULT_THRESHOLD)
				{
					LiteLoaderLogger.warning("Plugin channel listener %s exceeded fault threshold on channel %s with %s", pluginChannelListener.getName(), channel, ex.getClass().getSimpleName());
					this.faultingPluginChannelListeners.remove(pluginChannelListener);
				}
				else
				{
					this.faultingPluginChannelListeners.put(pluginChannelListener, Integer.valueOf(failCount));
				}
			}
		}
	}

	protected void sendRegisteredPluginChannels(INetHandler netHandler)
	{
		// Add the permissions manager channels
		this.addPluginChannelsFor(LiteLoader.getClientPermissionsManager());
		
		try
		{
			byte[] registrationData = this.getRegistrationData();
			if (registrationData != null)
			{
				this.sendRegistrationData(netHandler, registrationData);
			}
		}
		catch (Exception ex)
		{
			LiteLoaderLogger.warning(ex, "Error dispatching REGISTER packet to server %s", ex.getClass().getSimpleName());
		}
	}

	/**
	 * @param netHandler
	 * @param registrationData
	 */
	protected abstract void sendRegistrationData(INetHandler netHandler, byte[] registrationData);

	/**
	 * Send a message to the server on a plugin channel
	 * 
	 * @param channel Channel to send, must not be a reserved channel name
	 * @param data
	 */
	public static boolean sendMessage(String channel, byte[] data, ChannelPolicy policy)
	{
		if (ClientPluginChannels.instance != null)
		{
			return ClientPluginChannels.instance.send(channel, data, policy);
		}
		
		return false;
	}

	/**
	 * Send a message to the server on a plugin channel
	 * 
	 * @param channel Channel to send, must not be a reserved channel name
	 * @param data
	 */
	protected abstract boolean send(String channel, byte[] data, ChannelPolicy policy);
}
