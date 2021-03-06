/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * Copyright (C) 2016 by k3b.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Who   Date       Description
 * ====  =======    =================================================
 * k3b	 july2016	refactored; Added interface supoort for common tag handling
 */

package pixy.image.exifFields;

import java.io.IOException;

import pixy.io.RandomAccessOutputStream;
import pixy.string.StringUtils;

public abstract class AbstractByteField extends ExifField<byte[]> {

	public AbstractByteField(pixy.meta.exif.Tag tag, pixy.image.exifFields.FieldType fieldType, byte[] data) {
		super(tag, fieldType, data.length);
		this.data = data;
	}
	
	public byte[] getData() {
		return data.clone();
	}
	
	public String getDataAsString() {
		return StringUtils.toHexListString(data, 0, 10);
	}

	public void setValue(String value) {
		data = StringUtils.parseHexByteList(value);
	}

	protected int writeData(RandomAccessOutputStream os, int toOffset) throws IOException {
	
		if (data.length <= 4) {
			dataOffset = (int)os.getStreamPointer();
			byte[] tmp = new byte[4];
			System.arraycopy(data, 0, tmp, 0, data.length);
			os.write(tmp);
		} else {
			dataOffset = toOffset;
			os.writeInt(toOffset);
			os.seek(toOffset);
			os.write(data);
			toOffset += data.length;
		}
		return toOffset;
	}
}