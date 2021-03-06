/**
 * Copyright (c) 2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package pixy.meta;

public enum MetadataType {
	EXIF, // EXIF
	IPTC, // IPTC
	ICC_PROFILE, // ICC Profile
	XMP, // Adobe XMP
	PHOTOSHOP_IRB, // PHOTOSHOP Image Resource Block
	PHOTOSHOP_DDB, // PHOTOSHOP Document Data Block
	COMMENT, // General comment
	IMAGE, // Image specific information
	JPG_JFIF, // JPEG JPG_SEGMENT_JFIF_APP0 (JFIF)
	JPG_DUCKY, // JPEG APP12 (DUCKY)
	JPG_ADOBE, // JPEG APP14 (ADOBE)
	PNG_TEXTUAL, // PNG textual information
	PNG_TIME; // PNG tIME (last modified time) chunk
}