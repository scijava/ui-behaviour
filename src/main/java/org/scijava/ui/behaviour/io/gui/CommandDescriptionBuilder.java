package org.scijava.ui.behaviour.io.gui;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that is used to build a 2-level map of existing contexts ->
 * commands and their description.
 * 
 * @author Jean-Yves Tinevez
 *
 */
public class CommandDescriptionBuilder
{

	/**
	 * The map of commands -> map of contexts -> description of what the command
	 * do in a context.
	 */
	private HashMap< String, Map< String, String > > map;

	public CommandDescriptionBuilder()
	{
		this.map = new HashMap<>();
	}

	public CommandDescriptionBuilder addCommand( final String command, final String context, final String description )
	{
		Map< String, String > contextMap = map.get( command );
		if ( contextMap == null )
		{
			contextMap = new HashMap<>();
			map.put( command, contextMap );
		}
		contextMap.put( context, description );
		return this;
	}

	/**
	 * Returns the map of commands -> map of contexts -> description of what the
	 * command does in a context.
	 * 
	 * @return a new immutable map
	 */
	public Map< String, Map< String, String > > get()
	{
		return Collections.unmodifiableMap( map );
	}

}
