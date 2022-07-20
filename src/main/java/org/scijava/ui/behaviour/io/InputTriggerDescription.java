/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2022 Max Planck Institute of Molecular Cell Biology
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

import java.util.Arrays;
import javax.swing.Action;

import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.InputTrigger;

/**
 * IO record describing the mapping of one {@link InputTrigger} to an
 * {@link Action} or a {@link Behaviour}.
 *
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class InputTriggerDescription
{
	/**
	 * String representations of the {@link InputTrigger}s.
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
			final String[] triggers,
			final String action,
			final String... contexts )
	{
		this.triggers = triggers;
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

	public void addTrigger( final String trigger )
	{
		if ( !Arrays.asList( triggers ).contains( trigger ) )
		{
			triggers = Arrays.copyOf( triggers, triggers.length + 1 );
			triggers[ triggers.length - 1 ] = trigger;
		}
	}
}
