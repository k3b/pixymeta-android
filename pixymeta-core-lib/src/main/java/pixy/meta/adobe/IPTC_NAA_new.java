/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 * 
 * Change History - most recent changes go on top of previous changes
 *
 * IPTC_NAA.java
 *
 * Who   Date       Description
 * ====  =========  ==================================================
 * WY    25Apr2015  Added addValues()
 * WY    25Apr2015  Renamed getDataSet(0 to getFieldValueMap()
 * WY    13Apr2015  Changed write() to use ITPC.write()
 * WY    12Apr2015  Removed unnecessary read()
 */

package pixy.meta.adobe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.io.IOUtils;
import pixy.meta.MetadataBase;
import pixy.meta.MetadataType;
import pixy.meta.iptc.IPTC;
import pixy.meta.iptc.IPTCApplicationTag;
import pixy.meta.iptc.IPTCFieldValue;
import pixy.meta.iptc.IPTCFieldValueNew;

/** The main iptc payload living in an {@link AdobeIRBSegment} */
public class IPTC_NAA_new extends MetadataBase { // AdobyMetadataBase {
	Map<IFieldDefinition, IPTCFieldValueNew> map = new HashMap<IFieldDefinition, IPTCFieldValueNew>();

	public IPTC_NAA_new(ImageResourceID tag, byte[] data) {
		super(MetadataType.IPTC, data);
	}

	public void addField(IFieldDefinition tag, Object data) {
		// TODO wrong params
		map.put(tag, new IPTCFieldValueNew(tag, data, 0, 0));
	}

	public IFieldValue getValue(IFieldDefinition tag) {
		return map.get(tag);
	}

	@Override
	public void read() throws IOException {
		if(!isDataRead) {
			int i = 0;
			byte[] data = getData();
			int tagMarker = data[i];
			while (tagMarker == 0x1c) {
				i++;
				int recordNumber = data[i++]&0xff;
				int tag = data[i++]&0xff;
				int recordSize = IOUtils.readUnsignedShortMM(data, i);
				i += 2;

				IPTCApplicationTag tagEnum = IPTCApplicationTag.fromTag(tag);

				IPTCFieldValueNew existingValue = map.get(tagEnum);
				if (existingValue == null) {
					existingValue = new IPTCFieldValueNew(tagEnum, data, i, recordSize);
					map.put(tagEnum, existingValue);
				} else {
					existingValue.append(data, i, recordSize);
				}

				i += recordSize;
				// Sanity check
				if(i >= data.length) break;
				tagMarker = data[i];
			}

			/*
			// Remove possible duplicates
			for (Map.Entry<String, IPTCFieldValueNew.IPTCFieldValueList> entry : fieldValueMap.entrySet()){
			    entry.setValue(new IPTCFieldValueNew.IPTCFieldValueList(new HashSet<IPTCFieldValueNew>(entry.getValue())));
			}
			*/

			isDataRead = true;
		}
	}

	@Override
	public void showMetadata() {

	}

	public void write(OutputStream os) throws IOException {
		byte[] data = null;
		int size = 0;
		if(data == null) {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();

			for(IPTCFieldValueNew value : map.values()) {
				value.write(os);
			}

			data = bout.toByteArray();
			size = data.length;
		}
		super.write(os);
	}
}