package com.mumfrey.liteloader.transformers.event.inject;

import java.util.Collection;
import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

import com.mumfrey.liteloader.transformers.event.Event;
import com.mumfrey.liteloader.transformers.event.InjectionPoint;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * An injection point which searches for method invokations matching its arguments and returns a list of insns immediately
 * prior to matching invokations. Only the method name is required, owners and signatures are optional and can be used to disambiguate
 * between methods of the same name but with different args, or belonging to different classes.
 * 
 * @author Adam Mummery-Smith
 */
public class BeforeInvoke extends InjectionPoint
{
	/**
	 * Method name(s) to search for, usually this will contain the different names of the method for different obfuscations (mcp, srg, notch)
	 */
	private final String[] methodNames;
	
	/**
	 * Method owner(s) to search for, the values in this array MUST much the equivalent indices in methodNames, if the array is NULL then
	 * all owners are valid  
	 */
	private final String[] methodOwners;
	
	/**
	 * Method signature(s) to search for, the values in this array MUST much the equivalent indices in methodNames, if the array is NULL
	 * then all signatures are valid  
	 */
	private final String[] methodSignatures;
	
	/**
	 * This strategy can be used to identify a particular invokation if the same method is invoked at multiple points, if this value is -1
	 * then the strategy returns ALL invokations of the method. 
	 */
	private final int ordinal;
	
	/**
	 * True to turn on strategy debugging to the console
	 */
	private boolean logging = false;
	
	/**
	 * Match all occurrences of the specified method or methods
	 * 
	 * @param methodNames Method name(s) to search for
	 */
	public BeforeInvoke(String... methodNames)
	{
		this(methodNames, null, -1);
	}
	
	/**
	 * Match the specified invokation of the specified method
	 * 
	 * @param methodNames Method names to search for
	 * @param ordinal ID of the invokation to hook, or -1 to hook all invokations
	 */
	public BeforeInvoke(String methodName, int ordinal)
	{
		this(new String[] { methodName }, null, null, ordinal);
	}
	
	/**
	 * Match the specified invokation of the specified method(s)
	 * 
	 * @param methodNames Method names to search for
	 * @param ordinal ID of the invokation to hook, or -1 to hook all invokations
	 */
	public BeforeInvoke(String[] methodNames, int ordinal)
	{
		this(methodNames, null, null, ordinal);
	}
	
	/**
	 * Match all occurrences of the specified method or methods with the specified owners
	 * 
	 * @param methodNames Method names to search for
	 * @param methodOwners Owners to search for, indices in this array MUST match the indices in methodNames, eg. if methodNames contains
	 * { "mcpName", "func_12345_a", "a" } then methodOwners should contain { "net/minecraft/pkg/ClassName", "net/minecraft/pkg/ClassName", "abc" }
	 * in order that the appropriate owner name obfuscation matches the corresponding index in the methodNames array 
	 */
	public BeforeInvoke(String[] methodNames, String[] methodOwners)
	{
		this(methodNames, methodOwners, null, -1);
	}
	
	/**
	 * Match the specified invokation of the specified method or methods with the specified owners
	 * 
	 * @param methodNames Method names to search for
	 * @param methodOwners Owners to search for, indices in this array MUST match the indices in methodNames, eg. if methodNames contains
	 * { "mcpName", "func_12345_a", "a" } then methodOwners should contain { "net/minecraft/pkg/ClassName", "net/minecraft/pkg/ClassName", "abc" }
	 * in order that the appropriate owner name obfuscation matches the corresponding index in the methodNames array 
	 * @param ordinal ID of the invokation to hook, or -1 to hook all invokations
	 */
	public BeforeInvoke(String[] methodNames, String[] methodOwners, int ordinal)
	{
		this(methodNames, methodOwners, null, ordinal);
	}
	
	/**
	 * Match all occurrences of the specified method or methods with the specified owners or signatures, pass null to the owners array if you
	 * only want to match signatures
	 * 
	 * @param methodNames Method names to search for
	 * @param methodOwners Owners to search for, indices in this array MUST match the indices in methodNames, eg. if methodNames contains
	 * { "mcpName", "func_12345_a", "a" } then methodOwners should contain { "net/minecraft/pkg/ClassName", "net/minecraft/pkg/ClassName", "abc" }
	 * in order that the appropriate owner name obfuscation matches the corresponding index in the methodNames array 
	 * @param methodSignatures Signatures to search for, indices in this array MUST match the indices in methodNames, eg. if methodNames contains
	 * { "mcpName", "func_12345_a", "a" } then methodSignatures should contain { "(Lnet/minecraft/pkg/ClassName;)V", "(Lnet/minecraft/pkg/ClassName;)V", "(Labc;)V" }
	 * in order that the appropriate signature obfuscation matches the corresponding index in the methodNames array (and ownerNames array if present)
	 */
	public BeforeInvoke(String[] methodNames, String[] methodOwners, String[] methodSignatures)
	{
		this(methodNames, methodOwners, methodSignatures, -1);
	}
	
