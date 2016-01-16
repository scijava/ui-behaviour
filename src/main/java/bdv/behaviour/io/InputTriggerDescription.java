package bdv.behaviour.io;

import javax.swing.Action;

import bdv.behaviour.Behaviour;
import bdv.behaviour.InputTrigger;

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
	private String[] triggers;

	/**
	 * Key of the {@link Action} or {@link Behaviour}.
	 */
	private String action;

	/**
	 * A list of contexts in which this mapping is active.
	 */
	private String[] contexts;

	public InputTriggerDescription(
			final String[] trigger,
			final String action,
			final String... contexts )
	{
		this.triggers = trigger;
		this.action = action;
		setContexts( contexts );
	}

	public InputTriggerDescription()
	{}

	@Override
	public String toString()
	{
		String s = "( trigger  = [";
		if ( triggers != null )
			for ( int i = 0; i < triggers.length; ++i )
				s += "\"" + triggers[ i ] + "\"" + ( i == triggers.length - 1 ? "" : ", " );
		s += "]\n";

		s += "  action   = \"" + action + "\"\n";

		s += "  contexts = [";
		if ( contexts != null )
			for ( int i = 0; i < contexts.length; ++i )
				s += "\"" + contexts[ i ] + "\"" + ( i == contexts.length - 1 ? "" : ", " );
		s += "] )\n";
		return s;
	}

	public String[] getTriggers()
	{
		return triggers;
	}

	public String getAction()
	{
		return action;
	}

	public String[] getContexts()
	{
		return contexts;
	}

	public void setTriggers( final String[] triggers )
	{
		this.triggers = triggers;
	}

	public void setAction( final String action )
	{
		this.action = action;
	}

	public void setContexts( final String[] contexts )
	{
		if ( contexts == null || ( contexts.length == 1 && contexts[ 0 ].isEmpty() ) )
			this.contexts = new String[ 0 ];
		else
			this.contexts = contexts;
	}
}