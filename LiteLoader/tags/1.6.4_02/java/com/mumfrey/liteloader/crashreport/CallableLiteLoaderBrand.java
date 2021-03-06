package com.mumfrey.liteloader.crashreport;

import java.util.concurrent.Callable;

import com.mumfrey.liteloader.core.LiteLoader;

import net.minecraft.src.CrashReport;

public class CallableLiteLoaderBrand implements Callable<String>
{
	final CrashReport crashReport;
	
	public CallableLiteLoaderBrand(CrashReport report)
	{
		this.crashReport = report;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public String call() throws Exception
	{
		String brand = LiteLoader.getInstance().getBranding();
		return brand == null ? "Unknown / None" : brand;
	}
}
