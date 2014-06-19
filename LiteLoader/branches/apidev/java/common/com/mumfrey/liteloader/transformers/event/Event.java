package com.mumfrey.liteloader.transformers.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import com.mumfrey.liteloader.core.runtime.Obf;
import com.mumfrey.liteloader.util.log.LiteLoaderLogger;

/**
 * An injectable "event". An event is like a regular callback except that it is more intelligent about where
 * it can be injected in the bytecode and also supports conditional "cancellation", which is the ability to
 * conditionally return from the containing method with a custom return value.
 * 
 * @author Adam Mummery-Smith
 */
public class Event implements Comparable<Event>
{
	/**
	 * Natural ordering of events, for use with sorting events which have the same priority
	 */
	private static int eventOrder = 0;
	
	/**
	 * All the events which exist and their registered listeners
	 */
	private static final Set<Event> events = new HashSet<Event>();
	
	private static final Map<MethodNode, List<Event>> handlerMethods = new LinkedHashMap<MethodNode, List<Event>>();

	private static boolean generatedProxy = false;
	
	/**
	 * The name of this event
	 */
	protected final String name;
	
	/**
	 * Whether this event is cancellable - if it is cancellable then the isCancelled() -> RETURN code will be injected
	 */
	protected final boolean cancellable;
	
	/**
	 * Natural order of this event, for sorting 
	 */
	private final int order;
	
	/**
	 * Priority of this event, for sorting 
	 */
	private final int priority;
	
	private Set<MethodInfo> listeners = new HashSet<MethodInfo>();
	
	/**
	 * Method this event is currently "attached" to, we "attach" at the beginning of a method injection in order to save
	 * recalculating things like the return type and descriptor for each invokation, this means we need to calculate these
	 * things at most once for each method this event is injecting into.
	 */
	protected MethodNode attchedMethod;
	
	/**
	 * Descriptor for this event in the context of the attached method 
	 */
	protected String eventDescriptor;
	
	/**
	 * Method's original MAXS, used as a base to work out whether we need to increase the MAXS value
	 */
	protected int methodMAXS = 0;

	/**
	 * True if the attached method is static, used so that we know whether to push "this" onto the stack when
	 * constructing the EventInfo, or "null"
	 */
	protected boolean methodIsStatic;

	/**
	 * Return type for the attached method, used to determine which EventInfo class to use and which method
	 * to invoke.
	 */
	protected Type methodReturnType;

	protected String eventInfoClass;
	
	Event(String name, boolean cancellable, int priority)
	{
		this.name = name.toLowerCase();
		this.priority = priority;
		this.order = Event.eventOrder++;
		this.cancellable = cancellable;
		
		if (Event.events.contains(this))
		{
			throw new IllegalArgumentException("Event " + name + " is already defined");
		}
		
		Event.events.add(this);
	}
	
	/**
	 * Creates a new event with the specified name, if an event with the specified name already exists then
	 * the existing event is returned instead.
	 * 
	 * @param name Event name (case insensitive)
	 * @return
	 */
	public static Event getOrCreate(String name)
	{
		return Event.getOrCreate(name, false, 1000, false);
	}
	
	/**
	 * Creates a new event with the specified name, if an event with the specified name already exists then
	 * the existing event is returned instead.
	 * 
	 * @param name Event name (case insensitive)
	 * @param cancellable True if the event should be created as cancellable
	 * @return
	 */
	public static Event getOrCreate(String name, boolean cancellable)
	{
		return Event.getOrCreate(name, cancellable, 1000, true);
	}
	
	/**
	 * Creates a new event with the specified name, if an event with the specified name already exists then
	 * the existing event is returned instead.
	 * 
	 * @param name Event name (case insensitive)
	 * @param cancellable True if the event should be created as cancellable
	 * @param priority Priority for the event, only used when multiple events are being injected at the same instruction
	 * @return
	 */
	public static Event getOrCreate(String name, boolean cancellable, int priority)
	{
		return getOrCreate(name, cancellable, priority, true);
	}

	protected static Event getOrCreate(String name, boolean cancellable, int priority, boolean defining)
	{
		Event event = Event.getEvent(name);
		if (event != null)
		{
			if (!event.cancellable && cancellable && defining)
			{
				throw new IllegalArgumentException("Attempted to define the event " + event.name + " with cancellable '" + cancellable + "' but the event is already defined with cancellable is '" + event.cancellable + "'");
			}
			
			return event;
		}
		
		return new Event(name, cancellable, priority);
	}

	public String getName()
	{
		return this.name;
	}
	
	public boolean isCancellable()
	{
		return this.cancellable;
	}
	
	public int getPriority()
	{
		return this.priority;
	}
	
	public boolean isAttached()
	{
		return this.attchedMethod != null;
	}
	
