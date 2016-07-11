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

import pixy.api.IIntableField;
import pixy.io.RandomAccessOutputStream;
import pixy.string.StringUtils;

public abstract class AbstractLongField extends ExifField<int[]> implements IIntableField {

	public AbstractLongField(pixy.image.exifFields.Tag tag, pixy.image.exifFields.FieldType fieldType, int[] data) {
		super(tag, fieldType, data.length);
		this.data = data;
	}
	
	public int[] getData() {
		return data.clone();
	}

	@Override
	public int[] getDataAsLong() {
		return getData();
	}


	// [var, var, var]
	@Override
	public void setValue(String value) {
		data = StringUtils.parseIntList(value);
	}

	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
		
		if (data.length == 1) {
			dataOffset = (int)os.getStreamPointer();
			os.writeInt(data[0]);
		} else {
			dataOffset = toOffset;
			os.writeInt(toOffset);
			os.seek(toOffset);
			
			for (int value : data)
				os.writeInt(value);
			
			toOffset += (data.length << 2);
		}
		return toOffset;
	}
}