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
import java.util.EnumSet;

import pixy.io.IOUtils;
import pixy.util.Reader;

/**
 * JPEG SOF jpegSegment reader
 *  
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/09/2013
 */
public class SOFReader implements Reader {

	private int precision;
	private int frameHeight;
	private int frameWidth;
	private int numOfComponents;
	private Component[] components;
	private static final EnumSet<JpegSegmentMarker> SOFS =
			EnumSet.of(JpegSegmentMarker.SOF0, JpegSegmentMarker.SOF1, JpegSegmentMarker.SOF2, JpegSegmentMarker.SOF3, JpegSegmentMarker.SOF5,
		               JpegSegmentMarker.SOF6, JpegSegmentMarker.SOF7, JpegSegmentMarker.SOF9, JpegSegmentMarker.SOF10, JpegSegmentMarker.SOF11,
		               JpegSegmentMarker.SOF13, JpegSegmentMarker.SOF14, JpegSegmentMarker.SOF15)
	        ;
	
	private JpegSegment jpegSegment;
	
	public SOFReader(JpegSegment jpegSegment) throws IOException {
		//
		if(!SOFS.contains(jpegSegment.getJpegSegmentMarker())) {
			throw new IllegalArgumentException("Not a valid SOF jpegSegment: " + jpegSegment.getJpegSegmentMarker());
		}
		
		this.jpegSegment = jpegSegment;
		read();
	}
	
	public int getLength() {
		return jpegSegment.getLength();
	}
	
	public int getPrecision() {
		return precision;
	}
	
	public int getFrameHeight() {
		return frameHeight;
	}
	
	public int getFrameWidth() {
		return frameWidth;
	}
	
	public int getNumOfComponents() {
		return numOfComponents;
	}
	
	public Component[] getComponents() {
		return components.clone();
	}
	
	public void read() throws IOException {
		//
		byte[] data = jpegSegment.getData();
		// This is in bits/sample, usually 8, (12 and 16 not supported by most software). 
		precision = data[0]; // Usually 8, for baseline JPEG
		// Image frame width and height
		frameHeight = IOUtils.readUnsignedShortMM(data, 1);
		frameWidth = IOUtils.readUnsignedShortMM(data, 3);
		 // Number of components
		// Usually 1 = grey scaled, 3 = color YCbCr or YIQ, 4 = color CMYK 
        // JFIF uses either 1 component (Y, greyscaled) or 3 components (YCbCr, sometimes called YUV, color).
		numOfComponents = data[5];
		components = new Component[numOfComponents];
	
		int offset = 6;
		
		for (int i = 0; i < numOfComponents; i++) {
			byte componentId = data[offset++];
			// Sampling factors (1byte) (bit 0-3 horizontal, 4-7 vertical).
			byte sampleFactor = data[offset++];
			byte hSampleFactor = (byte)((sampleFactor>>4)&0x0f);
			byte vSampleFactor = (byte)((sampleFactor&0x0f));
			byte qTableNumber = data[offset++];
					
			components[i] = new Component(componentId, hSampleFactor, vSampleFactor, qTableNumber);
		}
	}
}