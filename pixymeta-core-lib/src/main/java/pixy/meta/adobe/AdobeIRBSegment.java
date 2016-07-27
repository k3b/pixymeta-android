/**
 * Copyright (c) 2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * AdobeIRBSegment.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    14Apr2015  Added getThumbnailResource()
 * WY    10Apr2015  Added containsThumbnail() and getThumbnail()
 * WY    13Mar2015  Initial creation
 */

package pixy.meta.adobe;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.fileprocessor.jpg.JpgFileProcessor;
import pixy.meta.MetadataBase;
import pixy.meta.MetadataType;
import pixy.util.ArrayUtils;
import pixy.io.IOUtils;

/**
 * The {@link pixy.meta.adobe.AdobeIRBSegment} is Container for sub-MetaData
 * The most important payload are the
 *    {@link pixy.meta.iptc.IPTCTag}-s in {@link pixy.meta.iptc.IPTC} containers.
 * */
public class AdobeIRBSegment extends MetadataBase {
	private static final String SUB_SEGMENT_MARKER = "8BIM";
	private boolean containsThumbnail;
	private ThumbnailResource thumbnail;
	Map<Short, AdobyMetadataBase> _8bims = new HashMap<Short, AdobyMetadataBase>();

	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(AdobeIRBSegment.class);
	
	public static void showIRB(byte[] data) {
		if(data != null && data.length > 0) {
			AdobeIRBSegment adobeIrbSegment = new AdobeIRBSegment(data);
			try {
				adobeIrbSegment.read();
				adobeIrbSegment.showMetadata();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public AdobeIRBSegment(byte[] data) {
		super(MetadataType.PHOTOSHOP_IRB, data);
		// setDebugMessageBuffer(new StringBuilder());
		debug("create AdobeIRBSegment");
	}
	
	public boolean containsThumbnail() {
		ensureDataRead();
		return containsThumbnail;
	}
	
	public Map<Short, AdobyMetadataBase> get8BIM() {
		ensureDataRead();
		return Collections.unmodifiableMap(_8bims);
	}
	
	public AdobyMetadataBase get8BIM(short tag) {
		ensureDataRead();
		return _8bims.get(tag);
	}
	
	public IRBThumbnail getThumbnail()  {
		ensureDataRead();
		return thumbnail.getThumbnail();
	}
	
	public ThumbnailResource getThumbnailResource() {
		ensureDataRead();
		return thumbnail;
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			int i = 0;
			int unknown = 0; // number of skipped bytes before SUB_SEGMENT_MARKER
			while((i+4) < getData().length) {
				int start = i;
				String _8bim = new String(getData(), i, SUB_SEGMENT_MARKER.length());
				i += SUB_SEGMENT_MARKER.length();
				if(_8bim.equals(SUB_SEGMENT_MARKER)) {
					short tagCode = IOUtils.readShortMM(getData(), i);
					i += 2;
					// Pascal string for name follows
					// First byte denotes string length -
					int nameLen = getData()[i++]&0xff;
					if((nameLen%2) == 0) nameLen++;
					String name = new String(getData(), i, nameLen).trim();
					i += nameLen;
					//
					int size = IOUtils.readIntMM(getData(), i);
					i += 4;
					
					ImageResourceID tag = ImageResourceID.fromShort(tagCode);

					if (isDebugEnabled()) {
						debug("AdobeIRBSegment.read " + tag + "[" + name +", skip=" + unknown +
								"] :" +
								start +"/" + (i-start) + "+" +size + "/" + getData().length);
					}
					switch(tag) {
						case JPEG_QUALITY:
							_8bims.put(tagCode, new JPEGQuality(name, ArrayUtils.subArray(getData(), i, size)));
							break;
						case VERSION_INFO:
							_8bims.put(tagCode, new VersionInfo(name, ArrayUtils.subArray(getData(), i, size)));
							break;
						case IPTC_NAA:
							byte[] newData = ArrayUtils.subArray(getData(), i, size);
							AdobyMetadataBase iptcBim = _8bims.get(tagCode);
							if(iptcBim != null) {
								byte[] oldData = iptcBim.getData();
								_8bims.put(tagCode, new IPTC_NAA(name, ArrayUtils.concat(oldData, newData)));
							} else
								_8bims.put(tagCode, new IPTC_NAA(name, newData));
							break;
						case THUMBNAIL_RESOURCE_PS4:
						case THUMBNAIL_RESOURCE_PS5:
							containsThumbnail = true;
							thumbnail = new ThumbnailResource(tag, ArrayUtils.subArray(getData(), i, size));
							_8bims.put(tagCode, thumbnail);
							break;
						default:
							_8bims.put(tagCode, new AdobyMetadataBase(tagCode, name, size, ArrayUtils.subArray(getData(), i, size)));
					}

					unknown = 0;
					i += size;
					if(size%2 != 0) i++; // Skip padding byte
				} else {
					unknown += SUB_SEGMENT_MARKER.length();
				}
			}
			isDataRead = true;
		}
	}
	
	public void showMetadata() {
		if (isDebugEnabled()) {
			ensureDataRead();
			debug("<<Adobe AdobeIRBSegment information starts>>");
			for (AdobyMetadataBase metadata : _8bims.values()) {
				metadata.print();
			}
			if (containsThumbnail) {
				debug("" + thumbnail.getResouceID());
				int thumbnailFormat = thumbnail.getDataType(); //1 = kJpegRGB. Also supports kRawRGB (0).
				switch (thumbnailFormat) {
					case IRBThumbnail.DATA_TYPE_KJpegRGB:
						debug("Thumbnail format: KJpegRGB");
						break;
					case IRBThumbnail.DATA_TYPE_KRawRGB:
						debug("Thumbnail format: KRawRGB");
						break;
				}
				debug("Thumbnail width: " + thumbnail.getWidth());
				debug("Thumbnail height: " + thumbnail.getHeight());
				// Padded row bytes = (width * bits per pixel + 31) / 32 * 4.
				debug("Padded row bytes: " + thumbnail.getPaddedRowBytes());
				// Total size = widthbytes * height * planes
				debug("Total size: " + thumbnail.getTotalSize());
				// Size after compression. Used for consistency check.
				debug("Size after compression: " + thumbnail.getCompressedSize());
				// Bits per pixel. = 24
				debug("Bits per pixel: " + thumbnail.getBitsPerPixel());
				// Number of planes. = 1
				debug("Number of planes: " + thumbnail.getNumOfPlanes());
			}

			debug("<<Adobe AdobeIRBSegment information ends>>");
		}
	}
}