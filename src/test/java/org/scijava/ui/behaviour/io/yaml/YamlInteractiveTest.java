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
package org.scijava.ui.behaviour.io.yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.scijava.ui.behaviour.io.InputTriggerDescription;

public class YamlInteractiveTest
{
	public static void main( final String[] args ) throws IOException
	{
		final String fileName = "yamltest.txt";

		final File file = new File( fileName );
		if ( file.exists() )
		{
			System.out.println( "File " + file + " found. Reading from it." );
			final List< InputTriggerDescription > list = YamlConfigIO.read( fileName );
			System.out.println( list );
		}

		/*
		 * TEST WRITE
		 */

		System.out.println( "\nWriting to file " + fileName );

		final ArrayList< InputTriggerDescription > keyMappings = new ArrayList<>();
		keyMappings.add( new InputTriggerDescription( new String[] { "button1" }, "ts select vertex", "trackscheme" ) );
		keyMappings.add( new InputTriggerDescription( new String[] { "button2" }, "ts select edge", "bdv", "trackscheme" ) );
		keyMappings.add( new InputTriggerDescription( new String[] { "A" }, "navigate", "bdv", "trackscheme" ) );
		keyMappings.add( new InputTriggerDescription( new String[] { "B" }, "sleep", "" ) );
		YamlConfigIO.write( keyMappings, "yamltest.txt" );

		System.out.println( "\nContent of file:" );

		final BufferedReader reader =  new BufferedReader(new FileReader( fileName ));
		String line = reader.readLine();
		while ( line != null )
		{
			System.out.println( line );
			line = reader.readLine();
		}
		reader.close();

		/*
		 * TEST READ
		 */

		System.out.println( "\nReading from file:" );
		List< InputTriggerDescription > list = YamlConfigIO.read( fileName );
		System.out.println( list );

		/*
		 * TEST HETEROGENOUS DATA
		 */

		System.out.println( "\nMalformed data." );

		final FileWriter writer = new FileWriter( fileName, false );

		// Write a tag with a mistake
		writer.write( "- !mapping\n" );
		writer.write( "acton: paste\n");
		writer.write( "contexts: []\n" );
		writer.write( "key: ctrl C\n");

		// Write a tag with a serious problem
		writer.write( "- !mapping\n" );
		writer.write( "action: pastecontexts: []\n" );
		writer.write( "key: ctrl C\n");

		// The write something legit.
		writer.write( "- !mapping\n" );
		writer.write( "action: zap2\n");
		writer.write( "contexts: []\n" );
		writer.write( "key: Z2\n");

		// Write a tag that is not handled
		writer.write( "---\n- !display\n" );
		writer.write( "brightness: 100\n" );
		writer.write( "contrast: 50\n" );

		writer.close();

		System.out.println( "\nReading from file:" );
		list = YamlConfigIO.read( fileName );
		System.out.println( list );

	}

}
