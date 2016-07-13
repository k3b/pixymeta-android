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

import pixy.api.IIntableField;
import pixy.string.StringUtils;

/**
 * TIFF Short type field.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/06/2013
 */
public final class ShortField extends AbstractShortField  implements IIntableField {

	public ShortField(pixy.meta.exif.Tag tag, short[] data) {
		super(tag, FieldType.SHORT, data);
	}

	@Override
	public int[] getDataAsLong() {
		//
		int[] temp = new int[data.length];
		
		for(int i=0; i<data.length; i++) {
			temp[i] = data[i]&0xffff;
		}
				
		return temp;
	}

	@Override
	public String getDataAsString() {
		return StringUtils.toListString(data, 0, 10, true);
	}

	@Override
	public void setValue(String value) {
		data = StringUtils.parseShortList(value);
	}


}