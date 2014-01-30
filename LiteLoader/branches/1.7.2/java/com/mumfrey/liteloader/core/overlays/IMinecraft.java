package com.mumfrey.liteloader.core.overlays;

import java.util.List;

import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.Timer;

public interface IMinecraft
{
	public abstract Timer getTimer();

	public abstract boolean isRunning();

	public abstract List<IResourcePack> getDefaultResourcePacks();

	public abstract void setSize(int width, int height);
}
