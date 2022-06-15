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
package org.scijava.ui.behaviour.io.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

import javax.swing.border.LineBorder;

public class RoundBorder extends LineBorder
{

	private static final long serialVersionUID = 1L;
	private final Component parent;

	public RoundBorder( final Color color, final Component parent, final int arc )
	{
		super( color, arc );
		this.parent = parent;
	}

	@Override
	public void paintBorder( final Component c, final Graphics g, final int x, final int y, final int width, final int height )
	{
		if ( ( this.thickness > 0 ) && ( g instanceof Graphics2D ) )
		{
			final Graphics2D g2d = ( Graphics2D ) g;
			final Object oldRenderingHint = g2d.getRenderingHint( RenderingHints.KEY_ANTIALIASING );
			final Color oldColor = g2d.getColor();
			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

			final float arc = 4f * thickness;
			final Shape inner = new RoundRectangle2D.Float( x, y, width - 1, height - 1, (int) arc, (int) arc );
			final Shape outer = new Rectangle( x, y, width, height );

			g2d.setColor( parent.getBackground() );
			final Area area = new Area(outer);
			area.exclusiveOr( new Area(inner) );
			g2d.fill( area );

			g2d.setColor( this.lineColor );
			g2d.draw( inner );

			g2d.setColor( oldColor );
			g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING, oldRenderingHint );
		}
	}

}
