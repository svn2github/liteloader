package com.mumfrey.liteloader.core;

import io.netty.util.concurrent.GenericFutureListener;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.INetHandler;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.PluginChannelListener;
import com.mumfrey.liteloader.core.exceptions.UnregisteredChannelException;
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
	 * Number of faults for a specific listener before a warning is generated
	 */
	private static final int WARN_FAULT_THRESHOLD = 1000;
	
	/**
	 * Active instance
	 */
	private static PluginChannels instance;

	/**
	 * Mapping of plugin channel names to listeners
	 */
	private HashMap<String, LinkedList<PluginChannelListener>> pluginChannels = new HashMap<String, LinkedList<PluginChannelListener>>();
	
	/**
	 * List of mods which implement PluginChannelListener interface
	 */
	private LinkedList<PluginChannelListener> pluginChannelListeners = new LinkedList<PluginChannelListener>();
	
	/**
	 * Plugin channels that we know the server supports
	 */
	private Set<String> serverPluginChannels = new HashSet<String>();
	
	/**
	 * Keep track of faulting listeners so that we can periodically log a message if a listener is throwing LOTS of exceptions
	 */
	private Map<PluginChannelListener, Integer> faultingPluginChannelListeners = new HashMap<PluginChannelListener, Integer>();
	
	/**
	 * Package private
	 */
	PluginChannels()
	{
		PluginChannels.instance = this;
	}
	
	/**
	 * Get the current set of registered client-side channels
	 */
	public static Set<String> getLocalChannels()
	{
		return Collections.unmodifiableSet(PluginChannels.instance.pluginChannels.keySet());
	}
	
	/**
	 * Get the current set of registered server channels
	 */
	public static Set<String> getServerChannels()
	{
		return Collections.unmodifiableSet(PluginChannels.instance.serverPluginChannels);
	}
	
	/**
	 * Check whether a server plugin channel is registered
	 * 
	 * @param channel
	 * @return
	 */
	public static boolean isServerChannelRegistered(String channel)
	{
		return PluginChannels.instance.serverPluginChannels.contains(channel);
	}

	/**
	 * @param listener
	 */
	void addListener(LiteMod listener)
	{
		if (listener instanceof PluginChannelListener)
		{
			this.addPluginChannelListener((PluginChannelListener)listener);
		}
	}
	
	/**
	 * @param pluginChannelListener
	 */
	private void addPluginChannelListener(PluginChannelListener pluginChannelListener)
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
	void onPostLogin(INetHandlerLoginClient netHandler, S02PacketLoginSuccess loginPacket)
	{
		this.clearPluginChannels(netHandler);
	}

	/**
	 * @param netHandler
	 * @param loginPacket
	 */
	void onJoinGame(INetHandler netHandler, S01PacketJoinGame loginPacket)
	{
		this.sendRegisteredPluginChannels(netHandler);
	}
	
	/**
	 * Callback for the plugin channel hook
	 * 
	 * @param customPayload
	 */
	public void onPluginChannelMessage(S3FPacketCustomPayload customPayload)
	{
		if (customPayload != null && customPayload.getChannel() != null)
		{
			if (PluginChannels.CHANNEL_REGISTER.equals(customPayload.getChannel()))
			{
				this.onRegisterPacketReceived(customPayload);
			}
			else if (this.pluginChannels.containsKey(customPayload.getChannel()))
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
				
				this.onModPacketReceived(customPayload);
			}
		}
	}

	/**
	 * @param customPayload
	 */
	private void onRegisterPacketReceived(S3FPacketCustomPayload customPayload)
	{
		try
		{
			String channels = new String(customPayload.getData(), "UTF8");
			for (String channel : channels.split("\u0000"))
			{
				this.serverPluginChannels.add(channel);
			}
		}
		catch (UnsupportedEncodingException ex)
		{
			LiteLoader.getLogger().log(Level.WARNING, "Error decoding REGISTER packet from server " + ex.getClass().getSimpleName(), ex);
		}
	}

	/**
	 * @param customPayload
	 */
	private void onModPacketReceived(S3FPacketCustomPayload customPayload)
	{
		String channel = customPayload.getChannel();
		byte[] data = customPayload.getData();
		int length = data.length;
		
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
					LiteLoader.getLogger().warning(String.format("Plugin channel listener %s exceeded fault threshold on channel %s with %s", pluginChannelListener.getName(), channel, ex.getClass().getSimpleName()));
					this.faultingPluginChannelListeners.remove(pluginChannelListener);
				}
				else
				{
					this.faultingPluginChannelListeners.put(pluginChannelListener, Integer.valueOf(failCount));
				}
			}
		}
	}
	
	/**
	 * Connecting to a new server, clear plugin channels
	 * 
	 * @param netHandler
	 */
	private void clearPluginChannels(INetHandler netHandler)
	{
		this.pluginChannels.clear();
		this.serverPluginChannels.clear();
		this.faultingPluginChannelListeners.clear();
	}
	
	/**
	 * Query loaded mods for registered channels
	 */
	private void sendRegisteredPluginChannels(INetHandler netHandler)
	{
		try
		{
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
	public static boolean sendMessage(String channel, byte[] data, ChannelPolicy policy)
	{
		if (!policy.allows(channel))
		{
			if (policy.isSilent()) return false;
			throw new UnregisteredChannelException(channel);
		}
		
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
	
	/**
	 * Policy for dispatching plugin channel packets
	 *
	 * @author Adam Mummery-Smith
	 */
	public enum ChannelPolicy
	{
		/**
		 * Dispatch the message, throw an exception if the channel is not registered 
		 */
		DISPATCH,
		
		/**
		 * Dispatch the message, return false if the channel is not registered 
		 */
		DISPATCH_IF_REGISTERED,
		
		/**
		 * Dispatch the message 
		 */
		DISPATCH_ALWAYS;
		
		/**
		 * True if this policy allows outbound traffic on the specified channel
		 * 
		 * @param channel
		 * @return
		 */
		public boolean allows(String channel)
		{
			if (this == ChannelPolicy.DISPATCH_ALWAYS) return true;
			return PluginChannels.isServerChannelRegistered(channel);
		}
		
		/**
		 * True if this policy does not throw an exception for unregistered outbound channels
		 * 
		 * @return
		 */
		public boolean isSilent()
		{
			return (this != ChannelPolicy.DISPATCH_IF_REGISTERED);
		}
	}
}
