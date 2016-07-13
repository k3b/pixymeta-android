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
 * IFD.java
 *
 * Who   Date       Description
 * ====  =========  =======================================================================
 * WY    15Dec2014  Added removeChild() method
 * WY    24Nov2014  Added getChild() method
 * WY    02Apr2014  Added setNextIFDOffset() to work with the case of non-contiguous IFDs
 * WY    30Mar2014  Added children map, changed write() method to write child nodes as well.
 */

package pixy.image.exifFields;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pixy.api.IDirectory;
import pixy.api.IFieldValue;
import pixy.io.RandomAccessOutputStream;
import pixy.string.StringUtils;

/**
 * Image File Directory containing meta-data-fields and sub-IFDs
 *
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/04/2013
 */
public class IFD implements IDirectory {

	private static final String MODUL_NAME = "IFD";
	/**
	 * Create a children map for sub IFDs. A sub IFD is associated with a tag of the current IFD
	 * which serves as pointer to the sub IFD.
	 */	 
	private Map<pixy.meta.exif.Tag, IFD> children = new HashMap<pixy.meta.exif.Tag, IFD>();
	
	/** Create a fields map to hold all of the fields for this IFD */
	private Map<Short, ExifField<?>> tiffFields = new HashMap<Short, ExifField<?>>();

	private int endOffset;
	
	private int startOffset;

	public IFD() {}
	
	// Copy constructor
	public IFD(IFD other) {
		// Defensive copy
		this.children = Collections.unmodifiableMap(other.children);
		this.tiffFields = Collections.unmodifiableMap(other.tiffFields);
		this.startOffset = other.startOffset;
		this.endOffset = other.endOffset;
	}
	
	public IFD addChild(pixy.meta.exif.Tag tag, IFD child) {
		children.put(tag, child);
		return this;
	}
	
	public void addField(ExifField<?> exifField) {
		tiffFields.put(exifField.getTag(), exifField);
	}
	
	public void addFields(Collection<ExifField<?>> exifFields) {
		for(ExifField<?> field : exifFields) {
			addField(field);
		}
	}
	
	public IFD getChild(pixy.meta.exif.Tag tag) {
		final IFD result = children.get(tag);
		return result;
	}
	
	public Map<pixy.meta.exif.Tag, IFD> getChildren() {
		return Collections.unmodifiableMap(children);
	}
	
	public int getEndOffset() {
		return endOffset;
	}
	
	public ExifField<?> getField(pixy.meta.exif.Tag tag) {
		return tiffFields.get(tag.getValue());
	}
	
	/**
	 * Return a String representation of the field 
	 * @param tag Tag for the field
	 * @return a String representation of the field
	 */
	public String getFieldAsString(pixy.meta.exif.Tag tag) {
		ExifField<?> field = tiffFields.get(tag.getValue());
		if(field != null) {
			FieldType ftype = field.getType();
			String suffix = null;
			if(ftype == FieldType.SHORT || ftype == FieldType.SSHORT)
				suffix = tag.getFieldAsString(field.getDataAsLong());
			else
				suffix = tag.getFieldAsString(field.getData());			
			
			return field.getDataAsString() + (StringUtils.isNullOrEmpty(suffix)?"":" => " + suffix);
		}
		return "";
	}
	
	/** Get all the fields for this IFD from the internal map. */
	public Collection<ExifField<?>> getFields() {
		return Collections.unmodifiableCollection(tiffFields.values());
	}
	
	public int getSize() {
		return tiffFields.size();
	}
	
	public int getStartOffset() {
		return startOffset;
	}
	
	/** Remove all the entries from the IDF fields map */
	public void removeAllFields() {
		tiffFields.clear();
	}
	
	public IFD removeChild(pixy.meta.exif.Tag tag) {
		return children.remove(tag);
	}
	
	/** Remove a specific field associated with the given tag */
	public ExifField<?> removeField(pixy.meta.exif.Tag tag) {
		return tiffFields.remove(tag.getValue());
	}
	
	/**
	 * Set the next IFD offset pointer
	 * <p>
	 * Note: This should <em>ONLY</em> be called
	 * after the current IFD has been written to the RandomAccessOutputStream
	 *  
	 * @param os RandomAccessOutputStream
	 * @param nextOffset next IFD offset value
	 * @throws IOException
	 */
	public void setNextIFDOffset(RandomAccessOutputStream os, int nextOffset) throws IOException {
		os.seek(endOffset - 4);
		os.writeInt(nextOffset);
	}
	
	/** Write this IFD and all the children, if any, to the output stream
	 * 
	 * @param os RandomAccessOutputStream
	 * @param offset stream offset to write this IFD
	 * 
	 * @throws IOException
	 */
	public int write(RandomAccessOutputStream os, int offset) throws IOException {
		startOffset = offset;
		// Write this IFD and its children, if any, to the RandomAccessOutputStream
		List<ExifField<?>> list = new ArrayList<ExifField<?>>(tiffFields.values());
		// Make sure tiffFields are in incremental order.
		Collections.sort(list);
		os.seek(offset);
		os.writeShort(list.size());
		offset += 2;
		endOffset = offset + list.size() * 12 + 4;			
		// The first available offset to write tiffFields. 
		int toOffset = endOffset;
		os.seek(offset); // Set first field offset.
				
		for (ExifField<?> exifField : list)
		{
			toOffset = exifField.write(os, toOffset);
			offset += 12; // Move to next field. Each field is of fixed length 12.
			os.seek(offset); // Reset position to next directory field.
		}
		
		/* Set the stream position at the end of the IFD to update
		 * next IFD offset
		 */
		os.seek(offset);
		os.writeInt(0);	// Set next IFD offset to default 0 
		
		// Write sub IFDs if any (we assume bare-bone sub IFDs pointed by long field type with no image data associated)
		if(children.size() > 0) {
			for (Map.Entry<pixy.meta.exif.Tag, IFD> entry : children.entrySet()) {
			    pixy.meta.exif.Tag key = entry.getKey();
			    IFD value = entry.getValue();
			    // Update parent field if present, otherwise skip
			    ExifField<?> exifField = this.getField(key);
			    if(exifField != null) {
			    	int dataPos = exifField.getDataOffset();
					os.seek(dataPos);
					os.writeInt(toOffset);
					os.seek(toOffset);
					toOffset = value.write(os, toOffset);
			    }
		    }
		}
			
		return toOffset;
	}

	private String name = null;

	// implementation of api.IDirectory
	@Override
	public String getName() {
		return name;
	}

	// implementation of api.IDirectory
	@Override
	public IDirectory setName(String name) {
		this.name = name;
		return this;
	}

	// implementation of api.IDirectory
	@Override
	public List<IFieldValue> getValues() {
		if ((tiffFields != null) && (tiffFields.size() > 0)) {
			return new ArrayList<IFieldValue>(tiffFields.values());
		}
		return null;
	}
}