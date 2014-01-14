package com.mumfrey.liteloader.core.transformers;

import com.mumfrey.liteloader.core.runtime.Obf;

/**
 * Information for injected callback methods
 * 
 * @author Adam Mummery-Smith
 */
public class Callback
{
	/**
	 * Type of callback to inject
	 */
	public enum CallBackType
	{
		/**
		 * Redirect callbacks are injected at the start of a method and either usurp the normal
		 * method behaviour (returnFrom is true) or act as simple method hooks (returnFrom is false)
		 */
		REDIRECT,
		
		/**
		 * Return callbacks are injected immediately prior to every RETURN opcode in a particular
		 * method and returnFrom must match the return type of the method (eg. false for methods which
		 * return void and true for all methods with a return value)
		 */
		RETURN
	}

	public final String callbackClass;
	public final String callbackMethod;
	public final boolean returnFrom;
	public int refNumber;
	
	Callback(String callbackMethod)
	{
		this(callbackMethod, Obf.InjectedCallbackProxy.ref, false);
	}
	
	Callback(String callbackMethod, boolean returnFrom)
	{
		this(callbackMethod, Obf.InjectedCallbackProxy.ref, returnFrom);
	}
	
	public Callback(String callbackMethod, String callbackClass, boolean returnFrom)
	{
		this.callbackClass = callbackClass.replace('.', '/');
		this.callbackMethod = callbackMethod;
		this.returnFrom = returnFrom;
	}
	
	private Callback(String callbackMethod, String callbackClass, int refNumber)
	{
		this(callbackMethod, callbackClass, false);
		this.refNumber = refNumber;
	}
	
	public Callback getNextCallback()
	{
		return new Callback(this.callbackMethod, this.callbackClass, this.refNumber++);
	}
	
	@Override
	public String toString()
	{
		return this.callbackMethod;
	}
}