	/**
	 * @param method
	 */
	void attach(final MethodNode method)
	{
		if (this.attchedMethod != null)
		{
			throw new IllegalStateException("Attempted to attach the event " + this.name + " to " + method.name + " but the event was already attached to " + this.attchedMethod.name + "!");
		}
		
		this.attchedMethod   = method;
		this.methodReturnType      = Type.getReturnType(method.desc);
		this.methodMAXS      = method.maxStack;
		this.methodIsStatic  = (method.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
		this.eventInfoClass  = EventInfo.getEventInfoClassName(this.methodReturnType).replace('.', '/');
		this.eventDescriptor = String.format("(L%s;%s)V", this.eventInfoClass, method.desc.substring(1, method.desc.indexOf(')')));
	}
	
	void detach()
	{
		this.attchedMethod = null;
	}
	
	public void addToHandler(MethodNode handler)
	{
		LiteLoaderLogger.debug("Adding event %s to handler %s", this.name, handler.name);
		
		List<Event> handlerEvents = Event.handlerMethods.get(handler);
		if (handlerEvents != null)
		{
			handlerEvents.add(this);
		}
	}
	
	final MethodNode inject(final String className, final MethodNode method, final AbstractInsnNode injectionPoint, boolean cancellable, final int globalEventID)
	{
		this.validate(className, method, injectionPoint, cancellable, globalEventID);
		
		MethodNode handler = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, Event.getHandlerName(globalEventID), this.eventDescriptor, null, null);
		Event.handlerMethods.put(handler, new ArrayList<Event>());
		
		LiteLoaderLogger.debug("Event %s is spawning handler %s", this.name, handler.name);
		
		Type[] argumentTypes = Type.getArgumentTypes(method.desc);
		int ctorMAXS = 0, invokeMAXS = argumentTypes.length;
		int eventInfoVar = method.maxLocals++;
		
		InsnList insns = new InsnList();
		insns.add(new TypeInsnNode(Opcodes.NEW, this.eventInfoClass)); ctorMAXS++;
		insns.add(new InsnNode(Opcodes.DUP)); ctorMAXS++; invokeMAXS++;
		insns.add(new LdcInsnNode(this.name)); ctorMAXS++;
		insns.add(this.methodIsStatic ? new InsnNode(Opcodes.ACONST_NULL) : new VarInsnNode(Opcodes.ALOAD, 0)); ctorMAXS++;
		insns.add(new InsnNode(cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0)); ctorMAXS++;
		insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, this.eventInfoClass, Obf.constructor.name, EventInfo.getConstructorDescriptor()));
		insns.add(new VarInsnNode(Opcodes.ASTORE, eventInfoVar));
		insns.add(new VarInsnNode(Opcodes.ALOAD, eventInfoVar));
		Event.pushArgs(argumentTypes, insns, this.methodIsStatic);
		insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Obf.EventProxy.ref, handler.name, handler.desc));
		
		if (cancellable)
		{
			this.injectCancellationCode(insns, injectionPoint, eventInfoVar);
		}
		
		method.instructions.insertBefore(injectionPoint, insns);
		method.maxStack = Math.max(method.maxStack, Math.max(this.methodMAXS + ctorMAXS, this.methodMAXS + invokeMAXS));
		
		return handler;
	}

	protected void validate(final String className, final MethodNode method, final AbstractInsnNode injectionPoint, boolean cancellable, final int globalEventID)
	{
		if (this.attchedMethod == null)
		{
			throw new IllegalStateException("Attempted to inject the event " + this.name + " but the event is not attached!");
		}
		
		if (Event.generatedProxy)
		{
			throw new IllegalStateException("Attempted to inject the event " + this.name + " but the event proxy was already generated!");
		}
	}
	
	protected void injectCancellationCode(final InsnList insns, final AbstractInsnNode injectionPoint, int eventInfoVar)
	{
		insns.add(new VarInsnNode(Opcodes.ALOAD, eventInfoVar));
		insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, this.eventInfoClass, EventInfo.getIsCancelledMethodName(), EventInfo.getIsCancelledMethodSig()));

		LabelNode notCancelled = new LabelNode();
		insns.add(new JumpInsnNode(Opcodes.IFEQ, notCancelled));
		
		this.injectReturnCode(insns, injectionPoint, eventInfoVar);
		
		insns.add(notCancelled);
	}

	protected void injectReturnCode(final InsnList insns, final AbstractInsnNode injectionPoint, int eventInfoVar)
	{
		if (this.methodReturnType.equals(Type.VOID_TYPE))
		{
			insns.add(new InsnNode(Opcodes.RETURN));
		}
		else
		{
			insns.add(new VarInsnNode(Opcodes.ALOAD, eventInfoVar));
			String accessor = ReturnEventInfo.getReturnAccessor(this.methodReturnType);
			String descriptor = ReturnEventInfo.getReturnDescriptor(this.methodReturnType);
			insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, this.eventInfoClass, accessor, descriptor));
			if (this.methodReturnType.getSort() == Type.OBJECT)
			{
				insns.add(new TypeInsnNode(Opcodes.CHECKCAST, this.methodReturnType.getInternalName()));
			}
			insns.add(new InsnNode(this.methodReturnType.getOpcode(Opcodes.IRETURN)));
		}
	}
	
	/**
	 * Add a listener for this event, the listener
	 * 
	 * @param listener
	 */
	public void addListener(MethodInfo listener)
	{
		if (listener.hasDesc())
		{
			throw new IllegalArgumentException("Descriptor is not allowed for listener methods");
		}
		
		this.listeners.add(listener);
	}
	
	static Event getEvent(String eventName)
	{
		for (Event event : Event.events)
			if (event.name.equalsIgnoreCase(eventName))
				return event;
		
		return null;
	}

	static Set<MethodInfo> getEventListeners(String eventName)
	{
		return Event.getEventListeners(Event.getEvent(eventName));
	}
	
	static Set<MethodInfo> getEventListeners(Event event)
	{
		return event == null ? null : Collections.unmodifiableSet(event.listeners);
	}
	
	static ClassNode populateProxy(final ClassNode classNode)
	{
		Event.generatedProxy = true;
		
		int handlerCount = 0;
		int invokeCount = 0;
		int lineNumber = 210;
		
		for (Entry<MethodNode, List<Event>> handler : Event.handlerMethods.entrySet())
		{
			MethodNode handlerMethod = handler.getKey();
			List<Event> handlerEvents = handler.getValue();
			Type[] args = Type.getArgumentTypes(handlerMethod.desc);
			
			classNode.methods.add(handlerMethod);
			handlerCount++;
			
			InsnList insns = handlerMethod.instructions;
			for (Event event : handlerEvents)
			{
				Set<MethodInfo> listeners = event.listeners;
				if (listeners.size() > 0)
				{
					LabelNode tryCatchStart = new LabelNode();
					LabelNode tryCatchEnd = new LabelNode();
					LabelNode tryCatchHandler1 = new LabelNode();
					LabelNode tryCatchHandler2 = new LabelNode();
					LabelNode tryCatchExit = new LabelNode();
					
					handlerMethod.tryCatchBlocks.add(new TryCatchBlockNode(tryCatchStart, tryCatchEnd, tryCatchHandler1, "java/lang/NoSuchMethodError"));
					handlerMethod.tryCatchBlocks.add(new TryCatchBlockNode(tryCatchStart, tryCatchEnd, tryCatchHandler2, "java/lang/NoClassDefFoundError"));
					
					insns.add(tryCatchStart);
					
					for (MethodInfo listener : listeners)
					{
						invokeCount++;
						
						LabelNode lineNumberLabel = new LabelNode(new Label());
						insns.add(lineNumberLabel);
						insns.add(new LineNumberNode(++lineNumber, lineNumberLabel));
						
						Event.pushArgs(args, insns, true);
						insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, listener.ownerRef, listener.name, handlerMethod.desc));
					}
					
					insns.add(tryCatchEnd);
					insns.add(new JumpInsnNode(Opcodes.GOTO, tryCatchExit));
					
					insns.add(tryCatchHandler1);
					insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
					insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Obf.EventProxy.ref, "onMissingHandler", "(Ljava/lang/Error;Lcom/mumfrey/liteloader/transformers/event/EventInfo;)V"));
					insns.add(new JumpInsnNode(Opcodes.GOTO, tryCatchExit));
					
					insns.add(tryCatchHandler2);
					insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
					insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Obf.EventProxy.ref, "onMissingClass", "(Ljava/lang/Error;Lcom/mumfrey/liteloader/transformers/event/EventInfo;)V"));
					insns.add(new JumpInsnNode(Opcodes.GOTO, tryCatchExit));
					
					insns.add(tryCatchExit);
				}
			}
		
			insns.add(new InsnNode(Opcodes.RETURN));
		}
		
		LiteLoaderLogger.info("Successfully generated event handler proxy class with %d handlers(s) and %d total invokations", handlerCount, invokeCount);
		
		return classNode;
	}

	private static String getHandlerName(int globalEventID)
	{
		return String.format("$event%05x", globalEventID);
	}

	/**
	 * @param args
	 * @param insns
	 */
	private static void pushArgs(Type[] args, InsnList insns, boolean isStatic)
	{
		int argNumber = isStatic ? 0 : 1;
		for (Type type : args)
		{
			insns.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), argNumber));
			argNumber += type.getSize();
		}
	}
	
	@Override
	public int compareTo(Event other)
	{
		if (other == null) return 0;
		if (other.priority == this.priority) return this.order - other.order;
		return (this.priority - other.priority);
	}

	@Override
	public int hashCode()
	{
		return this.name.hashCode();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == this) return true;
		if (other instanceof Event) return ((Event)other).name.equals(this.name);
		return false;
	}
	
	@Override
	public String toString()
	{
		return this.name;
	}
}
