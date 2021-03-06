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
 * TIFF FieldType.FLOAT wrapper
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 12/04/2014
 */
public class FloatField extends ExifField<float[]> {

	public FloatField(pixy.meta.exif.Tag tag, float[] data) {
		super(tag, pixy.image.exifFields.FieldType.FLOAT, data.length);
		this.data = data;
	}
	
	public float[] getData() {
		return data.clone();
	}

	@Override
	public String getDataAsString() {
		return Arrays.toString(data);
	}

	// [var, var, var]
	@Override
	public void setValue(String value) {
		data = StringUtils.parseFloatList(value);
	}

	@Override
	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
		if (data.length == 1) {
			dataOffset = (int)os.getStreamPointer();
			os.writeFloat(data[0]);
		} else {
			dataOffset = toOffset;
			os.writeInt(toOffset);
			os.seek(toOffset);
			
			for (float value : data)
				os.writeFloat(value);
			
			toOffset += (data.length << 2);
		}
		return toOffset;
	}

}
