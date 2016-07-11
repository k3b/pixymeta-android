/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package pixy.image.jpeg;

import java.io.IOException;

import pixy.util.Reader;

/**
 * JPEG JPG_SEGMENT_COMMNENTS_COM jpegSegment reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/11/2013
 */
public class COMReader implements Reader {

	private JpegSegment jpegSegment;
	private String comment;
	
	public COMReader(JpegSegment jpegSegment) throws IOException {
		//
		if(jpegSegment.getJpegSegmentMarker() != JpegSegmentMarker.JPG_SEGMENT_COMMNENTS_COM) {
			throw new IllegalArgumentException("Not a valid JPG_SEGMENT_COMMNENTS_COM jpegSegment!");
		}
		
		this.jpegSegment = jpegSegment;
		read();
	}
	
	public String getComment() {
		return this.comment;
	}
	
	public void read() throws IOException {
		this.comment = new String(jpegSegment.getData()).trim();
	}
}
