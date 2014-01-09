package com.mumfrey.liteloader.core.hooks.asm;

import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.network.packet.Packet3Chat;

import com.mumfrey.liteloader.core.Events;
import com.mumfrey.liteloader.core.LiteLoader;
import com.mumfrey.liteloader.core.PluginChannels;

/**
 * Proxy class which handles the redirected calls from the injected packet hooks and routes them to the
 * relevant liteloader handler classes. We do this rather than patching a bunch of bytecode into the packet
 * classes themselves because this is easier to maintain.
 * 
 * @author Adam Mummery-Smith
 */
public abstract class ASMHookProxy
{
	private ASMHookProxy() {}
	
	/**
	 * Packet3Chat::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleChatPacket(NetHandler netHandler, Packet3Chat packet)
	{
		Events events = LiteLoader.getEvents();
		if (events.onChat(packet))
		{
			netHandler.handleChat(packet);
		}
	}
	
	/**
	 * Packet3Chat::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleLoginPacket(NetHandler netHandler, Packet1Login packet)
	{
		Events events = LiteLoader.getEvents();
		if (events.onPreLogin(netHandler, packet))
		{
			netHandler.handleLogin(packet);
			events.onConnectToServer(netHandler, packet);
		}
	}
	
	/**
	 * Packet3Chat::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleCustomPayloadPacket(NetHandler netHandler, Packet250CustomPayload packet)
	{
		netHandler.handleCustomPayload(packet);
		
		PluginChannels pluginChannels = LiteLoader.getPluginChannels();
		pluginChannels.onPluginChannelMessage(packet);
	}
}
