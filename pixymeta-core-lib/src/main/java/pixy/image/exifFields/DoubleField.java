/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package pixy.image.exifFields;

import java.io.IOException;
import java.util.Arrays;

import pixy.io.RandomAccessOutputStream;
import pixy.string.StringUtils;

/**
 * TIFF FieldType.DOUBLE wrapper
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/04/2014
 */
public class DoubleField extends ExifField<double[]> {

	public DoubleField(pixy.meta.exif.Tag tag, double[] data) {
		super(tag, pixy.image.exifFields.FieldType.DOUBLE, data.length);
		this.data = data;
	}
	
	public double[] getData() {
		return data.clone();
	}
	
	public String getDataAsString() {
		return Arrays.toString(data);
	}

	// [var, var, var]
	@Override
	public void setValue(String value) {
		data = StringUtils.parseDoubleList(value);
	}

	@Override
	protected int writeData(RandomAccessOutputStream os, int toOffset)
			throws IOException {
		//
		dataOffset = toOffset;
		os.writeInt(toOffset);
		os.seek(toOffset);
		
		for (double value : data)
			os.writeDouble(value);
		
		toOffset += (data.length << 3);
		
		return toOffset;
	}

}
