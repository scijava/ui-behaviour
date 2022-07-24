/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2022 Max Planck Institute of Molecular Cell Biology
 * and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.scijava.ui.behaviour.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

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

	private final String[] keyConfigContexts;

	protected KeyStrokeAdder.Factory keyConfig;

	protected KeyStrokeAdder keyStrokeAdder;

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
		this.keyConfig = keyConfig != null ? keyConfig : new InputTriggerConfig();
		this.keyConfigContexts = keyConfigContexts;
		keyStrokeAdder = this.keyConfig.keyStrokeAdder( inputMap, keyConfigContexts );
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
	 * Create and install a new {@link javax.swing.Action} with the specified
	 * {@code name} that calls the specified {@link Runnable} when triggered.
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

	/**
	 * Clears the {@link InputMap} and re-adds all ({@code String}) action keys
	 * from {@link ActionMap} using the provided {@code keyConfig}.
	 *
	 * @param keyConfig
	 *            the new keyConfig
	 */
	public void updateKeyConfig( final InputTriggerConfig keyConfig )
	{
		updateKeyConfig( keyConfig, true );
	}

	/**
	 * Clears the {@link InputMap} and re-adds all ({@code String}) action keys
	 * from {@link ActionMap} using the provided {@code keyConfig}.
	 * <p>
	 * Actions that are currently in the {@code InputMap} but are not defined in
	 * the {@code keyConfig} retain their current keystrokes (note that
	 * {@code keyConfig} can map actions to "not mapped").
	 *
	 * @param keyConfig
	 *            the new keyConfig
	 * @param clearAll
	 *            whether to clear all bindings (also of actions that are
	 *            undefined in {@code keyConfig})
	 */
	public void updateKeyConfig( final InputTriggerConfig keyConfig, final boolean clearAll )
	{
		this.keyConfig = keyConfig;

		final Map< Object, List< KeyStroke > > unassigned = new HashMap<>();
		if ( !clearAll )
		{
			final KeyStroke[] inputs = inputMap.keys();
			if ( inputs != null )
			{
				final HashSet< String > contexts = new HashSet<>( Arrays.asList( keyConfigContexts ) );
				for ( final KeyStroke input : inputs )
				{
					final Object actionKey = inputMap.get( input );
					if ( ( !( actionKey instanceof String ) ) || keyConfig.getInputs( ( String ) actionKey, contexts ).isEmpty() )
						unassigned.computeIfAbsent( actionKey, k -> new ArrayList<>() ).add( input );
				}
			}
		}

		keyStrokeAdder = keyConfig.keyStrokeAdder( inputMap, keyConfigContexts );
		inputMap.clear();
		final Object[] keys = actionMap.keys();
		if ( keys != null )
			for ( final Object o : keys )
				if ( !unassigned.containsKey( o ) )
					keyStrokeAdder.put( ( String ) o );

		unassigned.forEach( ( actionMapKey, keyStrokes ) -> keyStrokes.forEach( keyStroke -> inputMap.put( keyStroke, actionMapKey ) ) );
	}
}
