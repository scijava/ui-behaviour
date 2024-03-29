/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2023 Max Planck Institute of Molecular Cell Biology
 * and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package org.scijava.ui.behaviour.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.InputMap;

import org.scijava.ui.behaviour.InputTriggerMap;
import org.scijava.ui.behaviour.io.InputTriggerConfig.Input;

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
		this( new InputTriggerConfig() );
	}

	public InputTriggerDescriptionsBuilder( final InputTriggerConfig config )
	{
		this.config = config;
	}

	public List< InputTriggerDescription > getDescriptions()
	{
		final ArrayList< InputTriggerDescription > descs = new ArrayList<>();

		for ( final Set< Input > inputs : config.actionToInputsMap.values() )
		{
			for ( final Input input : inputs )
			{
				boolean found = false;
				for ( final InputTriggerDescription desc : descs )
				{
					if ( input.behaviour.equals( desc.getAction() ) &&
							input.contexts.equals( new HashSet<>( Arrays.asList( desc.getContexts() ) ) ) )
					{
						desc.addTrigger( input.trigger.toString() );
						found = true;
						break;
					}
				}
				if ( !found )
				{
					descs.add( input.getDescription() );
				}
			}
		}

		return descs;
	}

	public Set< String > getContexts()
	{
		final Set< String > contexts = new LinkedHashSet<>();
		for ( final Entry< String, Set< Input > > entry : config.actionToInputsMap.entrySet() )
			for ( final Input input : entry.getValue() )
				contexts.addAll( input.contexts );
		return contexts;
	}

	public Set< String > getBehaviourNames()
	{
		return new LinkedHashSet<>( config.actionToInputsMap.keySet() );
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
