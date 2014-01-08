package com.mumfrey.liteloader.launch;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mumfrey.liteloader.launch.InjectionStrategy.InjectionPosition;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import sun.misc.URLClassPath;

/**
 * Nasty horrible reflection hacks to do nasty things with the classpath
 * 
 * @author Adam Mummery-Smith
 */
public abstract class ClassPathUtilities
{
	private static Logger logger = Logger.getLogger("liteloader");
	
	/**
	 * URLClassLoader::ucp -> instance of URLClassPath
	 */
	private static Field ucp;
	
	/**
	 * URLClassPath::urls -> instance of Stack<URL> 
	 */
	private static Field classPathURLs;
	
	/**
	 * URLClassPath::path -> instance of ArrayList<URL> 
	 */
	private static Field classPathPath;

	/**
	 * URLClassPath::lmap -> instance of HashMap<String, URLClassPath.Loader> 
	 */
	private static Field classPathLoaderMap;

	/**
	 * URLClassPath::loaders -> instance of ArrayList<URLClassPath.Loader> 
	 */
	private static Field classPathLoaderList;
	
	private static boolean canInject;
	
	static
	{
		try
		{
			ClassPathUtilities.ucp = URLClassLoader.class.getDeclaredField("ucp");
			ClassPathUtilities.ucp.setAccessible(true);
			
			ClassPathUtilities.classPathURLs = URLClassPath.class.getDeclaredField("urls");
			ClassPathUtilities.classPathURLs.setAccessible(true);
			ClassPathUtilities.classPathPath = URLClassPath.class.getDeclaredField("path");
			ClassPathUtilities.classPathPath.setAccessible(true);
			ClassPathUtilities.classPathLoaderMap = URLClassPath.class.getDeclaredField("lmap");
			ClassPathUtilities.classPathLoaderMap.setAccessible(true);
			ClassPathUtilities.classPathLoaderList = URLClassPath.class.getDeclaredField("loaders");
			ClassPathUtilities.classPathLoaderList.setAccessible(true);
			ClassPathUtilities.canInject = true;
		}
		catch (Throwable th)
		{
			ClassPathUtilities.logger.log(Level.SEVERE, "ClassPathUtilities: Error initialising ClassPathUtilities, special class path injection disabled", th);
			th.printStackTrace();
		}
	}
	
	/**
	 * Injects a URL into the classpath based on the specified injection strategy
	 * 
	 * @param classLoader
	 * @param url
	 */
	public static void injectIntoClassPath(URLClassLoader classLoader, URL url, InjectionStrategy strategy)
	{
		if (strategy == null || strategy.getPosition() == null)
		{
			ClassPathUtilities.addURL(classLoader, url);
			return;
		}
		
		if (strategy.getPosition() == InjectionPosition.Top)
		{
			ClassPathUtilities.injectIntoClassPath(classLoader, url);
		}
		else if (strategy.getPosition() == InjectionPosition.Base)
		{
			ClassPathUtilities.injectIntoClassPath(classLoader, url, LiteLoaderTweaker.getJarUrl());
		}
		else if (strategy.getPosition() == InjectionPosition.Above)
		{
			String[] params = strategy.getParams();
			if (params.length > 0)
			{
				ClassPathUtilities.injectIntoClassPath(classLoader, url, params[0]);
			}
		}
		else
		{
			ClassPathUtilities.addURL(classLoader, url);
		}
	}
	
	/**
	 * Injects a URL into the classpath at the TOP of the stack
	 * 
	 * @param classLoader
	 * @param url
	 */
	public static void injectIntoClassPath(URLClassLoader classLoader, URL url)
	{
		ClassPathUtilities.injectIntoClassPath(classLoader, url, (URL)null);
	}
	
	/**
	 * Injects a URL into the classpath at the TOP of the stack
	 * 
	 * @param classLoader
	 * @param url
	 * @param above
	 */
	@SuppressWarnings({ "unchecked" })
	public static void injectIntoClassPath(URLClassLoader classLoader, URL url, URL above)
	{
		if (ClassPathUtilities.canInject)
		{
			ClassPathUtilities.logger.info(String.format("ClassPathUtilities: attempting to inject %s into %s", url, classLoader.getClass().getSimpleName()));
			
			try
			{
				URLClassPath classPath = (URLClassPath)ClassPathUtilities.ucp.get(classLoader);
				
				Stack<URL> urls = (Stack<URL>)ClassPathUtilities.classPathURLs.get(classPath);
				ArrayList<URL> path = (ArrayList<URL>)ClassPathUtilities.classPathPath.get(classPath);
				
				synchronized (urls)
				{
					if (!path.contains(url))
					{
						urls.add(url);
						
						if (above == null)
						{
							path.add(0, url);
						}
						else
						{
							for (int pos = path.size() - 1; pos > 0; pos--)
							{
								if (above.equals(path.get(pos)))
									path.add(pos, url);
							}
						}
					}
				}
			}
			catch (Exception ex)
			{
				ClassPathUtilities.logger.warning(String.format("ClassPathUtilities: failed to inject %s", url));
			}
		}
		
		ClassPathUtilities.addURL(classLoader, url);
	}

	/**
	 * @param classLoader
	 * @param url
	 * @param above
	 */
	public static void injectIntoClassPath(URLClassLoader classLoader, URL url, String above)
	{
		above = above.trim().toLowerCase();
		if (above.length() < 1) return;
		
		for (URL classPathUrl : classLoader.getURLs())
		{
			if (classPathUrl.toString().toLowerCase().contains(above))
			{
				ClassPathUtilities.injectIntoClassPath(classLoader, url, classPathUrl);
				return;
			}
		}
	}

