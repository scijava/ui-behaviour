package bdv.behaviour.io.yaml;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.scanner.ScannerException;

import bdv.behaviour.io.InputTriggerDescription;

/**
 * Facilities to serialize / de-serialize {@link InputTriggerDescription}s to a
 * YAML file.
 * <p>
 * The YAML file will have this shape:
 *
 * <pre>
 * --- !mapping
 * action: ts select vertex
 * contexts: [trackscheme]
 * trigger: button1
 * --- !mapping
 * action: ts select edge
 * contexts: [bdv, trackscheme]
 * trigger: button2
 * --- !mapping
 * action: navigate
 * contexts: [bdv, trackscheme]
 * trigger: A
 * --- !mapping
 * action: sleep
 * contexts: []
 * trigger: B
 * </pre>
 *
 * @author Jean-Yves Tinevez.
 *
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
	 * <p>
	 * If the specified writer is configured to append to the stream, this method
	 * can be used to append configuration items to a configuration file created elsewhere.
	 *
	 * @param descriptions
	 *            an iterator over the mapping description to write.
	 * @param writer
	 *            the writer. Is not closed after this method returns.
	 */
	public static void write( final Iterator< InputTriggerDescription > descriptions, final Writer writer )
	{
		final Yaml yaml = getYaml();
		yaml.dumpAll( descriptions, writer );
	}

	/**
	 * Writes the specified {@link InputTriggerDescription}s on the specified
	 * file.
	 *
	 * @param descriptions
	 *            an iterator over the mapping description to write.
	 * @param fileName
	 *            the system-dependent filename.
	 * @throws IOException
	 *             if the named file exists but is a directory rather than a
	 *             regular file, does not exist but cannot be created, or cannot
	 *             be opened for any other reason
	 */
	public static void write( final Iterator< InputTriggerDescription > descriptions, final String fileName ) throws IOException
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
	 * @param reader
	 *            the reader to read from. Is not closed after this method
	 *            returns.
	 * @return a new list containing the {@link InputTriggerDescription}s found in
	 *         the stream. Other objects are ignored.
	 */
	public static List< InputTriggerDescription > read( final Reader reader )
	{
		final Yaml yaml = getYaml();
		final Iterator< Object > it = yaml.loadAll( reader ).iterator();
		final List< InputTriggerDescription > descriptions = new ArrayList< InputTriggerDescription >();
		try
		{
			while ( it.hasNext() )
			{
				try
				{
					final Object obj = it.next();
					if ( obj instanceof InputTriggerDescription )
					{
						final InputTriggerDescription mapping = ( InputTriggerDescription ) obj;
						if ( null == mapping.getAction())
						{
							System.err.println( "[YamlConfigIO] Missing action definition for mapping:\n" + mapping + "- ignored." );
							continue;
						}
						if ( null == mapping.getContexts() )
						{
							System.err.println( "[YamlConfigIO] Missing contexts definition for mapping:\n" + mapping + "- ignored." );
							continue;
						}
						if ( null == mapping.getTrigger() )
						{
							System.err.println( "[YamlConfigIO] Missing trigger definition for mapping:\n" + mapping + "- ignored." );
							continue;
						}

						descriptions.add( mapping );
					}
				}
				catch ( final ConstructorException ce )
				{
					System.err.println( "[YamlConfigIO] Unkown element at line " + ( ce.getProblemMark().getLine() + 1 ) + ", ignored:" );
					System.err.println( ce.getProblemMark().get_snippet() );
				}
				catch ( final ScannerException se )
				{
					System.err.println( "[YamlConfigIO] Unable to read element at line " + ( se.getProblemMark().getLine() + 1 ) + ", ignored:" );
					System.err.println( se.getProblemMark().get_snippet() );
				}
			}
		}
		catch ( final ScannerException se )
		{
			System.err.println( "[YamlConfigIO] Unable to scan file for value around line " + ( se.getProblemMark().getLine() + 1 ) + ", aborting:" );
			System.err.println( se.getProblemMark().get_snippet() );
		}
		return descriptions;
	}

	/**
	 * Reads from the specified file and returns the list of serialized
	 * {@link InputTriggerDescription}s that are found in the file.
	 *
	 * @param fileName
	 *            the system-dependent filename.
	 * @return a new list containing the {@link InputTriggerDescription}s found in
	 *         the file. Other objects are ignored.
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
