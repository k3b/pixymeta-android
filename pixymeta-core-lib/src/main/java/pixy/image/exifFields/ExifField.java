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

import java.io.IOException;

import pixy.api.IDataType;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.io.RandomAccessOutputStream;

/**
 * One meta data item of basetype T living in a IFD (Image File Directory).
 * <p>
 * We could have used a ExifTag enum as the first parameter of the constructor, but this
 * will not work with unknown tags of tag type ExifTag.JPG_SEGMENT_UNKNOWN. In that case, we cannot
 * use the tag values to sort the fields or as keys for a hash map as used by {@link IFD}.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/04/2013
 */
public abstract class ExifField<T> implements Comparable<ExifField<?>>, IFieldValue{
	private final pixy.image.exifFields.FieldType fieldType;
	private final int length;
	private final pixy.image.exifFields.Tag tag;
	protected T data;

	protected int dataOffset;
	
	public ExifField(pixy.image.exifFields.Tag tag, pixy.image.exifFields.FieldType fieldType, int length) {
		this.fieldType = fieldType;
		this.length = length;
		this.tag = tag;
	}
	
	public int compareTo(ExifField<?> that) {
		return (this.getTag()&0xffff) - (that.getTag()&0xffff);
    }
	
	public T getData() {
		return data;
	}

	/** Return an integer array representing TIFF long field */
	public int[] getDataAsLong() { 
		throw new UnsupportedOperationException("getDataAsLong() method is only supported by"
				+ " short, long, and rational data types");
	}
	
	/**
	 * @return a String representation of the field data
	 */
	public abstract String getDataAsString();
	
	public int getLength() {
		return length;
	}
	
	/**
	 * Used to update field data when necessary.
	 * <p>
	 * This method should be called only after the field has been written to the underlying RandomOutputStream.
	 * 
	 * @return the stream position where actual data starts to write
	 */
	public int getDataOffset() {
		return dataOffset;
	}
	
	public short getTag() {
		return tag.getValue();
	}
	
	public pixy.image.exifFields.FieldType getType() {
		return this.fieldType;
	}

	@Override public String toString() {
		return tag.toString();
	}
	
	public final int write(RandomAccessOutputStream os, int toOffset) throws IOException {
		// Write the header first
		os.writeShort(this.getTag());
		os.writeShort(getType().getValue());
		os.writeInt(getLength());
		// Then the actual data
		return writeData(os, toOffset);
	}
	
	protected abstract int writeData(RandomAccessOutputStream os, int toOffset) throws IOException;

	// implementation of api.IFieldValue
	@Override
	public IFieldDefinition getDefinition() {
		return tag;
	}

	// implementation of api.IFieldValue
	@Override
	public String getValueAsString() {
		return this.getDataAsString();
	}

	// implementation of api.IFieldValue
	@Override
	public IDataType getDataType(){
		return fieldType;
	}



}