package com.mumfrey.liteloader.core.hooks.asm;

/**
 * Transformer for S02PacketLoginSuccess
 *
 * @author Adam Mummery-Smith
 */
public class LoginSuccessPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public LoginSuccessPacketTransformer()
	{
		// TODO Obfuscation 1.7.2
		super("net.minecraft.network.login.server.S02PacketLoginSuccess", "jg", "com.mumfrey.liteloader.core.hooks.asm.ASMHookProxy", "handleLoginSuccessPacket", 1000);
	}
	
	@Override
	protected void notifyInjectionFailed()
	{
	}
	
	@Override
	protected void notifyInjected()
	{
		LoginSuccessPacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return LoginSuccessPacketTransformer.injected;
	}
}