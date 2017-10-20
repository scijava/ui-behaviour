package org.scijava.ui.behaviour.util;

import java.util.HashMap;
import java.util.Map;

import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.BehaviourMap;

/**
 * A view of the specified {@link BehaviourMap} that can have its own parent.
 * The wrapped {@code BehaviourMap} should not have a parent!?
 */
public class WrappedBehaviourMap extends BehaviourMap
{
	private final BehaviourMap behaviourMap;

	private BehaviourMap parent;

	private int expectedParentModCount;

	public WrappedBehaviourMap( final BehaviourMap behaviourMap )
	{
		this.behaviourMap = behaviourMap;
		parent = null;
		expectedParentModCount = 0;
	}

	@Override
	public void setParent( final BehaviourMap map )
	{
		parent = map;
		if ( map != null )
			expectedParentModCount = parent.modCount();
	}

	@Override
	public BehaviourMap getParent()
	{
		return parent;
	}

	@Override
	public synchronized void put( final String key, final Behaviour behaviour )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized Behaviour get( final String key )
	{
		final Behaviour behaviour = behaviourMap.get( key );
		if ( behaviour == null && parent != null )
			return parent.get( key );
		else
			return behaviour;
	}

	@Override
	public synchronized void remove( final String key )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized void clear()
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized Map< String, Behaviour > getAllBindings()
	{
		final Map< String, Behaviour > allBindings = ( parent == null ) ? new HashMap<>() : parent.getAllBindings();

		for ( final Map.Entry< String, Behaviour > entry : behaviourMap.getAllBindings().entrySet() )
			allBindings.put( entry.getKey(), entry.getValue() );

		return allBindings;
	}

	@Override
	public int modCount()
	{
		if ( parent != null )
		{
			final int m = parent.modCount();
			if ( m != expectedParentModCount )
			{
				expectedParentModCount = m;
				behaviourMap.remove( null ); // hack to bump
												// behaviourMap.modCount
			}
		}
		return behaviourMap.modCount();
	}
}
