package org.scijava.ui.behaviour.util;

import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;

public abstract class AbstractNamedBehaviour implements Behaviour
{
	private final String name;

	public AbstractNamedBehaviour( final String name )
	{
		this.name = name;
	}

	public String name()
	{
		return name;
	}

	public static void put( final BehaviourMap map, final AbstractNamedBehaviour a )
	{
		map.put( a.name(), a );
	}

	public static class NamedBehaviourAdder
	{
		private final BehaviourMap map;

		public NamedBehaviourAdder( final BehaviourMap map )
		{
			this.map = map;
		}

		public void put( final AbstractNamedBehaviour a )
		{
			AbstractNamedBehaviour.put( map, a );
		}
	}
}
