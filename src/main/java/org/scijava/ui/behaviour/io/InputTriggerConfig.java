/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2017 Max Planck Institute of Molecular Cell Biology
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
package org.scijava.ui.behaviour.io;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.InputMap;
import javax.swing.KeyStroke;

import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.InputTriggerAdder;
import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.KeyStrokeAdder;

public class InputTriggerConfig implements InputTriggerAdder.Factory, KeyStrokeAdder.Factory
{
	final LinkedHashMap< String, Set< Input > > actionToInputsMap;

	public InputTriggerConfig()
	{
		actionToInputsMap = new LinkedHashMap<>();
	}

	public InputTriggerConfig( final Collection< InputTriggerDescription > keyMappings ) throws IllegalArgumentException
	{
		actionToInputsMap = new LinkedHashMap<>();

		if ( keyMappings == null )
			return;

		for ( final InputTriggerDescription mapping : keyMappings )
		{
			final String behaviour = mapping.getAction();
			final HashSet< String > contexts = new HashSet<>();
			contexts.addAll( Arrays.asList( mapping.getContexts() ) );
			final String[] triggers = mapping.getTriggers();
			for ( final String triggerStr : triggers )
			{
				final InputTrigger trigger = InputTrigger.getFromString( triggerStr );
				final Input input = new Input( trigger, behaviour, contexts );

				final Set< Input > inputs = actionToInputsMap.computeIfAbsent( input.behaviour, k -> new LinkedHashSet<>() );
				inputs.add( input );
			}
		}
	}

	@Override
	public InputTriggerAdder inputTriggerAdder( final InputTriggerMap map, final String ... contexts )
	{
		return new InputTriggerAdderImp( map, this, contexts );
	}

	@Override
	public KeyStrokeAdder keyStrokeAdder( final InputMap map, final String ... contexts )
	{
		return new KeyStrokeAdderImp( map, this, contexts );
	}

	public Set< InputTrigger > getInputs( final String behaviourName, final String context )
	{
		return getInputs( behaviourName, Collections.singleton( context ) );
	}

	public Set< InputTrigger > getInputs( final String behaviourName, final Set< String > contexts )
	{
		final Set< Input > inputs = actionToInputsMap.get( behaviourName );
		final Set< InputTrigger > triggers = new LinkedHashSet<>();
		if ( inputs != null )
		{
			for ( final Input input : inputs )
				if ( ! Collections.disjoint( contexts, input.contexts ) )
					triggers.add( input.trigger );
		}
		return triggers;
	}

	public void clear()
	{
		actionToInputsMap.clear();
	}

	public void set( InputTriggerConfig config )
	{
		actionToInputsMap.clear();
		for ( Entry< String, Set< Input > > entry : config.actionToInputsMap.entrySet() )
		{
			final String behaviourName = entry.getKey();
			final Set< Input > inputs = new LinkedHashSet<>();
			entry.getValue().forEach( i -> inputs.add( i.copy() ) );
			actionToInputsMap.put( behaviourName, inputs );
		}
	}

	public void add( final String trigger, final String behaviourName, final String context )
	{
		add( InputTrigger.getFromString( trigger ), behaviourName, context );
	}

	public void add( final InputTrigger trigger, final String behaviourName, final String context )
	{
		add( trigger, behaviourName, Collections.singleton( context ) );
	}

	public synchronized void add( final InputTrigger trigger, final String behaviourName, final Collection< String > contexts )
	{
		final Set< Input > inputs = actionToInputsMap.computeIfAbsent( behaviourName, k -> new LinkedHashSet<>() );
		for ( final Input input : inputs )
		{
			if ( input.trigger.equals( trigger ) )
			{
				/*
				 * the trigger -> behaviour binding already exists.
				 * just add the new context
				 */
				input.contexts.addAll( contexts );
				return;
			}
		}

		/*
		 * the trigger -> behaviour binding does not exist.
		 * add it
		 */
		inputs.add( new Input( trigger, behaviourName, contexts ) );
	}

	public static class InputTriggerAdderImp implements InputTriggerAdder
	{
		private final InputTriggerMap map;

		private final InputTriggerConfig config;

		private final Set< String > contexts;

		public InputTriggerAdderImp(
				final InputTriggerMap map,
				final InputTriggerConfig config,
				final Set< String > contexts )
		{
			this.map = map;
			this.config = config;
			this.contexts = contexts;
		}