	/**
	 * @param classLoader
	 * @param url
	 */
	public static void addURL(URLClassLoader classLoader, URL url)
	{
		if (classLoader instanceof LaunchClassLoader)
		{
			((LaunchClassLoader)classLoader).addURL(url);
		}
		else
		{
			try
			{
				Method mAddUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
				mAddUrl.setAccessible(true);
				mAddUrl.invoke(classLoader, url);
			}
			catch (Exception ex) {}
		}
	}

	/**
	 * Gets the file containing the specified resource
	 * 
	 * @param contextClass
	 * @param resource
	 * @return
	 */
	public static File getPathToResource(Class<?> contextClass, String resource)
	{
		URL res = contextClass.getResource(resource);
		if (res == null) return null;

		boolean returnParent = true;
		String jarPath = res.toString();
		if (jarPath.startsWith("jar:") && jarPath.indexOf('!') > -1)
		{
			jarPath = jarPath.substring(4, jarPath.indexOf('!'));
			returnParent = false;
		}
		
		if (jarPath.startsWith("file:"))
		{
			try
			{
				File targetFile = new File(new URI(jarPath));
				return returnParent ? targetFile.getParentFile() : targetFile;
			}
			catch (URISyntaxException ex) { }
		}
		
		return null;
	}

	/**
	 * @param contextClass
	 * @param resource
	 * @return
	 */
	public static boolean deleteClassPathJarContaining(Class<?> contextClass, String resource)
	{
		File jarFile = ClassPathUtilities.getPathToResource(contextClass, resource);
		if (jarFile != null && jarFile.exists() && jarFile.isFile() && jarFile.getName().endsWith(".jar"))
		{
			return ClassPathUtilities.deleteClassPathJar(jarFile.getName());
		}
		
		return false;
	}
	
	/**
	 * @param jarFileName
	 * @return
	 */
	public static boolean deleteClassPathJar(String jarFileName)
	{
		try
		{
			// First try to find the jar reference in the class loaders
			JarFile jar = ClassPathUtilities.getJarFromClassLoader(Launch.classLoader, jarFileName, false);
			JarFile parentJar = ClassPathUtilities.getJarFromClassLoader((URLClassLoader)Launch.class.getClassLoader(), jarFileName, false);

			if (jar != null && parentJar != null && jar.getName().equals(parentJar.getName()))
			{
				final JarFile jarInClassLoader = ClassPathUtilities.getJarFromClassLoader(Launch.classLoader, jarFileName, true);
				final JarFile jarInParentClassLoader = ClassPathUtilities.getJarFromClassLoader((URLClassLoader)Launch.class.getClassLoader(), jarFileName, true);

				final File jarFileInClassLoader = new File(jarInClassLoader.getName());
				final File jarFileInParentClassLoader = new File(jarInParentClassLoader.getName());

				try
				{
					Boolean deleted = AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>()
					{
						@Override
						public Boolean run() throws Exception
						{
							jarInClassLoader.close();
							jarInParentClassLoader.close();
						
							boolean deletedJarFile = jarFileInClassLoader.delete();
							boolean deletedParentJarFile = jarFileInParentClassLoader.delete();
							
							return Boolean.valueOf(deletedJarFile || deletedParentJarFile);
						}
					});
					
					return deleted;
				}
				catch (PrivilegedActionException ex)
				{
					ex.printStackTrace();
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		return false;
	}

	/**
	 * @param classLoader
	 * @param fileName
	 * @param removeFromClassPath
	 * @return
	 * @throws MalformedURLException
	 */
	@SuppressWarnings("unchecked")
	private static JarFile getJarFromClassLoader(URLClassLoader classLoader, String fileName, boolean removeFromClassPath) throws MalformedURLException
	{
		JarFile jar = null;
		
		try
		{
			URLClassPath classPath = (URLClassPath)ClassPathUtilities.ucp.get(classLoader);
			Map<String, ?> loaderMap = (Map<String, ?>)ClassPathUtilities.classPathLoaderMap.get(classPath);
			
			for (Entry<String, ?> loaderEntry : loaderMap.entrySet())
			{
				String url = loaderEntry.getKey();
				
				if (url.endsWith(fileName))
				{
					Object loader = loaderEntry.getValue();
					Field jarField = loader.getClass().getDeclaredField("jar");
					jarField.setAccessible(true);
					
					jar = (JarFile)jarField.get(loader);
					
					if (removeFromClassPath)
					{
						jarField.set(loader, null);
						
						Stack<URL> urls = (Stack<URL>)ClassPathUtilities.classPathURLs.get(classPath);
						ArrayList<URL> path = (ArrayList<URL>)ClassPathUtilities.classPathPath.get(classPath);
						ArrayList<?> loaders = (ArrayList<?>)ClassPathUtilities.classPathLoaderList.get(classPath);
						
						loaders.remove(loader);
						loaderMap.remove(url);
						
						URL jarURL = new URL(url);
						urls.remove(jarURL);
						path.remove(jarURL);
					}
				}
			}
		}
		catch (IllegalArgumentException ex) {}
		catch (SecurityException ex) {}
		catch (IllegalAccessException ex) {}
		catch (NoSuchFieldException ex)
		{
			ex.printStackTrace();
		}
		
		return jar;
	}
}
