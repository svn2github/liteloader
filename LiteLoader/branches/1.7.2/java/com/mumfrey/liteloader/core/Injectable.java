package com.mumfrey.liteloader.core;

import java.net.MalformedURLException;

import net.minecraft.launchwrapper.LaunchClassLoader;

public interface Injectable
{
	public abstract boolean isInjected();
	
	public abstract boolean injectIntoClassPath(LaunchClassLoader classLoader, boolean injectIntoParent) throws MalformedURLException;
}
