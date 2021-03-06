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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import pixy.io.RandomAccessInputStream;
import pixy.io.RandomAccessOutputStream;
import pixy.meta.exif.*;
import pixy.meta.tiff.TIFFMetaUtils;

/**
 * TIFF image wrapper to manipulate pages and fields
 * 
 * @author Wen Yu, yuwen_66@yahoo.com
 * @version 1.0 05/23/2014
 */
public class TIFFImage implements Iterable<pixy.image.exifFields.IFD> {
	// Define fields
	private int numOfPages;
	private int workingPage;
	private List<pixy.image.exifFields.IFD> ifds;
	private RandomAccessInputStream rin;

	public TIFFImage(RandomAccessInputStream rin) throws IOException {
		ifds = new ArrayList<pixy.image.exifFields.IFD>();
		this.rin = rin;
		IfdMetaUtils.readIFDs(ifds, rin);
		this.numOfPages = ifds.size();
		this.workingPage = 0;
	}
	
	public void addField(ExifField<?> field) {
		ifds.get(workingPage).addField(field);
	}
	
	public ExifField<?> getField(pixy.meta.exif.Tag tag) {
		return ifds.get(workingPage).getField(tag);
	}
	
	public List<pixy.image.exifFields.IFD> getIFDs() {
		return Collections.unmodifiableList(ifds);
	}
	
	public RandomAccessInputStream getInputStream() {
		return rin;
	}
	
	public int getNumOfPages() {
		return numOfPages;
	}
	
	public ExifField<?> removeField(pixy.meta.exif.Tag tag) {
		return ifds.get(workingPage).removeField(tag);
	}
	
	public pixy.image.exifFields.IFD removePage(int index) {
		pixy.image.exifFields.IFD removed = ifds.remove(index);
		numOfPages--;
		
		return removed;
	}
	
	public void setWorkingPage(int workingPage) {
		if(workingPage >= 0 && workingPage < numOfPages)
			this.workingPage = workingPage;
		else
			throw new IllegalArgumentException("Invalid page number: " + workingPage);
	}
	
	public void write(RandomAccessOutputStream out) throws IOException {
		// Reset pageNumber if we have more than 1 pages
		if(numOfPages > 1) { 
			for(int i = 0; i < ifds.size(); i++) {
				ifds.get(i).removeField(ExifImageTag.PAGE_NUMBER);
				ifds.get(i).addField(new pixy.image.exifFields.ShortField(ExifImageTag.PAGE_NUMBER, new short[]{(short)i, (short)(numOfPages - 1)}));
			}
		}
		TIFFMetaUtils.write(this, out);
	}

	public Iterator<pixy.image.exifFields.IFD> iterator() {
		return ifds.iterator();
	}
}