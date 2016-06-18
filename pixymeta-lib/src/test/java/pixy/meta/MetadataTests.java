package pixy.meta;

import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import pixy.api.DefaultApiImpl;
import pixy.api.IDataType;
import pixy.api.IDirectory;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
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
//		for (String fileName : allTestFiles) {
//			format(fileName, false);
//		}
		for (String fileName : allTestFiles) {
			format(fileName, true);
		}
	}

	private void format(String fileName, boolean showDetailed)
	{
		StringBuffer result = new StringBuffer();
		result.append("\n\n############\n").append(fileName).append("\n");
		InputStream stream = TestPixyMetaJ2se.class.getResourceAsStream(fileName);
		Map<MetadataType, Metadata> metadataMap = null;
		try {
			metadataMap = Metadata.readMetadata(stream);

			for (Map.Entry<MetadataType, Metadata> entry : metadataMap.entrySet()) {
				result.append(entry.getKey()).append("\n");
				 if (showDetailed) {
					 List<IDirectory> metaDir = (entry.getValue() != null) ? entry.getValue().getMetaData() : null;
					 if (metaDir != null) {
						 for (IDirectory dir : metaDir) {
							 final List<IFieldValue> values = dir.getValues();
							 if (values != null) {
								 for (IFieldValue value : values) {
									 formatValue(result, dir, value);
								 }
							 }
						 }
						 result.append("----------------------\n");
					 }
					 entry.getValue().showMetadata();
				 }
			}


		} catch (Exception e) {
			String context = "err processing " + fileName +	":";
			result.append(context).append(e.getMessage()).append("\n");
			LOGGER.error(context, e);
			e.printStackTrace();
		}
		LOGGER.info(result.toString());
	}

	// add "dirName.fieldDefinitionName[dataTypeName]=valueAsString" to result
	private void formatValue(StringBuffer result, IDirectory dir, IFieldValue value) {
		if (value != null) {
			final String dirName = dir.getName();
			final IFieldDefinition fieldDefinition = value.getDefinition();

			IDataType dataType = value.getDataType();
			if (DefaultApiImpl.isNull(dataType) && !DefaultApiImpl.isNull(fieldDefinition)) dataType = fieldDefinition.getDataType();

			final String dataTypeName = DefaultApiImpl.isNull(dataType) ? null : dataType.getName();
			final String valueAsString = value.getValueAsString();
			final String fieldDefinitionName = DefaultApiImpl.isNull(fieldDefinition) ? null :  fieldDefinition.getName();

			if (dirName != null) result.append(dirName);

			if (fieldDefinitionName != null) result.append(".").append(fieldDefinitionName);
			if (dataTypeName != null) result.append("[").append(dataTypeName).append("]");
			if (valueAsString != null) result.append("=").append(valueAsString);
			result.append("\n");
        }
	}


}