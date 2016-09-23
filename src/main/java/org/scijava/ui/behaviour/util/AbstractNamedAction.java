package org.scijava.ui.behaviour.util;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;

public abstract class AbstractNamedAction extends AbstractAction
{
	public AbstractNamedAction( final String name )
	{
		super( name );
	}

	public String name()
	{
		return ( String ) getValue( NAME );
	}

	public void put( final ActionMap map )
	{
		map.put( name(), this );
	}

	private static final long serialVersionUID = 1L;
}
