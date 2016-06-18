package pixy.meta;

import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import pixy.api.DefaultApiImpl;
import pixy.api.IDataType;
import pixy.api.IDirectory;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.demo.j2se.TestPixyMetaJ2se;

// @RunWith(Parameterized.class)
@RunWith(JUnitParamsRunner.class)
public class MetadataRegressionTests {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataRegressionTests.class);
	private static final String OUTDIR = "build/testresults/metadata/";

	// resources in pixymeta-j2se-demo/resources/pixy.demo.j2se.images used by JUnitParamsRunner
	// for details see https://github.com/Pragmatists/JUnitParams/blob/master/src/test/java/junitparams/usage/SamplesOfUsageTest.java
	private static final Object allTestFiles[] = new Object[]{
		"12.jpg",
		"wizard.jpg",
		"1.jpg",
		"10.jpg",
		"app13.jpg",
		"bedroom_arithmetic.jpg",
		"cmykjpg.jpg",
		"darwin-station-wheelie.jpg",
		"example.jpg",
		"exif-jpeg-thumbnail-sony-dsc-p150-inverted-colors.jpg",
		"exif-rgb-thumbnail-bad-exif-kodak-dc210.jpg",
		"exif-rgb-thumbnail-sony-d700.jpg",
		"flower.jpg",
		"Nikon.jpg",
		"sea.jpg",
		"table.jpg",

		"butterfly.png",
		"colourTestFakeBRG.png",
		"flowerpink-InMyEasterBonnet-KrystalHartley.png",
		"ProPhoto.png",
		"stonehouse.png",

		"exif.tif",
		"f1.tif",
		"iptc.tif",
		"iptc-envelope.tif",
		"multimax.tif",
		"packbits.tif",

		"butterfly.gif",
		"happy_trans-xmp-inserted.gif",

		"niagra_falls.bmp"

	};

	// used by JUnitParamsRunner
	private Object getAllResourceImageNamesForTest() {
		return allTestFiles;
	}

	@BeforeClass
	public static void initDirectories() {
		new File("./" + OUTDIR).mkdirs();
	}


	@Test
	@Parameters({"12.jpg"})
	// @Parameters(method = "getAllResourceImageNamesForTest")
	public void shouldFormat(String fileName) throws IOException
	{
		boolean showDetailed = true;
		StringBuffer result = new StringBuffer();
		result.append("\n\n############\n").append(fileName).append("\n");

		InputStream stream = TestPixyMetaJ2se.class.getResourceAsStream("images/" + fileName);
		Map<MetadataType, Metadata> metadataMap = Metadata.readMetadata(stream);

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

		if (showDetailed) {
			saveResultToFile(OUTDIR + fileName + ".txt", result.toString());
		}
		stream.close();

		LOGGER.info(result.toString());

	}

	private void saveResultToFile(String fileName, String result) throws FileNotFoundException {
		String charset = "UTF8";

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(fileName))));
		PrintWriter out = new PrintWriter(bw);
		out.print(result);
		out.flush();
		out.close();
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

			result.append("\t");

			if (dirName != null) result.append(dirName);

			if (fieldDefinitionName != null) result.append(".").append(fieldDefinitionName);
			if (dataTypeName != null) result.append("[").append(dataTypeName).append("]");
			if (valueAsString != null) result.append("=").append(valueAsString);
			result.append("\n");
        }
	}


}