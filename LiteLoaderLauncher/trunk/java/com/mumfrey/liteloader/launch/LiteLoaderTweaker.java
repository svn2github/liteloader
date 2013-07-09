package com.mumfrey.liteloader.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderTweaker extends ChainedTweaker
{
	private static final String VERSION = "1.6.2";

	public static LaunchClassLoader launchClassLoader;

	private File gameDirectory;
	
	private File assetsDirectory;
	
	private String profile;

	private List<String> unClassifiedArgs = new ArrayList<String>();
	
	private Map<String, String> classifiedArgs = new HashMap<String, String>();
	
	@Override
	public void acceptOptions(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		// Parse out the arguments ourself because joptsimple doesn't really provide a good way to
		// reconstruct an argument list after parsing
		this.parseArgs(args);
		
		if (!this.classifiedArgs.containsKey("--version"))
			this.addClassifiedArg("--version", this.VERSION);
		
		if (!this.classifiedArgs.containsKey("--gameDir") && gameDirectory != null)
			this.addClassifiedArg("--gameDir", gameDirectory.getAbsolutePath());
		
		if (!this.classifiedArgs.containsKey("--assetsDir") && assetsDirectory != null)
			this.addClassifiedArg("--assetsDir", assetsDirectory.getAbsolutePath());
		
		this.gameDirectory = gameDirectory;
		this.assetsDirectory = assetsDirectory;
		this.profile = profile;
		
		LiteLoaderTransformer.gameDirectory = gameDirectory;
		LiteLoaderTransformer.assetsDirectory = assetsDirectory;
		LiteLoaderTransformer.profile = profile;

		// Called last to initialise chained tweakers
		super.acceptOptions(args, gameDirectory, assetsDirectory, profile);
	}

	private void parseArgs(List<String> args)
	{
		String classifier = null;
		
		for (String arg : args)
		{
			if (arg.startsWith("-"))
			{
				if (classifier != null)
					classifier = this.addClassifiedArg(classifier, "");
				
				classifier = arg;
			}
			else
			{
				if (classifier != null)
					classifier = this.addClassifiedArg(classifier, arg);
				else
					this.unClassifiedArgs.add(arg);
			}
		}
	}

	private String addClassifiedArg(String classifiedArg, String arg)
	{
		this.classifiedArgs.put(classifiedArg, arg);
		return null;
	}

	@Override
	public void injectIntoClassLoader(LaunchClassLoader classLoader)
	{
		LiteLoaderTweaker.launchClassLoader = classLoader;
		classLoader.registerTransformer("com.mumfrey.liteloader.launch.LiteLoaderTransformer");
	}

	@Override
	public String getLaunchTarget()
	{
		return "net.minecraft.client.main.Main";
	}

	@Override
	public String[] getLaunchArguments()
	{
		List<String> args = new ArrayList<String>();
		
		for (Entry<String, String> classifiedArg : this.classifiedArgs.entrySet())
		{
			args.add(classifiedArg.getKey().trim());
			args.add(classifiedArg.getValue().trim());
		}
		
		for (String unClassifiedArg : this.unClassifiedArgs)
			args.add(unClassifiedArg);
		
		return args.toArray(new String[args.size()]);
	}

	public File getGameDirectory()
	{
		return this.gameDirectory;
	}

	public File getAssetsDirectory()
	{
		return this.assetsDirectory;
	}
	
	public String getProfile()
	{
		return this.profile;
	}
}
