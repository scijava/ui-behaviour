package net.trackmate.revised.trackscheme.display.handler.io;

import javax.swing.Action;

import net.trackmate.revised.trackscheme.display.handler.Behaviour;
import net.trackmate.revised.trackscheme.display.handler.InputTrigger;

/**
 * IO record describing the mapping of one {@link InputTrigger} to an
 * {@link Action} or a {@link Behaviour}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class InputTriggerDescription
{
	/**
	 * String representation of the {@link InputTrigger}.
	 */
	private final String trigger;

	/**
	 * Key of the {@link Action} or {@link Behaviour}.
	 */
	private final String action;

	/**
	 * A list of contexts in which this mapping is active.
	 */
	private final String[] contexts;

	public InputTriggerDescription(
			final String trigger,
			final String action,
			final String... contexts )
	{
		this.trigger = trigger;
		this.action = action;
		this.contexts = contexts;
	}

	@Override
	public String toString()
	{
		String s =
				"( trigger  = \"" + trigger +"\"\n" +
				"  action   = \"" + action +"\"\n" +
				"  contexts = {";
		if ( contexts != null )
			for ( int i = 0; i < contexts.length; ++i )
				s += "\"" + contexts[ i ] + "\"" + ( i == contexts.length - 1 ? "" : ", " );
		s += "} )\n";
		return s;
	}

	public String getTrigger()
	{
		return trigger;
	}

	public String getAction()
	{
		return action;
	}

	public String[] getContexts()
	{
		return contexts;
	}
}