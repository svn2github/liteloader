package com.mumfrey.liteloader.core.hooks.asm;

import net.minecraft.network.INetHandler;
import net.minecraft.network.login.INetHandlerLoginClient;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S3FPacketCustomPayload;

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
public class ASMHookProxy
{
	public static void handleLoginSuccessPacket(INetHandler netHandler, S02PacketLoginSuccess packet)
	{
		((INetHandlerLoginClient)netHandler).func_147390_a(packet);
		LiteLoader.getEvents().onPostLogin((INetHandlerLoginClient)netHandler, packet);
	}
	
	/**
	 * S02PacketChat::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleChatPacket(INetHandler netHandler, S02PacketChat packet)
	{
		Events events = LiteLoader.getEvents();
		if (events.onChat(packet))
		{
			((INetHandlerPlayClient)netHandler).func_147251_a(packet);
		}
	}
	
	/**
	 * S01PacketJoinGame::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleJoinGamePacket(INetHandler netHandler, S01PacketJoinGame packet)
	{
		Events events = LiteLoader.getEvents();
		if (events.onPreJoinGame(netHandler, packet))
		{
			((INetHandlerPlayClient)netHandler).func_147282_a(packet);
			events.onJoinGame(netHandler, packet);
		}
	}
	
	/**
	 * S3FPacketCustomPayload::processPacket()
	 * 
	 * @param netHandler
	 * @param packet
	 */
	public static void handleCustomPayloadPacket(INetHandler netHandler, S3FPacketCustomPayload packet)
	{
		((INetHandlerPlayClient)netHandler).func_147240_a(packet);;
		
		PluginChannels pluginChannels = LiteLoader.getPluginChannels();
		pluginChannels.onPluginChannelMessage(packet);
	}
}
