package com.mumfrey.liteloader.core;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.INetHandler;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import com.mumfrey.liteloader.PluginChannelListener;
import com.mumfrey.liteloader.permissions.PermissionsManagerClient;

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
	 * @param netHandler
	 * @param loginPacket
	 */
	public void onConnectToServer(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
		this.setupPluginChannels();
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
	protected void setupPluginChannels()
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
			PluginChannels.dispatch(new C17PacketCustomPayload(CHANNEL_REGISTER, registrationData));
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
	public static void sendMessage(String channel, byte[] data)
	{
		if (channel == null || channel.length() > 16 || CHANNEL_REGISTER.equals(channel) || CHANNEL_UNREGISTER.equals(channel))
			throw new RuntimeException("Invalid channel name specified"); 
		
		C17PacketCustomPayload payload = new C17PacketCustomPayload(channel, data);
		PluginChannels.dispatch(payload);
	}

	/**
	 * @param channel
	 * @param data
	 */
	private static void dispatch(C17PacketCustomPayload payload)
	{
		try
		{
			Minecraft minecraft = Minecraft.getMinecraft();
			
			if (minecraft.thePlayer != null && minecraft.thePlayer.sendQueue != null)
				minecraft.thePlayer.sendQueue.addToSendQueue(payload);
		}
		catch (Exception ex) {}
	}
}
