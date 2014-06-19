package com.mumfrey.liteloader.transformers.event;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.transformers.Callback;

/**
 * Encapsulates a method descriptor with varying degrees of accuracy from a simpler owner/method mapping up to
 * and including a multi-faceted notch/srg/mcp method descriptor which works in all obfuscation environments. 
 * 
 * @author Adam Mummery-Smith
 */
public class MethodInfo
{
	// Owning class
	final String owner;
	final String ownerRef;
	final String ownerObf;
	
	// Method name
	final String name;
	final String nameSrg;
	final String nameObf;
	
	// Descriptor
	final String desc;
	final String descObf;
	
	// "Signature" - method name plus descriptor
	final String sig;
	final String sigSrg;
	final String sigObf;
	
	public MethodInfo(String owner, String method)
	{
		this(owner, owner, method, method, method, null, null);
	}
	
	public MethodInfo(Obf owner, String method)
	{
		this(owner.name, owner.obf, method, method, method, null, null);
	}
	
	public MethodInfo(Obf owner, String method, String descriptor)
	{
		this(owner.name, owner.obf, method, method, method, descriptor, descriptor);
	}
	
	public MethodInfo(String owner, String method, String descriptor)
	{
		this(owner, owner, method, method, method, descriptor, descriptor);
	}
	
	public MethodInfo(Obf owner, Obf method, String descriptor)
	{
		this(owner.name, owner.obf, method.name, method.srg, method.obf, descriptor, descriptor);
	}
	
	public MethodInfo(Obf owner, String method, Object returnType, Object... args)
	{
		this(owner.name, owner.obf, method, method, method, Callback.generateDescriptor(Obf.MCP, returnType, args), Callback.generateDescriptor(Obf.OBF, returnType, args));
	}
	
	public MethodInfo(Obf owner, Obf method, Object returnType, Object... args)
	{
		this(owner.name, owner.obf, method.name, method.srg, method.obf, Callback.generateDescriptor(Obf.MCP, returnType, args), Callback.generateDescriptor(Obf.OBF, returnType, args));
	}

	/**
	 * @param owner
	 * @param ownerObf
	 * @param name
	 * @param nameSrg
	 * @param nameObf
	 * @param desc
	 * @param descObf
	 */
	MethodInfo(String owner, String ownerObf, String name, String nameSrg, String nameObf, String desc, String descObf)
	{
		this.owner    = owner.replace('/', '.');
		this.ownerRef = owner.replace('.', '/');
		this.ownerObf = ownerObf;
		this.name     = name;
		this.nameSrg  = nameSrg;
		this.nameObf  = nameObf;
		this.desc     = desc;
		this.descObf  = descObf;
		this.sig      = MethodInfo.generateSignature(this.name, this.desc);
		this.sigSrg   = MethodInfo.generateSignature(this.nameSrg, this.desc);
		this.sigObf   = MethodInfo.generateSignature(this.nameObf, this.descObf);
	}

	public String getOwner()
	{
		return this.owner;
	}

	public String getOwnerObf()
	{
		return this.ownerObf;
	}

	public String getName()
	{
		return this.name;
	}

	public String getNameSrg()
	{
		return this.nameSrg;
	}

	public String getNameObf()
	{
		return this.nameObf;
	}

	public String getDesc()
	{
		return this.desc;
	}

	public String getDescObf()
	{
		return this.descObf;
	}
	
	public boolean hasDesc()
	{
		return this.desc != null;
	}

	public String getSignature(int type)
	{
		if (type == Obf.OBF) return this.sigObf;
		if (type == Obf.SRG) return this.sigSrg;
		return this.sig;
	}
	
	public boolean matches(String method, String desc)
	{
		return this.matches(method, desc, null);
	}
		
	public boolean matches(String method, String desc, String className)
	{
		if ((className == null || this.owner.equals(className)) && (this.name.equals(method) || this.nameSrg.equals(method)))
		{
			return this.desc == null || this.desc.equals(desc);
		}
		else if ((className == null || this.ownerObf.equals(className)) && this.nameObf.equals(method))
		{
			return this.descObf == null || this.descObf.equals(desc);
		}
		
		return false;
	}
	
	static String generateSignature(String methodName, String methodSignature)
	{
		return String.format("%s%s", methodName, methodSignature == null ? "" : methodSignature);
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == this) return true;
		if (other instanceof MethodInfo) return this.sig.equals(((MethodInfo)other).sig);
		if (other instanceof String) return this.sig.equals(other);
		return false;
	}
	
	@Override
	public String toString()
	{
		return this.sig;
	}
	
	@Override
	public int hashCode()
	{
		return this.sig.hashCode();
	}
}
