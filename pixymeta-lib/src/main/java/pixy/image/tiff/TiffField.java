/**
 * Copyright (c) 2014-2016 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package pixy.image.tiff;

import java.io.IOException;

import pixy.io.RandomAccessOutputStream;
import pixy.meta.IMetadataTag;

/**
 * One one meta data item of basetype T living in a IFD (Image File Directory).
 * <p>
 * We could have used a TiffTag enum as the first parameter of the constructor, but this
 * will not work with unknown tags of tag type TiffTag.UNKNOWN. In that case, we cannot
 * use the tag values to sort the fields or as keys for a hash map as used by {@link IFD}.
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 01/04/2013
 */
public abstract class TiffField<T> implements Comparable<TiffField<?>>, IMetadataTag{
	private final FieldType fieldType;
	private final int length;
	private final Tag tag;
	protected T data;	
		
	protected int dataOffset;
	
	public TiffField(Tag tag, FieldType fieldType, int length) {
		this.fieldType = fieldType;
		this.length = length;
		this.tag = tag;
	}
	
	public int compareTo(TiffField<?> that) {
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
	
	public FieldType getType() {
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

	/**
	 * Provides a String the name of the Tag, for display purposes.  E.g. <code>5.6</code> for name=Aperture
	 *
	 * @return the value of the Tag
	 */
	public String getValue() {
		return getDataAsString();
	}

	/**
	 * Provides the name of the Tag, for display purposes.  E.g. <code>Aperture</code>
	 *
	 * @return the name of the Tag
	 */
	public String getName() {
		return toString();
	}
}