package org.scijava.ui.behaviour.util;

import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;
import org.scijava.ui.behaviour.InputTriggerAdder;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

/**
 * Convenience class for adding to a {@link InputTriggerMap}/{@link BehaviourMap} pair,
 * with {@link InputTriggerMap} triggers that are read from a given
 * {@link InputTriggerConfig}.
 *
 * The maps can be {@link #install(TriggerBehaviourBindings, String) installed} into
 * {@link TriggerBehaviourBindings}.
 *
 * @author Tobias Pietzsch
 */
public class Behaviours
{
	private final InputTriggerMap inputTriggerMap;

	private final BehaviourMap behaviourMap;

	protected final InputTriggerAdder.Factory keyConfig;

	protected final InputTriggerAdder inputTriggerAdder;

	/**
	 * Construct with new, empty {@link InputTriggerMap} and
	 * {@link BehaviourMap}. Behaviours that are added to these maps (using
	 * {@link #behaviour(Behaviour, String, String...)},
	 * {@link #namedBehaviour(AbstractNamedBehaviour, String...)}) have their
	 * triggers defined by the specified {@code keyConfig}.
	 *
	 * @param keyConfig
	 *            added behaviours have their triggers defined by the specified
	 *            {@code keyConfig}. (overrides default triggers.)
	 * @param keyConfigContexts
	 *            for which context names in the keyConfig should triggers be
	 *            retrieved.
	 */
	public Behaviours(
			final InputTriggerAdder.Factory keyConfig,
			final String ... keyConfigContexts )
	{
		this( new InputTriggerMap(), new BehaviourMap(), keyConfig, keyConfigContexts );
	}

	/**
	 * Construct with the specified {@link InputTriggerMap} and
	 * {@link BehaviourMap}. Behaviours that are added to these maps (using
	 * {@link #behaviour(Behaviour, String, String...)},
	 * {@link #namedBehaviour(AbstractNamedBehaviour, String...)}) have their
	 * triggers defined by the specified {@code keyConfig}.
	 *
	 * @param inputTriggerMap
	 *            {@link InputTriggerMap} to add to.
	 * @param behaviourMap
	 *            {@link BehaviourMap} to add to.
	 * @param keyConfig
	 *            added behaviours have their triggers defined by the specified
	 *            {@code keyConfig}. (overrides default triggers.)
	 * @param keyConfigContexts
	 *            for which context names in the keyConfig should triggers be
	 *            retrieved.
	 */
	public Behaviours(
			final InputTriggerMap inputTriggerMap,
			final BehaviourMap behaviourMap,
			final InputTriggerAdder.Factory keyConfig,
			final String ... keyConfigContexts )
	{
		this.inputTriggerMap = inputTriggerMap;
		this.behaviourMap = behaviourMap;
		this.keyConfig = keyConfig;
		inputTriggerAdder = keyConfig.inputTriggerAdder( inputTriggerMap, keyConfigContexts );
	}

	public InputTriggerMap getInputTriggerMap()
	{
		return inputTriggerMap;
	}

	public BehaviourMap getBehaviourMap()
	{
		return behaviourMap;
	}

	/**
	 * Adds the {@link InputTriggerMap} and {@link BehaviourMap} to
	 * {@code triggerBehaviourBindings} under the given {@code name}.
	 * <p>
	 * Convenience method for
	 *
	 * <pre>
		triggerBehaviourBindings.addInputTriggerMap( name, getInputTriggerMap() );
		triggerBehaviourBindings.addBehaviourMap( name, getBehaviourMap() );
	 * </pre>
	 *
	 * @param triggerBehaviourBindings
	 *            where to install the {@link InputTriggerMap}/
	 *            {@link BehaviourMap}.
	 * @param name
	 *            name under which the {@link InputTriggerMap}/
	 *            {@link BehaviourMap} is installed.
	 */
	public void install(
			final TriggerBehaviourBindings triggerBehaviourBindings,
			final String name )
	{
		triggerBehaviourBindings.addInputTriggerMap( name, inputTriggerMap );
		triggerBehaviourBindings.addBehaviourMap( name, behaviourMap );
	}

	public void behaviour( final Behaviour behaviour, final String name, final String... defaultTriggers )
	{
		inputTriggerAdder.put( name, defaultTriggers );
		behaviourMap.put( name, behaviour );
	}

	public void namedBehaviour( final AbstractNamedBehaviour behaviour, final String... defaultTriggers )
	{
		inputTriggerAdder.put( behaviour.name(), defaultTriggers );
		behaviour.put( behaviourMap );
	}
}
