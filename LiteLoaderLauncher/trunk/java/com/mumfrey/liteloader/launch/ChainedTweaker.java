package com.mumfrey.liteloader.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 *
 * @author Adam Mummery-Smith
 */
public abstract class ChainedTweaker implements ITweaker
{
	protected ITweaker chainedTweaker = null;
	
	protected String chainedTweakArg = null;
	
	@Override
	public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile)
	{
		List<String> chainedArgs = new ArrayList<String>(args); 
		String chainedTweakArg = this.initChainedTweaker(chainedArgs);
		
		if (chainedTweakArg != null)
		{
			chainedArgs.add("--chainedTweakers");
			chainedArgs.add(chainedTweakArg);
		}
		
		if (this.chainedTweaker != null)
		{
			this.chainedTweaker.acceptOptions(chainedArgs, gameDir, assetsDir, profile);
		}
	}
	
	/**
	 * Pop chained tweaker class names from the launcher args until we find a valid one and assign it as 
	 * our chained tweak class. If there are remaining classes in the chain spec then return a csv list
	 * otherwise return null to indicate no further tweakers remain
	 * 
	 * @param args
	 * @return
	 */
	private String initChainedTweaker(List<String> args)
	{
		List<String> chainedTweakers = this.parseChainedTweakers(args);
		
		while (chainedTweakers.size() > 0)
		{
			String nextChainedTweakClassName = chainedTweakers.remove(0);

			try
			{
				System.out.println("Attempting to load chained tweak class " + nextChainedTweakClassName);
				this.chainedTweaker = (ITweaker)Class.forName(nextChainedTweakClassName).newInstance();
				return implode(',', chainedTweakers);
			}
			catch (Throwable th)
			{
				// TODO possibly log failure
			}
		}
		
		return null;
	}

	private List<String> parseChainedTweakers(List<String> args)
	{
        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        OptionSpec<String> chainedTweakers = parser.accepts("chainedTweakers", "Chained tweak classes to try to load").withRequiredArg().withValuesSeparatedBy(',');
        OptionSet options = parser.parse(args.toArray(new String[0]));
        
        // Remove chainedTweakers opt from args and populate with remaining (unparsed) args 
        args.clear();
        args.addAll(options.valuesOf(parser.nonOptions()));
        
        return new ArrayList<String>(options.valuesOf(chainedTweakers));
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
	
	protected static String implode(char separator, List<String> list)
	{
		if (list == null || list.size() < 1) return null;
		StringBuilder sb = new StringBuilder().append(list.remove(0));
		for(String string : list) sb.append(separator).append(string);
		return sb.toString();
	}
}