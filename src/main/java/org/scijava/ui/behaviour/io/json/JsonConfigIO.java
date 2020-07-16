/*-
 * #%L
 * Configurable key and mouse event handling
 * %%
 * Copyright (C) 2015 - 2020 Max Planck Institute of Molecular Cell Biology
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
package org.scijava.ui.behaviour.io.json;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.List;

import org.scijava.ui.behaviour.io.InputTriggerDescription;

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
