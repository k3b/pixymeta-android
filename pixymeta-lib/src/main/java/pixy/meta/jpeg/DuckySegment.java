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
 * DuckySegment.java
 *
 * Who   Date       Description
 * ====  =======    ============================================================
 * WY    02Jul2015  Initial creation
 */

package pixy.meta.jpeg;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.io.IOUtils;
import pixy.meta.IMetadataDirectory;
import pixy.meta.IMetadataTag;
import pixy.meta.Metadata;
import pixy.meta.MetadataDirectoryImpl;
import pixy.meta.MetadataType;

public class DuckySegment extends Metadata  implements IMetadataDirectory {

	private static final String MODUL_NAME = "JPeg-Ducky";
	private Map<DuckyTag, DuckyDataSet> datasetMap;
	
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(DuckySegment.class);
		
	public DuckySegment() {
		super(MetadataType.JPG_DUCKY, null);
		datasetMap =  new EnumMap<DuckyTag, DuckyDataSet>(DuckyTag.class);
		isDataRead = true;
	}
	
	public DuckySegment(byte[] data) {
		super(MetadataType.JPG_DUCKY, data);
	}
	
	public void addDataSet(DuckyDataSet dataSet) {
		if(datasetMap != null) {
			datasetMap.put(DuckyTag.fromTag(dataSet.getTag()), dataSet);				
		}
	}
	
	public void addDataSets(Collection<? extends DuckyDataSet> dataSets) {
		if(datasetMap != null) {
			for(DuckyDataSet dataSet: dataSets) {
				datasetMap.put(DuckyTag.fromTag(dataSet.getTag()), dataSet);				
			}
		}
	}
	
	public Map<DuckyTag, DuckyDataSet> getDataSets() {
		ensureDataRead();
		return Collections.unmodifiableMap(datasetMap);
	}
	
	public void read() throws IOException {
		if(!isDataRead) {
			int i = 0;
			datasetMap = new EnumMap<DuckyTag, DuckyDataSet>(DuckyTag.class);
			
			for(;;) {
				if(i + 4 > data.length) break;
				int tag = IOUtils.readUnsignedShortMM(data, i);
				i += 2;
				int size = IOUtils.readUnsignedShortMM(data, i);
				i += 2;
				DuckyTag etag = DuckyTag.fromTag(tag);
				datasetMap.put(etag, new DuckyDataSet(tag, size, data, i));
				i += size;
			}
			
		    isDataRead = true;
		}
	}

	public void showMetadata() {
		ensureDataRead();
		LOGGER.info("JPEG DuckySegment output starts =>");
		// Print DuckyDataSet
		for(DuckyDataSet dataset : datasetMap.values()) {
			dataset.print();
		}
		LOGGER.info("<= JPEG DuckySegment output ends");
	}
	
	public void write(OutputStream os) throws IOException {
		ensureDataRead();
		for(DuckyDataSet dataset : getDataSets().values())
			dataset.write(os);
	}

	private MetadataDirectoryImpl metaData = null;

	// calculate metaData on demand
	private MetadataDirectoryImpl get() {
		if ((metaData == null)) {
			metaData = new MetadataDirectoryImpl().setName(MODUL_NAME);

			ensureDataRead();
			// MetadataDirectoryImpl child = new MetadataDirectoryImpl().setName(entry.getKey());
			// metaData.getSubdirectories().add(child);

			// final List<IMetadataTag> tags = child.getTags();
			// tags.add(new MetaDataTagImpl("type", thumbnail.getDataTypeAsString()));
		}
		return metaData;
	}

	/**
	 * Provides the name of the directory, for display purposes.  E.g. <code>Exif</code>
	 *
	 * @return the name of the directory
	 */
	@Override
	public String getName() {
		return get().getName();
	}

	/**
	 * @return sub-directories that belong to this Directory or null if there are no sub-directories
	 */
	@Override
	public List<IMetadataDirectory> getSubdirectories() {
		return get().getSubdirectories();
	}

	/**
	 * @return Tags that belong to this Directory or null if there are no tags
	 */
	@Override
	public List<IMetadataTag> getTags() {
		return get().getTags();
	}

}