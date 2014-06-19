package com.mumfrey.liteloader.client.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.event.Event;
import com.mumfrey.liteloader.transformers.event.EventInjectionTransformer;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.transformers.event.MethodInfo;
import com.mumfrey.liteloader.transformers.event.inject.MethodHead;

public class LiteLoaderEventInjectionTransformer extends EventInjectionTransformer
{
	@Override
	protected void addEvents()
	{
		Event runGameLoop = Event.getOrCreate("runGameLoop", true);
		MethodInfo target = new MethodInfo(Obf.Minecraft, Obf.runGameLoop, Void.TYPE);
		InjectionPoint point = new MethodHead();
		
		this.addEvent(runGameLoop, target, point).addListener(new MethodInfo(Obf.CallbackProxyClient));
	}
}
