package com.mumfrey.liteloader.core.hooks.asm;

/**
 * Struct which contains a mapping of a PacketTransformer's priority to its class name, used for
 * sorting PacketTransformers during initialisation
 * 
 * @author Adam Mummery-Smith
 */
public class PacketTransformerInfo implements Comparable<PacketTransformerInfo>
{
	private final int priority;
	
	private final int order;
	
	private final String transformerClassName;

	public PacketTransformerInfo(int priority, int order, String transformerClassName)
	{
		this.priority = priority;
		this.order = order;
		this.transformerClassName = transformerClassName;
	}

	public int getPriority()
	{
		return this.priority;
	}
	
	public int getOrder()
	{
		return this.order;
	}
	
	public String getTransformerClassName()
	{
		return this.transformerClassName;
	}
	
	@Override
	public int compareTo(PacketTransformerInfo other)
	{
		if (other == null) return 0;
		if (other.priority == this.priority) return this.order - other.order;
		return (this.priority - other.priority);
	}
}
