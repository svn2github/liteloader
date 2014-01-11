package com.mumfrey.liteloader.core.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;

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
		super(Obf.S3FPacketCustomPayload, Obf.InjectedCallbackProxy.name, "handleCustomPayloadPacket", 1000);
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
