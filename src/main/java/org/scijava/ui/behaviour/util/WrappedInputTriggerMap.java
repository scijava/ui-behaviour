package org.scijava.ui.behaviour.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.behaviour.InputTriggerMap;

/**
 * A view of the specified {@link InputTriggerMap} that can have its own parent.
 * The wrapped {@code InputTriggerMap} should not have a parent!?
 */
public class WrappedInputTriggerMap extends InputTriggerMap
{
	private final InputTriggerMap inputTriggerMap;

	private InputTriggerMap parent;

	private int expectedParentModCount;

	public WrappedInputTriggerMap( final InputTriggerMap inputTriggerMap )
	{
		this.inputTriggerMap = inputTriggerMap;
		parent = null;
		expectedParentModCount = 0;
	}

	@Override
	public void setParent( final InputTriggerMap map )
	{
		parent = map;
		if ( map != null )
			expectedParentModCount = parent.modCount();
	}

	@Override
	public InputTriggerMap getParent()
	{
		return parent;
	}

	@Override
	public synchronized void put( final InputTrigger inputTrigger, final String behaviourKey )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized Set< String > get( final InputTrigger inputTrigger )
	{
		Set< String > keys = null;
		if ( parent != null )
			keys = parent.get( inputTrigger );
		else
			keys = new HashSet<>();
		keys.addAll( inputTriggerMap.get( inputTrigger ) );
		return keys;
	}

	@Override
	public synchronized void remove( final InputTrigger inputTrigger, final String behaviourKey )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized void removeAll( final InputTrigger inputTrigger )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized void clear()
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public synchronized Map< InputTrigger, Set< String > > getAllBindings()
	{
		final Map< InputTrigger, Set< String > > allBindings;
		if ( parent != null )
			allBindings = parent.getAllBindings();
		else
			allBindings = new HashMap<>();

		for ( final Map.Entry< InputTrigger, Set< String > > entry : inputTriggerMap.getAllBindings().entrySet() )
		{
			final InputTrigger inputTrigger = entry.getKey();
			if ( entry.getValue() == null || entry.getValue().isEmpty() )
				continue;

			final Set< String > behaviourKeys = allBindings.computeIfAbsent( inputTrigger, k -> new HashSet<>() );
			behaviourKeys.addAll( entry.getValue() );
		}

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
				inputTriggerMap.remove( null, null ); // hack to bump
														// inputTriggerMap.modCount
			}
		}
		return inputTriggerMap.modCount();
	}
}
