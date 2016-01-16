package bdv.behaviour.io.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bdv.behaviour.io.InputTriggerDescription;

public class JSonInteractiveTest
{
	public static void main( final String[] args ) throws IOException
	{
		final String fileName = "jsontest.txt";

		final File file = new File( fileName );
		if ( file.exists() )
		{
			System.out.println( "File " + file + " found. Reading from it." );
			final List< InputTriggerDescription > list = JsonConfigIO.read( fileName );
			System.out.println( list );
		}
		
		/*
		 * TEST WRITE
		 */

		System.out.println( "\nWriting to file " + fileName );

		final ArrayList< InputTriggerDescription > keyMappings = new ArrayList< InputTriggerDescription >();
		keyMappings.add( new InputTriggerDescription( new String[] { "button1" }, "ts select vertex", "trackscheme" ) );
		keyMappings.add( new InputTriggerDescription( new String[] { "button2" }, "ts select edge", "bdv", "trackscheme" ) );
		keyMappings.add( new InputTriggerDescription( new String[] { "A" }, "navigate", "bdv", "trackscheme" ) );
		keyMappings.add( new InputTriggerDescription( new String[] { "B" }, "sleep", "" ) );
		JsonConfigIO.write( keyMappings, file.getAbsolutePath() );

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
		final List< InputTriggerDescription > list = JsonConfigIO.read( fileName );
		System.out.println( list );
		
	}

}
