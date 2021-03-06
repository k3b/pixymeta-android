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

import pixy.util.Builder;

/**
 * Base builder for JPEG segments.
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 10/11/2013
 */
public abstract class SegmentBuilder implements Builder<JpegSegment> {
	//
	private final JpegSegmentMarker jpegSegmentMarker;
	
	public SegmentBuilder(JpegSegmentMarker jpegSegmentMarker) {
		this.jpegSegmentMarker = jpegSegmentMarker;
	}
		
	public final JpegSegment build() {
		byte[] data = buildData();
		
		return new JpegSegment(jpegSegmentMarker, data);
	}
	
	protected abstract byte[] buildData();
}