package org.scijava.ui.behaviour.util;

import javax.swing.Action;
import javax.swing.ActionMap;

/**
 * A view of the specified {@link ActionMap} that can have its own parent. The
 * wrapped {@code ActionMap} should not have a parent!?
 */
public class WrappedActionMap extends ActionMap
{
	private static final long serialVersionUID = 1L;

	private final ActionMap actionMap;

	private ActionMap parent;

	public WrappedActionMap( final ActionMap inputMap )
	{
		this.actionMap = inputMap;
	}

	@Override
	public void setParent( final ActionMap map )
	{
		parent = map;
	}

	@Override
	public ActionMap getParent()
	{
		return parent;
	}

	@Override
	public void put( final Object key, final Action action )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public Action get( final Object key )
	{
		final Action value = actionMap.get( key );
		if ( value == null )
		{
			final ActionMap parent = getParent();
			if ( parent != null )
				return parent.get( key );
		}
		return value;
	}

	@Override
	public void remove( final Object key )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public void clear()
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public Object[] keys()
	{
		return actionMap.keys();
	}

	@Override
	public int size()
	{
		return actionMap.size();
	}
}
