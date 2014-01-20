package com.mumfrey.liteloader.core.transformers;

import java.util.ArrayList;
import java.util.List;

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
	
	private List<Callback> chainedCallbacks = new ArrayList<Callback>();
	
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
	
	private Callback(String callbackMethod, String callbackClass, int refNumber, List<Callback> chainedCallbacks)
	{
		this(callbackMethod, callbackClass, false);
		this.refNumber = refNumber;
		this.chainedCallbacks = chainedCallbacks;
	}
	
	public Callback getNextCallback()
	{
		return new Callback(this.callbackMethod, this.callbackClass, this.refNumber++, this.chainedCallbacks);
	}
	
	void addChainedCallback(Callback chained)
	{
		this.chainedCallbacks.add(chained);
	}
	
	public List<Callback> getChainedCallbacks()
	{
		return this.chainedCallbacks;
	}	
	
	@Override
	public String toString()
	{
		return this.callbackMethod;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null || !(other instanceof Callback)) return false;
		Callback callback = (Callback)other;
		return callback.callbackClass.equals(this.callbackClass) && callback.callbackMethod.equals(this.callbackMethod) && callback.returnFrom == this.returnFrom;
	}

	@Override
	public int hashCode()
	{
		return super.hashCode();
	}
}