		public InputTriggerAdderImp(
				final InputTriggerMap map,
				final InputTriggerConfig config,
				final String ... contexts )
		{
			this.map = map;
			this.config = config;
			this.contexts = new HashSet<>();
			this.contexts.addAll( Arrays.asList( contexts ) );
		}

		@Override
		public void put( final String behaviourName, final InputTrigger ... defaultTriggers )
		{
			final Set< InputTrigger > triggers = config.getInputs( behaviourName, contexts );
			if ( !triggers.isEmpty() )
			{
				if ( triggers.contains( InputTrigger.NOT_MAPPED ) )
					return;

				for ( final InputTrigger trigger : triggers )
					map.put( trigger, behaviourName );
			}
			else if ( defaultTriggers.length > 0 )
			{
				if ( defaultTriggers[ 0 ].equals( InputTrigger.NOT_MAPPED ) )
				{
					config.add( InputTrigger.NOT_MAPPED, behaviourName, contexts );
					return;
				}

				for ( final InputTrigger trigger : defaultTriggers )
				{
					config.add( trigger, behaviourName, contexts );
					map.put( trigger, behaviourName );
				}
			}
			else
			{
				System.err.println( "Could not assign InputTrigger for \"" + behaviourName + "\". Nothing defined in InputTriggerConfig, and no default given." );
			}
		}

		@Override
		public void put( final String behaviourName, final String ... defaultTriggers )
		{
			final InputTrigger[] triggers = new InputTrigger[ defaultTriggers.length ];
			int i = 0;
			for ( final String s : defaultTriggers )
				triggers[ i++ ] = InputTrigger.getFromString( s );
			put( behaviourName, triggers );
		}

		@Override
		public void put( final String behaviourName )
		{
			put( behaviourName, new InputTrigger[ 0 ] );
		}
	}

	public static class KeyStrokeAdderImp implements KeyStrokeAdder
	{
		private final InputMap map;

		private final InputTriggerConfig config;

		private final Set< String > contexts;

		public KeyStrokeAdderImp(
				final InputMap map,
				final InputTriggerConfig config,
				final Set< String > contexts )
		{
			this.map = map;
			this.config = config;
			this.contexts = contexts;
		}

		public KeyStrokeAdderImp(
				final InputMap map,
				final InputTriggerConfig config,
				final String ... contexts )
		{
			this.map = map;
			this.config = config;
			this.contexts = new HashSet<>();
			this.contexts.addAll( Arrays.asList( contexts ) );
		}

		@Override
		public void put( final String actionName, final KeyStroke... defaultKeyStrokes )
		{
			final Set< InputTrigger > triggers = config.getInputs( actionName, contexts );
			if ( triggers.contains( InputTrigger.NOT_MAPPED ) )
				return;

			boolean configKeyAdded = false;
			for ( final InputTrigger trigger : triggers )
			{
				if ( trigger.isKeyStroke() )
				{
					map.put( trigger.getKeyStroke(), actionName );
					configKeyAdded = true;
				}
			}
			if ( configKeyAdded )
				return;

			if ( defaultKeyStrokes.length > 0 )
			{
				for ( final KeyStroke keyStroke : defaultKeyStrokes )
				{
					config.add( InputTrigger.getFromString( keyStroke.toString() ), actionName, contexts );
					map.put( keyStroke, actionName );
				}
			}
			else
			{
				System.err.println( "Could not assign KeyStroke for \"" + actionName + "\". Nothing defined in InputTriggerConfig, and no default given." );
			}
		}

		@Override
		public void put( final String actionName, final InputTrigger... defaultKeyStrokes )
		{
			final Set< InputTrigger > triggers = config.getInputs( actionName, contexts );
			if ( triggers.contains( InputTrigger.NOT_MAPPED ) )
				return;

			boolean configKeyAdded = false;
			for ( final InputTrigger trigger : triggers )
			{
				if ( trigger.isKeyStroke() )
				{
					map.put( trigger.getKeyStroke(), actionName );
					configKeyAdded = true;
				}
			}
			if ( configKeyAdded )
				return;

			if ( defaultKeyStrokes.length > 0 )
			{
				if ( defaultKeyStrokes[ 0 ].equals( InputTrigger.NOT_MAPPED ) )
				{
					config.add( InputTrigger.NOT_MAPPED, actionName, contexts );
					return;
				}

				for ( final InputTrigger trigger : defaultKeyStrokes )
				{
					if ( trigger.isKeyStroke() )
					{
						config.add( trigger, actionName, contexts );
						map.put( trigger.getKeyStroke(), actionName );
						configKeyAdded = true;
					}
				}
			}

			if ( !configKeyAdded )
				System.err.println( "Could not assign KeyStroke for \"" + actionName + "\". Nothing defined in InputTriggerConfig, and no default given." );
		}

