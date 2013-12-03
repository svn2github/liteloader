package com.mumfrey.liteloader.core.hooks.asm;

/**
 * Transformer for S02PacketChat
 *
 * @author Adam Mummery-Smith
 */
public class ChatPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public ChatPacketTransformer()
	{
		// TODO Obfuscation 1.7.2
		super("net.minecraft.network.play.server.S02PacketChat", "ga", "com.mumfrey.liteloader.core.hooks.asm.ASMHookProxy", "handleChatPacket", 1000);
	}

	@Override
	protected void notifyInjectionFailed()
	{
	}
	
	@Override
	protected void notifyInjected()
	{
		ChatPacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return ChatPacketTransformer.injected;
	}
}
