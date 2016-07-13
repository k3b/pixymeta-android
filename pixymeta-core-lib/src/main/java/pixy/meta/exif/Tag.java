/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package pixy.meta.exif;

import pixy.api.IFieldDefinition;
import pixy.image.exifFields.FieldType;

/**
 * Common interface for all TIFF/EXIF related tag enumerations
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 03/28/2014
 */
public interface Tag extends IFieldDefinition {
	public String getFieldAsString(Object value);
	public FieldType getFieldType();
	public String getName();
	public short getValue();
}