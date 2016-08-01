package pixy.meta;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.api.IMetadata;
import pixy.fileprocessor.jpg.JpegMetaDef;
import pixy.fileprocessor.jpg.JpgFileProcessor;
import pixy.meta.exif.ExifCompositeTag;
import pixy.meta.exif.ExifImageTag;
import pixy.meta.exif.ExifSubTag;
import pixy.meta.exif.GPSTag;
import pixy.meta.image.Comments;
import pixy.meta.iptc.IPTCApplicationTag;

/**
 * Copyright (C) 2016 by k3b.
 *
 */
@RunWith(JUnitParamsRunner.class)
public class MetadataTests {
	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataTests.class);

	private static final IFieldDefinition allExampleTags[] = new IFieldDefinition[]{
			Comments.CommentTag,

			ExifImageTag.DATETIME,
			ExifSubTag.USER_COMMENT,
			GPSTag.GPS_LATITUDE,
			ExifCompositeTag.GPS_LONGITUDE_EX
			// ,IPTCApplicationTag.KEY_WORDS
	};

	// used by JUnitParamsRunner
	private IFieldDefinition[] getExampleTags() {
		return allExampleTags;
	}

	@BeforeClass
	public static void initDirectories() {
		JpegMetaDef.register();
	}

	@Test
	@Parameters(method = "getExampleTags")
	public void shouldNotFind(IFieldDefinition tag) throws IOException
	{
		JpgFileProcessor doCopy = getMeta("NoMeta.jpg");

		IFieldValue found = doCopy.getValue(tag);
		Assert.assertEquals(null, found);
	}

	@Test
	@Parameters(method = "getExampleTags")
	public void shouldFind(IFieldDefinition tag) throws IOException
	{
		JpgFileProcessor doCopy = getMeta("WithMeta.jpg");

		IFieldValue found = doCopy.getValue(tag);
		Assert.assertNotNull(found);
	}

	protected JpgFileProcessor getMeta(String fileName) throws IOException {
		InputStream inputStream = MetadataTests.class.getResourceAsStream("images/" + fileName);
		Assert.assertNotNull("open images/" + fileName, inputStream);
		JpgFileProcessor doCopy = new JpgFileProcessor(inputStream);
		doCopy.load();
		return doCopy;
	}
}