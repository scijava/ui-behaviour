package bdv.behaviour.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.InputMap;

import bdv.behaviour.InputTriggerMap;
import bdv.behaviour.io.InputTriggerConfig.Input;

/**
 * Creates {@link InputTriggerDescription}s from existing {@link InputMap}s and {@link InputTriggerMap}s.
 * This can be used to dump the current key and mouse configuration of an application to a config file.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class InputTriggerDescriptionsBuilder
{
	private final InputTriggerConfig config;

	public InputTriggerDescriptionsBuilder()
	{
		config = new InputTriggerConfig();
	}

	public List< InputTriggerDescription > getDescriptions()
	{
		final ArrayList< InputTriggerDescription > descs = new ArrayList<>();
		final String[] emptyStringArray = new String[ 0 ];

		for ( final Entry< String, Set< Input > > entry : config.actionToInputsMap.entrySet() )
		{
			final Set< Input > inputs = entry.getValue();
			for ( final Input input : inputs )
			{
				boolean found = false;
				for ( final InputTriggerDescription desc : descs )
				{
					if ( input.behaviour.equals( desc.getAction() ) &&
							input.contexts.equals( new HashSet<>( Arrays.asList( desc.getContexts() ) ) ) )
					{
						final HashSet< String > triggers = new HashSet<>( Arrays.asList( desc.getTriggers() ) );
						triggers.add( input.trigger.toString() );
						desc.setTriggers( triggers.toArray( emptyStringArray ) );
						found = true;
						break;
					}
				}
				if ( !found )
				{
					final InputTriggerDescription desc = new InputTriggerDescription(
							new String[] { input.trigger.toString() },
							input.behaviour,
							input.contexts.toArray( emptyStringArray ) );
					descs.add( desc );
				}
			}
		}

		return descs;
	}

	public void addMap( final InputTriggerMap map, final String context )
	{
		config.addMap( map, context );
	}

	public void addMap( final InputMap map, final String context )
	{
		config.addMap( map, context );
	}
}
