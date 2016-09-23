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

	public void put( final BehaviourMap map )
	{
		map.put( name(), this );
	}
}
