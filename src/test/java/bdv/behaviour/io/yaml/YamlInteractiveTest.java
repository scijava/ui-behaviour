package bdv.behaviour.io.yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bdv.behaviour.io.InputTriggerDescription;

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

		final ArrayList< InputTriggerDescription > keyMappings = new ArrayList< InputTriggerDescription >();
		keyMappings.add( new InputTriggerDescription( "button1", "ts select vertex", "trackscheme" ) );
		keyMappings.add( new InputTriggerDescription( "button2", "ts select edge", "bdv", "trackscheme" ) );
		keyMappings.add( new InputTriggerDescription( "A", "navigate", "bdv", "trackscheme" ) );
		keyMappings.add( new InputTriggerDescription( "B", "sleep", "" ) );
		YamlConfigIO.write( keyMappings.iterator(), "yamltest.txt" );

		System.out.println( "\nContent of file:" );

		BufferedReader reader =  new BufferedReader(new FileReader( fileName ));
		String line = reader.readLine();
		while ( line != null )
		{
			System.out.println( line );
			line = reader.readLine();
		}
		reader.close();

		/*
		 * TEST APPEND
		 */

		System.out.println("\nAppending to file " + fileName);
		
		final ArrayList< InputTriggerDescription > keyMappings2 = new ArrayList< InputTriggerDescription >();
		keyMappings2.add( new InputTriggerDescription( "ctrl X", "cut", "" ) );
		keyMappings2.add( new InputTriggerDescription( "ctrl V", "paste", "" ) );
		FileWriter writer = new FileWriter( fileName, true );
		YamlConfigIO.write( keyMappings2.iterator(), writer );
		writer.close();
		

		System.out.println( "\nContent of file:" );
		reader =  new BufferedReader(new FileReader( fileName ));
		line = reader.readLine();
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
		
		System.out.println("\nHeterogeneous data.");
		
		writer = new FileWriter( fileName, true );
		
		// Write a tag that is not handled
		writer.write( "--- !display\n" );
		writer.write( "brightness: 100\n" );
		writer.write( "contrast: 50\n" );
		
		// Write a tag with a mistake
		writer.write( "--- !mapping\n" );
		writer.write( "acton: paste\n");
		writer.write( "contexts: []\n" );
		writer.write( "key: ctrl C\n");
		
		// Write a tag with a serious problem
		writer.write( "--- !mapping\n" );
		writer.write( "action: pastecontexts: []\n" );
		writer.write( "key: ctrl C\n");

		// The write something legit.
		writer.write( "--- !mapping\n" );
		writer.write( "action: zap2\n");
		writer.write( "contexts: []\n" );
		writer.write( "key: Z2\n");
		
		writer.close();
		
		System.out.println( "\nReading from file:" );
		list = YamlConfigIO.read( fileName );
		System.out.println( list );
		
	}

}
