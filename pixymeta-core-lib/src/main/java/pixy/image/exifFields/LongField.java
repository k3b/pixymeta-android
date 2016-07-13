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

import pixy.string.StringUtils;

/**
 * TIFF Long type field.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/06/2013
 */
public final class LongField extends AbstractLongField {

	public LongField(pixy.meta.exif.Tag context, int[] data) {
		super(context, pixy.image.exifFields.FieldType.LONG, data);
	}
	
	public String getDataAsString() {
		return StringUtils.toListString(data, 0, 10, true);
	}
	
}