package com.mumfrey.liteloader.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 *
 * @author Adam Mummery-Smith
 */
public class LiteLoaderTweaker implements ITweaker
{
	private static final String VERSION = "1.6.2";

	private File gameDirectory;
	
	private File assetsDirectory;
	
	private String profile;

	private List<String> unClassifiedArgs = new ArrayList<String>();
	
	private Map<String, String> classifiedArgs = new HashMap<String, String>();
	
    private List<ITweaker> cascadedTweaks = new ArrayList<ITweaker>();
    private ArgumentAcceptingOptionSpec<String> cascadedTweaksOption, modsOption;
    private OptionSet parsedOptions;
    
    private boolean fmlIsPresent = false;

	private List<String> passThroughArgs;
	
	@Override
	public void acceptOptions(List<String> args, File gameDirectory, File assetsDirectory, String profile)
	{
		this.gameDirectory = gameDirectory;
		this.assetsDirectory = assetsDirectory;
		this.profile = profile;
		
		LiteLoaderTransformer.gameDirectory = gameDirectory;
		LiteLoaderTransformer.assetsDirectory = assetsDirectory;
		LiteLoaderTransformer.profile = profile;
		
        OptionParser optionParser = new OptionParser();
        this.cascadedTweaksOption = optionParser.accepts("cascadedTweaks", "Additional tweaks to be called by FML, implementing ITweaker").withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
        this.modsOption = optionParser.accepts("mods", "Comma-separated list of mods to load").withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
        optionParser.allowsUnrecognizedOptions();
        NonOptionArgumentSpec<String> nonOptions = optionParser.nonOptions();

        this.parsedOptions = optionParser.parse(args.toArray(new String[args.size()]));
        this.passThroughArgs = this.parsedOptions.valuesOf(nonOptions);
        
		// Parse out the arguments ourself because joptsimple doesn't really provide a good way to
		// add arguments to the unparsed argument list after parsing
		this.parseArgs(this.passThroughArgs);
		
		if (!this.classifiedArgs.containsKey("--version"))
			this.addClassifiedArg("--version", this.VERSION);
		
		if (!this.classifiedArgs.containsKey("--gameDir") && gameDirectory != null)
			this.addClassifiedArg("--gameDir", gameDirectory.getAbsolutePath());
		
		if (!this.classifiedArgs.containsKey("--assetsDir") && assetsDirectory != null)
			this.addClassifiedArg("--assetsDir", assetsDirectory.getAbsolutePath());
		
		if (this.parsedOptions.has(this.modsOption))
		{
			LiteLoaderTransformer.modsToLoad = this.modsOption.values(this.parsedOptions);
		}
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
		this.computeCascadedTweaks(classLoader);
		LiteLoaderTransformer.launchClassLoader = classLoader;
		classLoader.registerTransformer("com.mumfrey.liteloader.launch.LiteLoaderTransformer");
		this.runAdditionalTweaks(classLoader);
	}

	// Shamelessly stolen from FML
    void computeCascadedTweaks(LaunchClassLoader classLoader)
    {
        if (this.parsedOptions.has(this.cascadedTweaksOption))
        {
            for (String tweaker : this.cascadedTweaksOption.values(this.parsedOptions))
            {
                try
                {
                    classLoader.addClassLoaderExclusion(tweaker.substring(0, tweaker.lastIndexOf('.')));
                    @SuppressWarnings("unchecked")
					Class<? extends ITweaker> tweakClass = (Class<? extends ITweaker>) Class.forName(tweaker, true, classLoader);
                    ITweaker additionalTweak = tweakClass.newInstance();
                    this.cascadedTweaks.add(additionalTweak);
                    
                    if ("cpw.mods.fml.common.launcher.FMLTweaker".equals(tweaker))
                    {
                    	this.fmlIsPresent = true;
                    }
                }
                catch (Exception e)
                {
                    Logger.getLogger("liteloader").info(String.format("Missing additional tweak class %s", tweaker));
                }
            }
        }
    }
    
    void runAdditionalTweaks(LaunchClassLoader classLoader)
    {
        List<String> cascadedArgs = new ArrayList<String>(this.passThroughArgs);
        if (this.fmlIsPresent) cascadedArgs.add("--fmlIsPresent");
        for (ITweaker tweak : this.cascadedTweaks)
        {
            tweak.acceptOptions(cascadedArgs, this.gameDirectory, this.assetsDirectory, this.profile);
            tweak.injectIntoClassLoader(classLoader);
        }
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