	/**
	 * Match the specified invokation of the specified method or methods with the specified owners or signatures, pass null to the owners array if you
	 * only want to match signatures
	 * 
	 * @param methodNames Method names to search for
	 * @param methodOwners Owners to search for, indices in this array MUST match the indices in methodNames, eg. if methodNames contains
	 * { "mcpName", "func_12345_a", "a" } then methodOwners should contain { "net/minecraft/pkg/ClassName", "net/minecraft/pkg/ClassName", "abc" }
	 * in order that the appropriate owner name obfuscation matches the corresponding index in the methodNames array 
	 * @param methodSignatures Signatures to search for, indices in this array MUST match the indices in methodNames, eg. if methodNames contains
	 * { "mcpName", "func_12345_a", "a" } then methodSignatures should contain { "(Lnet/minecraft/pkg/ClassName;)V", "(Lnet/minecraft/pkg/ClassName;)V", "(Labc;)V" }
	 * in order that the appropriate signature obfuscation matches the corresponding index in the methodNames array (and ownerNames array if present)
	 * @param ordinal ID of the invokation to hook, or -1 to hook all invokations
	 */
	public BeforeInvoke(String[] methodNames, String[] methodOwners, String[] methodSignatures, int ordinal)
	{
		if (methodNames == null || methodNames.length == 0)
			throw new IllegalArgumentException("Method name selector must not be null"); 
		
		if (methodSignatures != null && methodSignatures.length == 0) methodSignatures = null;
		if (methodOwners != null && methodOwners.length == 0) methodOwners = null;
		if (ordinal < 0) ordinal = -1;
		
		this.methodNames = methodNames;
		this.methodOwners = methodOwners;
		this.methodSignatures = methodSignatures;
		this.ordinal = ordinal;
	}
	
	public void setLogging(boolean logging)
	{
		this.logging = logging;
	}
	
	/* (non-Javadoc)
	 * @see com.mumfrey.liteloader.transformers.event.InjectionStrategy#findInjectionPoint(java.lang.String, org.objectweb.asm.tree.InsnList, com.mumfrey.liteloader.transformers.event.Event, java.util.Collection)
	 */
	@Override
	public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes, Event event)
	{
		int ordinal = 0;
		boolean found = false;
		
		ListIterator<AbstractInsnNode> iter = insns.iterator();
		while (iter.hasNext())
		{
			AbstractInsnNode insn = iter.next();
			
			if (insn instanceof MethodInsnNode)
			{
				MethodInsnNode node = (MethodInsnNode)insn;
				
				if (this.logging) LiteLoaderLogger.info("BeforeInvokeStrategy is considering invokation NAME=" + node.name + " DESC=" + node.desc + " OWNER=" + node.owner);
				
				int index = BeforeInvoke.arrayIndexOf(this.methodNames, node.name, -1);
				if (index > -1 && this.logging) LiteLoaderLogger.info("BeforeInvokeStrategy found a matching invoke, checking owner/signature...");
				
				if (index > -1 && BeforeInvoke.arrayIndexOf(this.methodOwners, node.owner, index) == index && BeforeInvoke.arrayIndexOf(this.methodSignatures, node.desc, index) == index)
				{
					if (this.logging) LiteLoaderLogger.info("BeforeInvokeStrategy found a matching invoke, checking ordinal...");
					if (this.ordinal == -1)
					{
						if (this.logging) LiteLoaderLogger.info("BeforeInvokeStrategy found a matching invoke at ordinal %d", ordinal);
						nodes.add(node);
						found = true;
					}
					else if (this.ordinal == ordinal)
					{
						if (this.logging) LiteLoaderLogger.info("BeforeInvokeStrategy found a matching invoke at ordinal %d", ordinal);
						nodes.add(node);
						return true;
					}
					
					ordinal++;
				}
			}
		}
		
		return found;
	}

	/**
	 * Special version of contains which returns TRUE if the haystack array is null, which is an odd behaviour we actually
	 * want here because null indicates that the value is not important
	 * 
	 * @param haystack
	 * @param needle
	 * @return
	 */
	private static int arrayIndexOf(String[] haystack, String needle, int pos)
	{
		if (haystack == null) return pos;
		for (int index = 0; index < haystack.length; index++)
		{
			if (needle.equals(haystack[index])) return index;
		}
		return pos;
	}
}
