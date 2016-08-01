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
import java.util.Collection;
import java.util.Map;

import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.meta.iptc.IPTC;
import pixy.meta.iptc.IPTCFieldValue;

/** The main iptc payload living in an {@link pixy.meta.adobe.AdobeIRBSegment} */
public class IPTC_NAA extends AdobyMetadataBase {
	//
	private IPTC iptc;
		
	public IPTC_NAA(ImageResourceID tag) {
		this(tag, "IPTC_NAA");
	}
	
	public IPTC_NAA(ImageResourceID tag, String name) {
		super(tag, name, null);
		iptc = new IPTC();
	}

	public IPTC_NAA(ImageResourceID tag, String name, byte[] data) {
		super(tag, name, data);
		iptc = new IPTC(data);
	}

	public void addField(IFieldDefinition tag, Object data) {
		iptc.addValue(new IPTCFieldValue(tag, data));
	}

	public IFieldValue getValue(IFieldDefinition tag) {
		return iptc.getValue(tag);
	}

	public void addDataSets(Collection<? extends IPTCFieldValue> dataSets) {
		iptc.addValues(dataSets);
	}
	
	/**
	 * Get all the IPTCFieldValue as a map for this IPTC data
	 * 
	 * @return a map with the key for the IPTCFieldValue name and a list of IPTCFieldValue as the value
	 */
	public Map<String, IPTCFieldValue.IPTCFieldValueList> getDataSets() {
		return iptc.getFieldValueMap();
	}
	
	/**
	 * Get a list of IPTCFieldValue associated with a key
	 * 
	 * @param key name of the data set
	 * @return a list of IPTCFieldValue associated with the key
	 */
	public IPTCFieldValue.IPTCFieldValueList getDataSet(String key) {
		return iptc.getValue(key);
	}
	
	public void print() {
		super.print();
		// Print multiple entry IPTCFieldValue
		for(IPTCFieldValue.IPTCFieldValueList datasets : iptc.getFieldValueMap().values())
			for(IPTCFieldValue dataset : datasets)
				dataset.print();			
	}
	
	public void write(OutputStream os) throws IOException {
		if(data == null) {			
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			iptc.write(bout);
			data = bout.toByteArray();
			size = data.length;
		}
		super.write(os);
	}
}