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
 * Comments.java
 *
 * Who   Date       Description
 * ====  =========  =================================================
 * WY    06Nov2015  Initial creation
 */

package pixy.meta.image;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.api.DefaultApiImpl;
import pixy.api.IDataType;
import pixy.api.IDirectory;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.meta.MetadataBase;
import pixy.meta.MetadataType;
import pixy.string.StringUtils;

public class Comments extends MetadataBase implements IDirectory, IFieldValue {
	private static final String NAME = "Comment";

	public static final IDataType StringLines = new DefaultApiImpl("StringLines");
	public static final IFieldDefinition CommentTag = new DefaultApiImpl(NAME, StringLines) {
		// must be its own type to allow matching tag.class => handler
	};

	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(Comments.class);
		
	private Queue<byte[]> queue;
	private List<String> comments = null;
	
	public Comments(byte[] comment) {
		super(MetadataType.COMMENT, null);
		queue = new LinkedList<byte[]>();
		comments = new ArrayList<String>();

		if (comment != null) {
			addComment(comment);
		}
	}
	
	public List<String> getComments() {
		ensureDataRead();
		return Collections.unmodifiableList(comments);
	}
	
	public void addComment(byte[] comment) {
		if(comment == null) throw new IllegalArgumentException("Input is null");
		queue.offer(comment);
	}
	
	public void addComment(String comment) {
		if(comment == null) throw new IllegalArgumentException("Input is null");
		comments.add(comment);
	}
	
	public void read() throws IOException {
		if(queue.size() > 0) {
			for(byte[] comment : queue) {
				try {
					comments.add(new String(comment, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					throw new UnsupportedEncodingException("UTF-8");
				}
			}
			queue.clear();
		}
	}
	
	@Override
	public void showMetadata() {
		ensureDataRead();
		
		LOGGER.info("Comments start =>");
		
		for (String comment : comments)
		    LOGGER.info("Comment: {}", comment);
		
		LOGGER.info("Comments end <=");
	}

	private List<IDirectory> directoryList = null;
	/**
	 * @return directories that belong to this MetaData
	 * */
	@Override
	public List<IDirectory> getMetaData() {
		ensureDataRead();
		if (directoryList == null) {
			directoryList = new ArrayList<IDirectory>();
			directoryList.add(this);
		}
		return directoryList;
	}

	@Override
	public IDirectory setName(String name) {
		return this;
	}

	@Override
	public String getName() {
		return NAME;
	}

	private List<IFieldValue> valueList = null;

	@Override
	public List<IFieldValue> getValues() {
		ensureDataRead();
		if (valueList == null) {
			valueList = new ArrayList<IFieldValue>();
			valueList.add(this);
		}
		return valueList;
	}

	@Override
	public IFieldDefinition getDefinition() {
		return CommentTag;
	}

	@Override
	public String getValueAsString() {
		return StringUtils.toStringLines(this.comments);
	}

	/**
	 * return values for fieldDefinition or null if it does not exist.
	 *
	 * @param fieldDefinition
	 */
	@Override
	public IFieldValue getValue(IFieldDefinition fieldDefinition) {
		if (CommentTag.equals(fieldDefinition)) return this;
		return null;
	}

	@Override
	public void setValue(String value) {
		this.comments = StringUtils.fromLines(value);
	}

	@Override
	public IDataType getDataType() {
		return StringLines;
	}
}