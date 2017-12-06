package org.scijava.ui.behaviour.io.gui;

/**
 * Behaviour or action name in a context.
 */
public class Command
{
	private final String name;

	private final String context;

	public Command( final String name, final String context )
	{
		if ( name == null || context == null )
			throw new IllegalArgumentException();
		this.name = name;
		this.context = context;
	}

	public String getName()
	{
		return name;
	}

	public String getContext()
	{
		return context;
	}

	@Override
	public boolean equals( final Object o )
	{
		if ( this == o )
			return true;
		if ( o == null || getClass() != o.getClass() )
			return false;

		final Command command = ( Command ) o;

		if ( !name.equals( command.name ) )
			return false;
		return context.equals( command.context );
	}

	@Override
	public int hashCode()
	{
		int result = name.hashCode();
		result = 31 * result + context.hashCode();
		return result;
	}
}
