package com.mumfrey.liteloader.modconfig;

import com.google.gson.GsonBuilder;

/**
 * Interface for Exposables which want a finer degree of control over the serialisation process
 *
 * @author Adam Mummery-Smith
 */
public interface AdvancedExposable extends Exposable
{
	/**
	 * Allows this object to configure the GsonBuilder prior to the construction of the Gson instance. Use
	 * this callback to (for example) register custom type adapters or set other Gson options such as
	 * pretty printing.
	 * 
	 * @param gsonBuilder
	 */
	public abstract void setupGsonSerialiser(GsonBuilder gsonBuilder);
}
