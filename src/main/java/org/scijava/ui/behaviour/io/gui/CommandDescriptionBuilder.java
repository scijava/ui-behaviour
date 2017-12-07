package org.scijava.ui.behaviour.io.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that is used to build a map of {@link Command}s to their
 * description.
 *
 * @author Jean-Yves Tinevez
 */
public class CommandDescriptionBuilder
{

	/**
	 * The map of {@link Command} to description of what the command does.
	 */
	private final HashMap< Command, String > map;

	public CommandDescriptionBuilder()
	{
		this.map = new HashMap<>();
	}

	public CommandDescriptionBuilder addCommand( final String name, final String context, final String description )
	{
		final Command command = new Command( name, context );
		map.put( command, description );
		return this;
	}

	/**
	 * Returns the map of {@link Command} to description of what the command
	 * does.
	 *
	 * @return a new immutable map
	 */
	public Map< Command, String > get()
	{
		return Collections.unmodifiableMap( map );
	}

}
