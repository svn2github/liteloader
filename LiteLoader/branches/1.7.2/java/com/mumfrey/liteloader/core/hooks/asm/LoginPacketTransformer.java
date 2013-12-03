package com.mumfrey.liteloader.core.hooks.asm;

/**
 * Transformer for S01PacketJoinGame
 *
 * @author Adam Mummery-Smith
 */
public class LoginPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public LoginPacketTransformer()
	{
		// TODO Obfuscation 1.7.2
		super("net.minecraft.network.play.server.S01PacketJoinGame", "gu", "com.mumfrey.liteloader.core.hooks.asm.ASMHookProxy", "handleLoginPacket", 1000);
	}
	
	@Override
	protected void notifyInjectionFailed()
	{
	}
	
	@Override
	protected void notifyInjected()
	{
		LoginPacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return LoginPacketTransformer.injected;
	}
}