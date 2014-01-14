package com.mumfrey.liteloader.core.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;

/**
 * Transformer for C17PacketCustomPayload
 *
 * @author Adam Mummery-Smith
 */
public class ServerCustomPayloadPacketTransformer extends PacketTransformer
{
	private static boolean injected = false;
	
	public ServerCustomPayloadPacketTransformer()
	{
		super(Obf.C17PacketCustomPayload, Obf.InjectedCallbackProxy.name, "handleCustomPayloadPacket", 1000);
	}

	@Override
	protected void notifyInjectionFailed()
	{
	}
	
	@Override
	protected void notifyInjected()
	{
		ServerCustomPayloadPacketTransformer.injected = true;
	}
	
	public static boolean isInjected()
	{
		return ServerCustomPayloadPacketTransformer.injected;
	}
}
