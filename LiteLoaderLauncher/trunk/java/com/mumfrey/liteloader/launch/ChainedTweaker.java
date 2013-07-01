package com.mumfrey.liteloader.launch;

import java.io.File;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 *
 * @author Adam Mummery-Smith
 */
public abstract class ChainedTweaker implements ITweaker
{
	private Class<? extends ITweaker> chainedTweakerClass;
	
	private ITweaker chainedTweaker;
	
	@SuppressWarnings("unchecked")
	public ChainedTweaker(String chainedTweakerClassName)
	{
		if (chainedTweakerClassName != null && !chainedTweakerClassName.isEmpty())
		{
			try
			{
				this.chainedTweakerClass = (Class<? extends ITweaker>) Class.forName(chainedTweakerClassName);
				this.chainedTweaker = this.chainedTweakerClass.newInstance();
			}
			catch (Throwable th)
			{
				// Log an error maybe?
			}
		}
	}
	
	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile)
	{
		if (this.chainedTweaker != null)
		{
			this.chainedTweaker.acceptOptions(args, gameDir, assetsDir, profile);
		}
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader)
	{
		if (this.chainedTweaker != null)
		{
			this.chainedTweaker.injectIntoClassLoader(classLoader);
		}
	}
	
	public ITweaker getChainedTweaker()
	{
		return this.chainedTweaker;
	}
}