package pixy.meta;

import org.junit.Test;
import org.junit.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import pixy.demo.j2se.TestPixyMetaJ2se;

public class MetadataTests  {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataTests.class);

	private static final String allTestFiles[] = new String[]{
		"images/12.jpg",
		"images/wizard.jpg",
		"images/1.jpg",
		"images/10.jpg",
		"images/app13.jpg",
		"images/bedroom_arithmetic.jpg",
		"images/cmykjpg.jpg",
		"images/darwin-station-wheelie.jpg",
		"images/example.jpg",
		"images/exif-jpeg-thumbnail-sony-dsc-p150-inverted-colors.jpg",
		"images/exif-rgb-thumbnail-bad-exif-kodak-dc210.jpg",
		"images/exif-rgb-thumbnail-sony-d700.jpg",
		"images/flower.jpg",
		"images/Nikon.jpg",
		"images/sea.jpg",
		"images/table.jpg",

		"images/butterfly.png",
		"images/colourTestFakeBRG.png",
		"images/flowerpink-InMyEasterBonnet-KrystalHartley.png",
		"images/ProPhoto.png",
		"images/stonehouse.png",

		"images/exif.tif",
		"images/f1.tif",
		"images/iptc.tif",
		"images/iptc-envelope.tif",
		"images/multimax.tif",
		"images/packbits.tif",

		"images/butterfly.gif",
		"images/happy_trans-xmp-inserted.gif",

		"images/niagra_falls.bmp"

	};

	@Test
	public void shouldFormat()  throws IOException {
		for (String fileName : allTestFiles) {
			format(fileName);
		}
	}

	private void format(String fileName)
	{
		StringBuffer result = new StringBuffer();
		result.append("\n\n############\n").append(fileName).append("\n");
		InputStream stream = TestPixyMetaJ2se.class.getResourceAsStream(fileName);
		Map<MetadataType, Metadata> metadataMap = null;
		try {
			metadataMap = Metadata.readMetadata(stream);

		} catch (Exception e) {
			result.append("err :").append(e.getMessage()).append("\n");
			e.printStackTrace();
		}
		LOGGER.info(result.toString());
	}


}