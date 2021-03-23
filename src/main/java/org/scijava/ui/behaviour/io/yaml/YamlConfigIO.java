/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2021 Max Planck Institute of Molecular Cell Biology
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
package org.scijava.ui.behaviour.io.yaml;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.scijava.ui.behaviour.io.InputTriggerDescription;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Facilities to serialize / de-serialize {@link InputTriggerDescription}s to a
 * YAML file.
 * <p>
 * The YAML file will have this shape:
 *
 * <pre>
 * ---
 *- !mapping
 *  action: ts select vertex
 *  contexts: [trackscheme]
 *  trigger: button1
 *- !mapping
 *  action: ts select edge
 *  contexts: [bdv, trackscheme]
 *  trigger: button2
 *- !mapping
 *  action: navigate
 *  contexts: [bdv, trackscheme]
 *  trigger: A
 *- !mapping
 *  action: sleep
 *  contexts: []
 *  trigger: B
 * </pre>
 *
 * @author Jean-Yves Tinevez.
 */
public class YamlConfigIO
{
	private static final Tag tag = new Tag( "!mapping" );

	/**
	 * Returns a new, properly configured YAML instance, able to write and read
	 * key mappings.
	 *
	 * @return a new {@link Yaml} instance.
	 */
	private static final Yaml getYaml()
	{
		final Representer representer = new Representer();
		representer.addClassTag( InputTriggerDescription.class, tag );

		final Constructor constructor = new Constructor();
		constructor.addTypeDescription( new TypeDescription( InputTriggerDescription.class, tag ) );

		final DumperOptions options = new DumperOptions();
		options.setExplicitStart( true );

		final Yaml yaml = new Yaml( constructor, representer, options );
		return yaml;
	}

	/**
	 * Writes the specified {@link InputTriggerDescription}s on the specified
	 * writer.
	 *
	 * @param descriptions
	 *            an iterable of the mapping description to write.
	 * @param writer
	 *            the writer. Is not closed after this method returns.
	 */
	public static void write( final Iterable< InputTriggerDescription > descriptions, final Writer writer )
	{
		final Yaml yaml = getYaml();
		yaml.dump( descriptions, writer );
	}

	/**
	 * Writes the specified {@link InputTriggerDescription}s on the specified
	 * file.
	 *
	 * @param descriptions
	 *            an iterable of the mapping description to write.
	 * @param fileName
	 *            the system-dependent filename.
	 * @throws IOException
	 *             if the named file exists but is a directory rather than a
	 *             regular file, does not exist but cannot be created, or cannot
	 *             be opened for any other reason
	 */
	public static void write( final Iterable< InputTriggerDescription > descriptions, final String fileName ) throws IOException
	{
		final FileWriter writer = new FileWriter( fileName );
		write( descriptions, writer );
		writer.close();
	}

	/**
	 * Reads from the specified reader instance and returns the list of
	 * serialized {@link InputTriggerDescription}s that are found in the input
	 * stream.
	 *
	 * <p>
	 * Malformed serializations generate an error which is echoed on the
	 * console.
	 *
	 * @param reader
	 *            the reader to read from. Is not closed after this method
	 *            returns.
	 * @return a new list containing the {@link InputTriggerDescription}s found
	 *         in the stream. Empty if the serialization is malformed.
	 */
	public static List< InputTriggerDescription > read( final Reader reader )
	{
		final Yaml yaml = getYaml();
		final List< InputTriggerDescription > descriptions = new ArrayList<>();

		try
		{
			final Object obj = yaml.load( reader );
			if ( obj instanceof Iterable )
			{
				final Iterable< ? > raw = ( Iterable< ? > ) obj;
				for ( final Object item : raw )
				{
					if ( item instanceof InputTriggerDescription )
					{
						final InputTriggerDescription mapping = ( InputTriggerDescription ) item;
						if ( null == mapping.getAction() )
						{
							System.err.println( "[YamlConfigIO] Missing action definition for mapping:\n" + mapping + "- ignored." );
							continue;
						}
						if ( null == mapping.getContexts() )
						{
							System.err.println( "[YamlConfigIO] Missing contexts definition for mapping:\n" + mapping + "- ignored." );
							continue;
						}
						if ( null == mapping.getTriggers() )
						{
							System.err.println( "[YamlConfigIO] Missing trigger definition for mapping:\n" + mapping + "- ignored." );
							continue;
						}

						descriptions.add( mapping );
					}
				}
			}
		}
		catch ( final ParserException pse )
		{
			System.err.println( "Problem reading data:" );
			System.err.println( pse.getProblemMark() );
		}
		return descriptions;
	}

	/**
	 * Reads from the specified file and returns the list of serialized
	 * {@link InputTriggerDescription}s that are found in the file.
	 *
	 * @param fileName
	 *            the system-dependent filename.
	 * @return a new list containing the {@link InputTriggerDescription}s found
	 *         in the file. Empty if the file is malformed.
	 * @throws IOException
	 *             if an I/O error occurs, if the named file does not exist, is
	 *             a directory rather than a regular file, or for some other
	 *             reason cannot be opened for reading.
	 */
	public static List< InputTriggerDescription > read( final String fileName ) throws IOException
	{
		final FileReader reader = new FileReader( fileName );
		final List< InputTriggerDescription > descriptions = read( reader );
		reader.close();
		return descriptions;
	}

	private YamlConfigIO()
	{}
}
