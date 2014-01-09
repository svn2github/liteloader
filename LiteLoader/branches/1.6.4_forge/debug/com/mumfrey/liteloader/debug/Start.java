package com.mumfrey.liteloader.debug;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import net.minecraft.launchwrapper.Launch;

import com.mumfrey.liteloader.launch.LiteLoaderTweaker;
import com.mumfrey.liteloader.util.log.LiteLoaderLogFormatter;

/**
 * Wrapper class for LaunchWrapper Main class, which logs into minecraft.net first so that online shizzle can be tested
 * 
 * @author Adam Mummery-Smith
 * @version 0.6
 */
public abstract class Start
{
	private static final String FML_TWEAKER_NAME = "cpw.mods.fml.common.launcher.FMLTweaker";
	private static Logger logger = Logger.getLogger("liteloader");
	
	/**
	 * Entry point.
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		List<String> argsList = new ArrayList<String>();
		
		boolean fmlDetected = false;
		for (String arg : args) fmlDetected |= FML_TWEAKER_NAME.equals(arg);
		if (fmlDetected)
		{
			args = new String[0];
			argsList.add("--tweakClass");argsList.add(FML_TWEAKER_NAME);
		}

		Start.prepareLogger(fmlDetected);
		
		String usernameFromCmdLine = (args.length > 0) ? args[0] : null;
		String passwordFromCmdLine = (args.length > 1) ? args[1] : null;
		
		File loginJson = new File(new File(System.getProperty("user.dir")), ".auth.json");
		LoginManager loginManager = new LoginManager(loginJson);
		loginManager.login(usernameFromCmdLine, passwordFromCmdLine, 5);

		Start.logger.info(String.format("Launching game as %s", loginManager.getProfileName()));
		
		File gameDir = new File(System.getProperty("user.dir"));
		File assetsDir = new File(gameDir, "assets");

		argsList.add("--tweakClass");  argsList.add(LiteLoaderTweaker.class.getName());
		argsList.add("--username");    argsList.add(loginManager.getProfileName());
//		argsList.add("--uuid");        argsList.add(loginManager.getUUID());
		argsList.add("--session");     argsList.add(loginManager.getAuthenticatedToken());
		argsList.add("--version");     argsList.add("mcp");
		argsList.add("--gameDir");     argsList.add(gameDir.getAbsolutePath());
		argsList.add("--assetsDir");   argsList.add(assetsDir.getAbsolutePath());
		
		Launch.main(argsList.toArray(args));
	}
	
	private static void prepareLogger(boolean fmlDetected)
	{
		if (!fmlDetected) System.setProperty("liteloaderFormatLog", "true");
		
		for (Handler handler : Start.logger.getParent().getHandlers())
		{
			if (handler instanceof ConsoleHandler)
				handler.setFormatter(new LiteLoaderLogFormatter(false));
		}
	}
}
