package bdv.behaviour.io;

import java.util.ArrayList;
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
		final ArrayList< InputTriggerDescription > descs = new ArrayList< InputTriggerDescription >();

		for ( final Entry< String, Set< Input > > entry : config.actionToInputsMap.entrySet() )
		{
			final Set< Input > inputs = entry.getValue();
			for ( final Input input : inputs )
			{
				final InputTriggerDescription desc = new InputTriggerDescription(
						input.trigger.toString(),
						input.behaviour,
						input.contexts.toArray( new String[ 0 ] ) );
				descs.add( desc );
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