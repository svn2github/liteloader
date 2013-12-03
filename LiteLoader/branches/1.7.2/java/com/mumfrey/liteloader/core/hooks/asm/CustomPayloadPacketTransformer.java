package com.mumfrey.liteloader.core.hooks.asm;

/**
 * Transformer for S3FPacketCustomPayload
 *
 * @author Adam Mummery-Smith
 */
public class CustomPayloadPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public CustomPayloadPacketTransformer()
	{
		// TODO Obfuscation 1.7.2
		super("net.minecraft.network.play.server.S3FPacketCustomPayload", "gi", "com.mumfrey.liteloader.core.hooks.asm.ASMHookProxy", "handleCustomPayloadPacket", 1000);
	}

	@Override
	protected void notifyInjectionFailed()
	{
	}
	
	@Override
	protected void notifyInjected()
	{
		CustomPayloadPacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return CustomPayloadPacketTransformer.injected;
	}
}
