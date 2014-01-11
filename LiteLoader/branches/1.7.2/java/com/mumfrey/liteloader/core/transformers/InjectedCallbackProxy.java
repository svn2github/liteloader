package com.mumfrey.liteloader.core.transformers;

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
public class InjectedCallbackProxy
{
	/**
	 * Initialisation done
	 */
	private static boolean initDone = false;
	
	/**
	 * Tick clock, sent as a flag to the core onTick so that mods know it's a new tick
	 */
	private static boolean clock = false;
	
	private static Events events;
	
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
	
	public static void onTimerUpdate(int ref)
	{
		if (!InjectedCallbackProxy.initDone)
		{
			InjectedCallbackProxy.initDone = true;
			InjectedCallbackProxy.events = LiteLoader.getEvents();
			InjectedCallbackProxy.events.preBeginGame();
		}
		
		InjectedCallbackProxy.events.onTimerUpdate();
	}
	
	public static void onAnimateTick(int ref)
	{
		InjectedCallbackProxy.clock = true;
	}
	
	public static void onTick(int ref)
	{
		if (ref == 2)
		{
			InjectedCallbackProxy.events.onTick(InjectedCallbackProxy.clock);
		}
	}
	
	public static void onRender(int ref)
	{
		InjectedCallbackProxy.events.onRender();
	}
	
	public static void preRenderGUI(int ref)
	{
		if (ref == 1)
		{
			InjectedCallbackProxy.events.preRenderGUI();
		}
	}
	
	public static void onSetupCameraTransform(int ref)
	{
		InjectedCallbackProxy.events.onSetupCameraTransform();
	}
	
	public static void postRenderEntities(int ref)
	{
		InjectedCallbackProxy.events.postRenderEntities();
	}
	
	public static void postRender(int ref)
	{
		InjectedCallbackProxy.events.postRender();
	}
	
	public static void onRenderHUD(int ref)
	{
		InjectedCallbackProxy.events.onRenderHUD();
	}
	
	public static void onRenderChat(int ref)
	{
		InjectedCallbackProxy.events.onRenderChat();
	}
	
	public static void postRenderChat(int ref)
	{
		if (ref == 10)
		{
			InjectedCallbackProxy.events.postRenderChat();
		}
	}
	
	public static void postRenderHUDandGUI(int ref)
	{
		if (ref == 2)
		{
			InjectedCallbackProxy.events.postRenderHUD();
			InjectedCallbackProxy.events.preRenderGUI();
		}
	}
}
