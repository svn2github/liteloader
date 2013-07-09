package com.mumfrey.liteloader.launch;

import java.io.File;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.launchwrapper.IClassTransformer;

//import com.mumfrey.liteloader.core.LiteLoader;

public class LiteLoaderTransformer implements IClassTransformer
{
	private static final String classMappingRenderLightningBolt = "bgp";

	private static Logger logger = Logger.getLogger("liteloader");
	
	public static File gameDirectory;
	
	public static File assetsDirectory;
	
	public static String profile;

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
        if (classMappingRenderLightningBolt.equals(name))
        {
			logger.info("Beginning LiteLoader Init...");

			try
        	{
        		Class<?> loaderClass = Class.forName("com.mumfrey.liteloader.core.LiteLoader", false, LiteLoaderTweaker.launchClassLoader);
        		Method mInit = loaderClass.getDeclaredMethod("init", File.class, File.class, String.class);
        		mInit.invoke(null, LiteLoaderTransformer.gameDirectory, LiteLoaderTransformer.assetsDirectory, LiteLoaderTransformer.profile);
			}
        	catch (Throwable th)
        	{
        		logger.log(Level.SEVERE, String.format("Error initialising LiteLoader: %s", th.getMessage()), th);
			}
        }
        
        return basicClass;
	}
}
