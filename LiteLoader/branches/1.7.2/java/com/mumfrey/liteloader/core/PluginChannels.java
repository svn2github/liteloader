package com.mumfrey.liteloader.core;

import io.netty.util.concurrent.GenericFutureListener;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.INetHandler;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import com.mumfrey.liteloader.PluginChannelListener;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;
import com.mumfrey.liteloader.util.PrivateFields;

/**
 * Manages plugin channel connections and subscriptions for LiteLoader
 *
 * @author Adam Mummery-Smith
 */
public class PluginChannels
{
	// reserved channel consts
	private static final String CHANNEL_REGISTER = "REGISTER";
	private static final String CHANNEL_UNREGISTER = "UNREGISTER";

	/**
	 * Mapping of plugin channel names to listeners
	 */
	private HashMap<String, LinkedList<PluginChannelListener>> pluginChannels = new HashMap<String, LinkedList<PluginChannelListener>>();
	
	/**
	 * List of mods which implement PluginChannelListener interface
	 */
	private LinkedList<PluginChannelListener> pluginChannelListeners = new LinkedList<PluginChannelListener>();
	
	/**
	 * Package private
	 */
	PluginChannels()
	{
	}
	
	/**
	 * @param pluginChannelListener
	 */
	public void addPluginChannelListener(PluginChannelListener pluginChannelListener)
	{
		if (!this.pluginChannelListeners.contains(pluginChannelListener))
		{
			this.pluginChannelListeners.add(pluginChannelListener);
		}
	}

	/**
	 * Callback for the plugin channel hook
	 * 
	 * @param customPayload
	 */
	public void onPluginChannelMessage(S3FPacketCustomPayload customPayload)
	{
		if (customPayload != null && customPayload.getChannel() != null && this.pluginChannels.containsKey(customPayload.getChannel()))
		{
			try
			{
				PermissionsManagerClient permissionsManager = LiteLoader.getPermissionsManager();
				if (permissionsManager != null)
				{
					permissionsManager.onCustomPayload(customPayload.getChannel(), customPayload.getData().length, customPayload.getData());
				}
			}
			catch (Exception ex) {}
			
			for (PluginChannelListener pluginChannelListener : this.pluginChannels.get(customPayload.getChannel()))
			{
				try
				{
					pluginChannelListener.onCustomPayload(customPayload.getChannel(), customPayload.getData().length, customPayload.getData());
				}
				catch (Exception ex) {}
			}
		}
	}
	
	/**
	 * Query loaded mods for registered channels
	 */
	void setupPluginChannels(INetHandler netHandler)
	{
		try
		{
			// Clear any channels from before
			this.pluginChannels.clear();
			
			// Add the permissions manager channels
			this.addPluginChannelsFor(LiteLoader.getPermissionsManager());
			
			// Enumerate mods for plugin channels
			for (PluginChannelListener pluginChannelListener : this.pluginChannelListeners)
			{
				this.addPluginChannelsFor(pluginChannelListener);
			}
			
			// If any mods have registered channels, send the REGISTER packet
			if (this.pluginChannels.keySet().size() > 0)
			{
				StringBuilder channelList = new StringBuilder();
				boolean separator = false;
				
				for (String channel : this.pluginChannels.keySet())
				{
					if (separator) channelList.append("\u0000");
					channelList.append(channel);
					separator = true;
				}
				
				byte[] registrationData = channelList.toString().getBytes(Charset.forName("UTF8"));

				if (netHandler instanceof INetHandlerLoginClient)
				{
					INetworkManager networkManager = PrivateFields.netManager.get(((NetHandlerLoginClient)netHandler));
					networkManager.func_150725_a(new C17PacketCustomPayload(CHANNEL_REGISTER, registrationData), new GenericFutureListener[0]);
				}
				else if (netHandler instanceof INetHandlerPlayClient)
				{
					PluginChannels.dispatch(new C17PacketCustomPayload(CHANNEL_REGISTER, registrationData));
				}
			}
		}
		catch (Exception ex)
		{
			LiteLoader.getLogger().log(Level.WARNING, "Error dispatching REGISTER packet to server " + ex.getClass().getSimpleName(), ex);
		}
	}
	
	/**
	 * Adds plugin channels for the specified listener to the local channels
	 * collection
	 * 
	 * @param pluginChannelListener
	 */
	private void addPluginChannelsFor(PluginChannelListener pluginChannelListener)
	{
		List<String> channels = pluginChannelListener.getChannels();
		
		if (channels != null)
		{
			for (String channel : channels)
			{
				if (channel.length() > 16 || channel.toUpperCase().equals(CHANNEL_REGISTER) || channel.toUpperCase().equals(CHANNEL_UNREGISTER))
					continue;
				
				if (!this.pluginChannels.containsKey(channel))
				{
					this.pluginChannels.put(channel, new LinkedList<PluginChannelListener>());
				}
				
				this.pluginChannels.get(channel).add(pluginChannelListener);
			}
		}
	}
	
	/**
	 * Send a message on a plugin channel
	 * 
	 * @param channel Channel to send, must not be a reserved channel name
	 * @param data
	 */
	public static boolean sendMessage(String channel, byte[] data)
	{
		if (channel == null || channel.length() > 16 || CHANNEL_REGISTER.equals(channel) || CHANNEL_UNREGISTER.equals(channel))
			throw new RuntimeException("Invalid channel name specified"); 
		
		C17PacketCustomPayload payload = new C17PacketCustomPayload(channel, data);
		return PluginChannels.dispatch(payload);
	}

	/**
	 * @param channel
	 * @param data
	 */
	private static boolean dispatch(C17PacketCustomPayload payload)
	{
		try
		{
			Minecraft minecraft = Minecraft.getMinecraft();
			
			if (minecraft.thePlayer != null && minecraft.thePlayer.sendQueue != null)
			{
				minecraft.thePlayer.sendQueue.addToSendQueue(payload);
				return true;
			}
		}
		catch (Exception ex) {}
		
		return false;
	}
}
