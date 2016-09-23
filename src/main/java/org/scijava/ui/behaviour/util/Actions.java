package org.scijava.ui.behaviour.util;

import javax.swing.ActionMap;
import javax.swing.InputMap;

import org.scijava.ui.behaviour.KeyStrokeAdder;
import org.scijava.ui.behaviour.io.InputTriggerConfig;

/**
 * Convenience class for adding to a {@link InputMap}/{@link ActionMap} pair,
 * with {@link InputMap} keys that are read from a given
 * {@link InputTriggerConfig}.
 *
 * The maps can be {@link #install(InputActionBindings, String) installed} into
 * {@link InputActionBindings}.
 *
 * @author Tobias Pietzsch
 */
public class Actions
{
	private final InputMap inputMap;

	private final ActionMap actionMap;

	protected final KeyStrokeAdder.Factory keyConfig;

	protected final KeyStrokeAdder keyStrokeAdder;

	/**
	 * Construct with new, empty {@link InputMap} and {@link ActionMap}. Actions
	 * that are added to these maps (using
	 * {@link #namedAction(AbstractNamedAction, String...)},
	 * {@link #runnableAction(Runnable, String, String...)}) have their key
	 * stroke triggers defined by the specified {@code keyConfig}.
	 *
	 * @param keyConfig
	 *            added actions have their key stroke triggers defined by the
	 *            specified {@code keyConfig}. (overrides default key strokes.)
	 * @param keyConfigContexts
	 *            for which context names in the keyConfig should key strokes be
	 *            retrieved.
	 */
	public Actions(
			final KeyStrokeAdder.Factory keyConfig,
			final String ... keyConfigContexts )
	{
		this( new InputMap(), new ActionMap(), keyConfig, keyConfigContexts );
	}

	/**
	 * Construct with specified {@link InputMap} and {@link ActionMap}. Actions
	 * that are added to these maps (using
	 * {@link #namedAction(AbstractNamedAction, String...)},
	 * {@link #runnableAction(Runnable, String, String...)}) have their key
	 * stroke triggers defined by the specified {@code keyConfig}.
	 *
	 * @param inputMap
	 *            {@link InputMap} to add to.
	 * @param actionMap
	 *            {@link ActionMap} to add to.
	 * @param keyConfig
	 *            added actions have their key stroke triggers defined by the
	 *            specified {@code keyConfig}. (overrides default key strokes.)
	 * @param keyConfigContexts
	 *            for which context names in the keyConfig should key strokes be
	 *            retrieved.
	 */
	public Actions(
			final InputMap inputMap,
			final ActionMap actionMap,
			final KeyStrokeAdder.Factory keyConfig,
			final String ... keyConfigContexts )
	{
		this.actionMap = actionMap;
		this.inputMap = inputMap;
		this.keyConfig = keyConfig;
		keyStrokeAdder = keyConfig.keyStrokeAdder( inputMap, keyConfigContexts );
	}

	public InputMap getInputMap()
	{
		return inputMap;
	}

	public ActionMap getActionMap()
	{
		return actionMap;
	}

	/**
	 * Adds the {@link InputMap} and {@link ActionMap} to
	 * {@code inputActionBindings} under the given {@code name}.
	 * <p>
	 * Convenience method for
	 *
	 * <pre>
	 * inputActionBindings.addActionMap( name, getActionMap() );
	 * inputActionBindings.addInputMap( name, getInputMap() );
	 * </pre>
	 *
	 * @param inputActionBindings
	 *            where to install the {@link InputMap}/{@link ActionMap}.
	 * @param name
	 *            name under which the {@link InputMap}/{@link ActionMap} is
	 *            installed.
	 */
	public void install(
			final InputActionBindings inputActionBindings,
			final String name )
	{
		inputActionBindings.addActionMap( name, actionMap );
		inputActionBindings.addInputMap( name, inputMap );
	}

	/**
	 * Create and install a new {@link Action} with the specified {@code name} that
	 * calls the specified {@link Runnable} when triggered.
	 *
	 * @param runnable
	 *            action to install.
	 * @param name
	 *            name of the action.
	 * @param defaultKeyStrokes
	 *            default key strokes that trigger the action. These are used,
	 *            if no mapping in the {@link InputTriggerConfig} (specified in
	 *            the constructor) is found.
	 */
	public void runnableAction( final Runnable runnable, final String name, final String... defaultKeyStrokes )
	{
		namedAction( new RunnableAction( name, runnable ), defaultKeyStrokes );
	}

	/**
	 * Install the specified {@link AbstractNamedAction}.
	 *
	 * @param action
	 *            action to install.
	 * @param defaultKeyStrokes
	 *            default key strokes that trigger the action. These are used,
	 *            if no mapping in the {@link InputTriggerConfig} (specified in
	 *            the constructor) is found.
	 */
	public void namedAction( final AbstractNamedAction action, final String... defaultKeyStrokes )
	{
		keyStrokeAdder.put( action.name(), defaultKeyStrokes );
		action.put( actionMap );
	}
}
