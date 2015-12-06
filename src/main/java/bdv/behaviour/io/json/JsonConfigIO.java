package bdv.behaviour.io.json;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.List;

import bdv.behaviour.io.InputTriggerDescription;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class JsonConfigIO
{
	public static void write( final List< InputTriggerDescription > descriptions, final Writer writer )
	{
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		gson.toJson( descriptions, writer );
	}

	public static void write( final List< InputTriggerDescription > descriptions, final String fileName ) throws IOException
	{
		final FileWriter writer = new FileWriter( fileName );
		write( descriptions, writer );
		writer.close();
	}

	public static List< InputTriggerDescription > read( final Reader reader )
	{
		final Gson gson = new GsonBuilder().create();
		final Type type = new TypeToken< List< InputTriggerDescription > >(){}.getType();
		final List< InputTriggerDescription > descriptions = gson.fromJson( reader, type );
		return descriptions;
	}

	public static List< InputTriggerDescription > read( final String fileName ) throws IOException
	{
		final FileReader reader = new FileReader( fileName );
		final List< InputTriggerDescription > descriptions = read( reader );
		reader.close();
		return descriptions;
	}
}
