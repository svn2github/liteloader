package com.mumfrey.liteloader.launch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.minecraft.launchwrapper.IClassTransformer;

import com.mumfrey.liteloader.core.LiteLoader;

public class LiteLoaderTransformer implements IClassTransformer
{
	private static final String classMappingCallableJVMFlags = "h";

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
				LiteLoader.init(LiteLoaderTransformer.gameDirectory, LiteLoaderTransformer.assetsDirectory, LiteLoaderTransformer.profile);
			}
        	catch (Throwable th)
        	{
        		logger.log(Level.SEVERE, String.format("Error initialising LiteLoader: %s", th.getMessage()), th);
			}
        }
        
        if (classMappingCallableJVMFlags.equals(name))
        {
        	return this.injectCallableJVMFlags("CallableJVMFlags", classMappingCallableJVMFlags, basicClass);
        }
        
        return basicClass;
	}

	/**
	 * Inject the only base class we override, which is CallableJVMFlags just to hook the crash log
	 * 
	 * @param binaryClassName
	 * @param fileName
	 * @param basicClass
	 * @return
	 */
	private byte[] injectCallableJVMFlags(String binaryClassName, String fileName, byte[] basicClass)
	{
		try
		{
			InputStream resourceInputStream = LiteLoader.class.getResourceAsStream("/classes/" + fileName + ".bin");
			
			if (resourceInputStream != null)
			{
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				
				for (int readBytes = resourceInputStream.read(); readBytes >= 0; readBytes = resourceInputStream.read())
				{
					outputStream.write(readBytes);
				}
			
				byte[] data = outputStream.toByteArray();

				outputStream.close();
				resourceInputStream.close();
				
				logger.info("Defining class override for " + binaryClassName);
				return data;
			}
			
			logger.info("Error defining class override for " + binaryClassName + ", file not found");
		}
		catch (Throwable th)
		{
			logger.log(Level.WARNING, "Error defining class override for " + binaryClassName, th);
		}
		
		return basicClass;
	}
}
