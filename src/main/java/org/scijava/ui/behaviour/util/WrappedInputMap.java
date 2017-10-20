package org.scijava.ui.behaviour.util;

import javax.swing.InputMap;
import javax.swing.KeyStroke;

/**
 * A view of the specified {@link InputMap} that can have its own parent. The
 * wrapped {@code InputMap} should not have a parent!?
 */
public class WrappedInputMap extends InputMap
{
	private static final long serialVersionUID = 1L;

	private final InputMap inputMap;

	private InputMap parent;

	public WrappedInputMap( final InputMap inputMap )
	{
		this.inputMap = inputMap;
	}

	@Override
	public void setParent( final InputMap map )
	{
		parent = map;
	}

	@Override
	public InputMap getParent()
	{
		return parent;
	}

	@Override
	public void put( final KeyStroke keyStroke, final Object actionMapKey )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public Object get( final KeyStroke keyStroke )
	{
		final Object value = inputMap.get( keyStroke );
		if ( value == null )
		{
			final InputMap parent = getParent();
			if ( parent != null )
				return parent.get( keyStroke );
		}
		return value;
	}

	@Override
	public void remove( final KeyStroke key )
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public void clear()
	{
		throw new UnsupportedOperationException( getClass().getSimpleName() + " cannot be modified." );
	}

	@Override
	public KeyStroke[] keys()
	{
		return inputMap.keys();
	}

	@Override
	public int size()
	{
		return inputMap.size();
	}
}