		@Override
		public void put( final String actionName, final String... defaultKeyStrokes )
		{
			final InputTrigger[] keyStrokes = new InputTrigger[ defaultKeyStrokes.length ];
			int i = 0;
			for ( final String s : defaultKeyStrokes )
				keyStrokes[ i++ ] = InputTrigger.getFromString( s );
			put( actionName, keyStrokes );
		}

		@Override
		public void put( final String actionName )
		{
			put( actionName, new KeyStroke[ 0 ] );
		}
	}

	/*
	 * PRIVATE...
	 */

	static class Input
	{
		final InputTrigger trigger;

		final String behaviour;

		final Set< String > contexts;

		Input(
				final InputTrigger trigger,
				final String behaviour,
				final Collection< String > contexts )
		{
			this.trigger = trigger;
			this.behaviour = behaviour;
			this.contexts = new HashSet<>( contexts );
		}

		Input( Input input )
		{
			this.trigger = input.trigger;
			this.behaviour = input.behaviour;
			this.contexts = new HashSet<>( input.contexts );
		}

		Input copy()
		{
			return new Input( this );
		}

		@Override
		public int hashCode()
		{
			int value = 17;
			value = 31 * value + trigger.hashCode();
			value = 31 * value + behaviour.hashCode();
			value = 31 * value + contexts.hashCode();
			return value;
		}

		@Override
		public boolean equals( final Object obj )
		{
			if ( obj == null || !( obj instanceof Input ) )
				return false;
			final Input i = ( Input ) obj;
			return i.trigger.equals( trigger ) && i.behaviour.equals( behaviour ) && i.contexts.equals( contexts );
		}

		InputTriggerDescription getDescription()
		{
			return new InputTriggerDescription( new String[] { trigger.toString() }, behaviour, contexts.toArray( new String[ 0 ] ) );
		}
	}

	/*
	 * creating InputTriggerConfig from InputTriggerMaps and InputMaps
	 */

	void addMap( final InputTriggerMap map, final String context )
	{
		for ( final Entry< InputTrigger, Set< String > > entry : map.getAllBindings().entrySet() )
		{
			final InputTrigger trigger = entry.getKey();
			final Set< String > behaviours = entry.getValue();

			for ( final String behaviourName : behaviours )
			{
				final Set< Input > inputs = actionToInputsMap.computeIfAbsent( behaviourName, k -> new LinkedHashSet<>() );

				boolean added = false;
				for ( final Input input : inputs )
				{
					if ( input.trigger.equals( trigger ) )
					{
						/*
						 * the trigger -> behaviour binding already exists.
						 * just add the new context
						 */
						input.contexts.add( context );
						added = true;
						break;
					}
				}

				if ( !added )
				{
					inputs.add(	new Input(
							trigger,
							behaviourName,
							Collections.singleton( context ) ) );
				}
			}
		}
	}

	void addMap( final InputMap map, final String context )
	{
		final KeyStroke[] keys = map.allKeys();
		for ( final KeyStroke key : keys )
		{
			final InputTrigger trigger = InputTrigger.getFromString( key.toString() );
			final String behaviourName = map.get( key ).toString();

			final Set< Input > inputs = actionToInputsMap.computeIfAbsent( behaviourName, k -> new LinkedHashSet<>() );

			boolean added = false;
			for ( final Input input : inputs )
			{
				if ( input.trigger.equals( trigger ) )
				{
					/*
					 * the trigger -> behaviour binding already exists.
					 * just add the new context
					 */
					input.contexts.add( context );
					added = true;
					break;
				}
			}

			if ( !added )
			{
				inputs.add(	new Input(
						trigger,
						behaviourName,
						Collections.singleton( context ) ) );
			}
		}
	}
}
