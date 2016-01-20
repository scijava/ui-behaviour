package org.scijava.ui.behaviour.io;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
	final HashMap< String, Set< Input > > actionToInputsMap;

	public InputTriggerConfig()
	{
		actionToInputsMap = new HashMap<>();
	}

	public InputTriggerConfig( final Collection< InputTriggerDescription > keyMappings ) throws IllegalArgumentException
	{
		actionToInputsMap = new HashMap<>();

		if ( keyMappings == null )
			return;

		for ( final InputTriggerDescription mapping : keyMappings )
		{
			final String behaviour = mapping.getAction();
			final HashSet< String > contexts = new HashSet< String >();
			contexts.addAll( Arrays.asList( mapping.getContexts() ) );
			final String[] triggers = mapping.getTriggers();
			for ( final String triggerStr : triggers )
			{
				final InputTrigger trigger = InputTrigger.getFromString( triggerStr );
				final Input input = new Input( trigger, behaviour, contexts );

				Set< Input > inputs = actionToInputsMap.get( input.behaviour );
				if ( inputs == null )
				{
					inputs = new HashSet< >();
					actionToInputsMap.put( input.behaviour, inputs );
				}
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
				for ( final InputTrigger trigger : triggers )
					map.put( trigger, behaviourName );
			}
			else if ( defaultTriggers.length > 0 )
			{
				for ( final InputTrigger trigger : defaultTriggers )
					map.put( trigger, behaviourName );
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
			this.contexts = new HashSet< String >();
			this.contexts.addAll( Arrays.asList( contexts ) );
		}

		@Override
		public void put( final String actionName, final KeyStroke... defaultKeyStrokes )
		{
			final Set< InputTrigger > triggers = config.getInputs( actionName, contexts );
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
					map.put( keyStroke, actionName );
			}
			else
			{
				System.err.println( "Could not assign KeyStroke for \"" + actionName + "\". Nothing defined in InputTriggerConfig, and no default given." );
			}
		}

		@Override
		public void put( final String actionName, final String... defaultKeyStrokes )
		{
			final KeyStroke[] keyStrokes = new KeyStroke[ defaultKeyStrokes.length ];
			int i = 0;
			for ( final String s : defaultKeyStrokes )
				keyStrokes[ i++ ] = KeyStroke.getKeyStroke( s );
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
				final Set< String > contexts )
		{
			this.trigger = trigger;
			this.behaviour = behaviour;
			this.contexts = new HashSet<>( contexts );
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
			if ( obj == null && !( obj instanceof Input ) )
				return false;
			final Input i = ( Input ) obj;
			return i.trigger.equals( trigger ) && i.behaviour.equals( behaviour ) && i.contexts.equals( contexts );
		}

		InputTriggerDescription getDescription()
		{
			return new InputTriggerDescription( new String[] { trigger.toString() }, behaviour, contexts.toArray( new String[ 0 ] ) );
		}
	}

	Set< InputTrigger > getInputs( final String behaviourName, final Set< String > contexts )
	{
		final Set< Input > inputs = actionToInputsMap.get( behaviourName );
		final Set< InputTrigger > triggers = new HashSet<>();
		if ( inputs != null )
		{
			for ( final Input input : inputs )
				if ( ! Collections.disjoint( contexts, input.contexts ) )
					triggers.add( input.trigger );
		}
		return triggers;
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
				Set< Input > inputs = actionToInputsMap.get( behaviourName );
				if ( inputs == null )
				{
					inputs = new HashSet<>();
					actionToInputsMap.put( behaviourName, inputs );
				}

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

			Set< Input > inputs = actionToInputsMap.get( behaviourName );
			if ( inputs == null )
			{
				inputs = new HashSet<>();
				actionToInputsMap.put( behaviourName, inputs );
			}

			boolean added = false;
			for ( final Input input : inputs )
			{
				if ( input.trigger.equals( trigger ) )
				{
					/*
					 * the trigger -> behavioiur binding already exists.
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
