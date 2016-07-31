package pixy.meta;

import org.junit.Assert;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import pixy.api.IMetadata;
import pixy.fileprocessor.jpg.JpgFileProcessor;

/**
 * Copyright (C) 2016 by k3b.
 *
 */
public class MetadataTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataTests.class);

	@Test
	public void shouldNotFind() throws IOException
	{
		String fileName = "NoMeta.jpg";
		InputStream inputStream = MetadataTests.class.getResourceAsStream("images/" + fileName);
		JpgFileProcessor doCopy = new JpgFileProcessor(inputStream);
		// doCopy.setDebugMessageBuffer(new StringBuilder());

		Assert.assertNotNull("open images/" + fileName, inputStream);

		try {
			doCopy.load();
		} finally {
			inputStream.close();

			Map<MetadataType, IMetadata> metadataMap = doCopy.getMetadataMap();

		}
	}
}