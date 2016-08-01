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
 * IPTC.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    25Apr2015  Renamed getDataSet() to getFieldValueMap()
 * WY    25Apr2015  Added addValues()
 * WY    13Apr2015  Added write()
 */

package pixy.meta.iptc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import pixy.api.DefaultApiImpl;
import pixy.api.IDirectory;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.meta.MetadataBase;
import pixy.meta.MetadataType;
import pixy.io.IOUtils;

/** recursive container for {@link pixy.meta.iptc.IPTCFieldValue} */
public class IPTC extends MetadataBase {
	public static void showIPTC(byte[] data) {
		if(data != null && data.length > 0) {
			IPTC iptc = new IPTC(data);
			try {
				iptc.read();
				iptc.showMetadata();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public static void showIPTC(InputStream is) {
		try {
			showIPTC(IOUtils.inputStreamToByteArray(is));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private IPTCFieldValue.IPTCFieldValueMap fieldValueMap;
	
	public IPTC() {
		super(MetadataType.IPTC, null);
		fieldValueMap =  new IPTCFieldValue.IPTCFieldValueMap();
		isDataRead = true;
	}
	
	public IPTC(byte[] data) {
		super(MetadataType.IPTC, data);
		ensureDataRead();
	}

	public void addField(IFieldDefinition tag, Object data) {
		addValue(new IPTCFieldValue(tag, data));
	}

	public void addValue(IPTCFieldValue value) {
		addValues(Arrays.asList(value));
	}

	public IFieldValue getValue(IFieldDefinition tag) {
		ensureDataRead();
		IPTCFieldValue.IPTCFieldValueList list = getValue(tag.toString());
		return (list == null) ? null : list.getValue(tag);
	}

	public void addValues(Collection<? extends IPTCFieldValue> values) {
		if(fieldValueMap != null) {
			for(IPTCFieldValue value: values) {
				String name = value.getName();
				if(fieldValueMap.get(name) == null) {
					IPTCFieldValue.IPTCFieldValueList list = new IPTCFieldValue.IPTCFieldValueList();
					list.add(value);
					fieldValueMap.put(name, list);
				} else if(value.allowMultiple()) {
					fieldValueMap.get(name).add(value);
				}
			}
		}
	}

	/**
	 * Get a string representation of the IPTCFieldValue associated with the key
	 *  
	 * @param key the name for the IPTCFieldValue
	 * @return a String representation of the IPTCFieldValue, separated by ";"
	 */
	public String getAsString(String key) {
		// Retrieve the IPTCFieldValue list associated with this key
		// Most of the time the list will only contain one item
		IPTCFieldValue.IPTCFieldValueList list = getValue(key);
		
		String value = "";
	
		if(list != null) {
			if(list.size() == 1) {
				value = list.get(0).getDataAsString();
			} else {
				for(int i = 0; i < list.size() - 1; i++)
					value += list.get(i).getDataAsString() + ";";
				value += list.get(list.size() - 1).getDataAsString();
			}
		}
			
		return value;
	}
	
	/**
	 * Get a list of IPTCFieldValue associated with a key
	 * 
	 * @param name name of the data set
	 * @return a list of IPTCFieldValue associated with the key
	 */
	public IPTCFieldValue.IPTCFieldValueList getValue(String name) {
		return getFieldValueMap().get(name);
	}
	
	/**
	 * Get all the IPTCFieldValue as a map for this IPTC data
	 * 
	 * @return a map with the key for the IPTCFieldValue name and a list of IPTCFieldValue as the value
	 */
	public IPTCFieldValue.IPTCFieldValueMap getFieldValueMap() {
		ensureDataRead();
		return fieldValueMap;
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			int i = 0;
			int tagMarker = getData()[i];
			fieldValueMap = new IPTCFieldValue.IPTCFieldValueMap();
			while (tagMarker == 0x1c) {
				i++;
				int recordNumber = getData()[i++]&0xff;
				int tag = getData()[i++]&0xff;
				int recordSize = IOUtils.readUnsignedShortMM(getData(), i);
				i += 2;
				IPTCFieldValue value = new IPTCFieldValue(recordNumber, tag, recordSize, getData(), i);
				String name = value.getName();
				final IPTCFieldValue.IPTCFieldValueList existingValue = fieldValueMap.get(name);
				if(existingValue == null) {
					IPTCFieldValue.IPTCFieldValueList list = new IPTCFieldValue.IPTCFieldValueList();
					list.add(value);
					fieldValueMap.put(name, list);
				} else {
					existingValue.add(value);
				}
				i += recordSize;
				// Sanity check
				if(i >= getData().length) break;
				tagMarker = getData()[i];
			}

			/*
			// Remove possible duplicates
			for (Map.Entry<String, IPTCFieldValue.IPTCFieldValueList> entry : fieldValueMap.entrySet()){
			    entry.setValue(new IPTCFieldValue.IPTCFieldValueList(new HashSet<IPTCFieldValue>(entry.getValue())));
			}
			*/

			isDataRead = true;
		}
	}
	
	public void showMetadata() {
		ensureDataRead();
		if(fieldValueMap != null){
			// Print multiple entry IPTCFieldValue
			for(IPTCFieldValue.IPTCFieldValueList iptcs : fieldValueMap.values()) {
				for(IPTCFieldValue iptc : iptcs)
					iptc.print();
			}
		}
	}

	/**
	 * @return directories that belong to this MetaData.
	 * */
	@Override
	public List<IDirectory> getMetaData() {
		ensureDataRead();
		ArrayList<IDirectory> result = new ArrayList<IDirectory>();
		for (Map.Entry<String, IPTCFieldValue.IPTCFieldValueList> entry : getFieldValueMap().entrySet()) {
			result.add(DefaultApiImpl.createDirectory(entry.getKey(), new ArrayList<IFieldValue>(entry.getValue())));
		}
		return result;
	}


	public void write(OutputStream os) throws IOException {
		for(IPTCFieldValue.IPTCFieldValueList values : getFieldValueMap().values())
			for(IPTCFieldValue value : values)
				value.write(os);
	}

}