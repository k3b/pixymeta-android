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
 * ImageMetadata.java
 *
 * Who   Date       Description
 * ====  =========  =====================================================
 * WY    13Mar2015  Initial creation
*/

package pixy.meta.image;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import pixy.meta.IMetadataDirectory;
import pixy.meta.IMetadataTag;
import pixy.meta.MetaDataTagImpl;
import pixy.meta.Metadata;
import pixy.meta.MetadataDirectoryImpl;
import pixy.meta.MetadataType;
import pixy.meta.Thumbnail;
import pixy.string.XMLUtils;

public class ImageMetadata extends Metadata  implements IMetadataDirectory {
	private static final String MODUL_NAME = "Image";
	private Document document;
	private Map<String, Thumbnail> thumbnails;

	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageMetadata.class);
	
	public ImageMetadata(Document document) {
		super(MetadataType.IMAGE, null);
		this.document = document;
	}
	
	public ImageMetadata(Document document, Map<String, Thumbnail> thumbnails) {
		super(MetadataType.IMAGE, null);
		this.document = document;
		this.thumbnails = thumbnails;
	}
	
	public boolean containsThumbnail() {
		return thumbnails != null && thumbnails.size() > 0;
	}
	
	public Document getDocument() {
		return document;
	}
	
	public Map<String, Thumbnail> getThumbnails() {
		return thumbnails;
	}
	
	public void read() throws IOException {
		if(!isDataRead)
			// No implementation
			isDataRead = true;
	}
	
	@Override
	public void showMetadata() {
		XMLUtils.showXML(document);
		// Thumbnail information
		if(containsThumbnail()) { // We have thumbnail
			Iterator<Map.Entry<String, Thumbnail>> entries = thumbnails.entrySet().iterator();
			LOGGER.info("Total number of thumbnails: {}", thumbnails.size());
			int i = 0;
			while (entries.hasNext()) {
			    Map.Entry<String, Thumbnail> entry = entries.next();
			    LOGGER.info("Thumbnail #{}: {} thumbnail:", i, entry.getKey());
			    Thumbnail thumbnail = entry.getValue();
			    LOGGER.info("Thumbnail width: {}", ((thumbnail.getWidth() < 0)? " Unavailable": thumbnail.getWidth()));
				LOGGER.info("Thumbanil height: {}", ((thumbnail.getHeight() < 0)? " Unavailable": thumbnail.getHeight()));
				LOGGER.info("Thumbnail metaData type: {}", thumbnail.getDataTypeAsString());
				i++;
			}
		}		
	}

	private MetadataDirectoryImpl metaData = null;

	// calculate metaData on demand
	private MetadataDirectoryImpl get() {
		if ((metaData == null)) {
			metaData = new MetadataDirectoryImpl().setName(MODUL_NAME);

			ensureDataRead();
			if (containsThumbnail()) {
				Iterator<Map.Entry<String, Thumbnail>> entries = thumbnails.entrySet().iterator();

				while (entries.hasNext()) {
					Map.Entry<String, Thumbnail> entry = entries.next();
					MetadataDirectoryImpl child = new MetadataDirectoryImpl().setName(entry.getKey());
					metaData.getSubdirectories().add(child);
					Thumbnail thumbnail = entry.getValue();

					final List<IMetadataTag> tags = child.getTags();
					if (thumbnail.getWidth() > 0)
						tags.add(new MetaDataTagImpl("width", "" + thumbnail.getWidth()));
					if (thumbnail.getHeight() > 0)
						tags.add(new MetaDataTagImpl("height", "" + thumbnail.getHeight()));
					tags.add(new MetaDataTagImpl("type", thumbnail.getDataTypeAsString()));
				}

				if (document != null) {
					metaData.getTags().add(new MetaDataTagImpl("xml", XMLUtils.print(document, "", new StringBuilder()).toString()));
				}
			}
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