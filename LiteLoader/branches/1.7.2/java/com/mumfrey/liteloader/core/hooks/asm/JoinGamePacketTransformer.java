package com.mumfrey.liteloader.core.hooks.asm;

/**
 * Transformer for S01PacketJoinGame
 *
 * @author Adam Mummery-Smith
 */
public class JoinGamePacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public JoinGamePacketTransformer()
	{
		// TODO Obfuscation 1.7.2
		super("net.minecraft.network.play.server.S01PacketJoinGame", "gu", "com.mumfrey.liteloader.core.hooks.asm.ASMHookProxy", "handleJoinGamePacket", 1000);
	}
	
	@Override
	protected void notifyInjectionFailed()
	{
	}
	
	@Override
	protected void notifyInjected()
	{
		JoinGamePacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return JoinGamePacketTransformer.injected;
	}
}