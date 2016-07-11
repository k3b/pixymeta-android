/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * Change History - most recent changes go on top of previous changes
 *
 * JpegSegment.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    16Mar2015  Changed write() to work with stand-alone segments
 */

package pixy.image.jpeg;

import java.io.IOException;
import java.io.OutputStream;

import pixy.io.IOUtils;

/**
 * JPEG segment. It may contain Exif, IPTC, XMP or other data
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/21/2013
 */
public class JpegSegment {

	private JpegSegmentMarker jpegSegmentMarker;
	private byte[] data;
	private int padding = 0; // number of oxff behind segment
	
	public JpegSegment(JpegSegmentMarker jpegSegmentMarker, byte[] data) {
		this.jpegSegmentMarker = jpegSegmentMarker;
		this.data = data;
	}
	
	public JpegSegmentMarker getJpegSegmentMarker() {
		return jpegSegmentMarker;
	}
	
	public int getLength() {
		if (data == null) return 2;
		return data.length +2;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void write(OutputStream os) throws IOException {
		IOUtils.writeShortMM(os, jpegSegmentMarker.getValue());
		// If this is not a stand-alone segment, write the content as well
		if(getLength() > 0) {
			IOUtils.writeShortMM(os, getLength());
			IOUtils.write(os, data);
		}
		if(padding > 0) {
			byte[] paddingData = new byte[padding];
			for(int i = 0; i < padding; i++) paddingData[i] = (byte) 0xff;
			IOUtils.write(os, paddingData);
		}
	}
	
	public int getPadding() {
		return padding;
	}

	public JpegSegment setPadding(int padding) {
		this.padding = padding;
		return this;
	}

	public JpegSegment addPadding(int padding) {
		this.padding += padding;
		return this;
	}

	@Override public String toString() {
		return "" + jpegSegmentMarker + "; len " + getLength() + ", pad " + padding;
	}
}