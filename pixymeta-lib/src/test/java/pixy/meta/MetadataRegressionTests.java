package pixy.meta;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import pixy.api.DefaultApiImpl;
import pixy.api.IDataType;
import pixy.api.IDirectory;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.api.IMetadata;
import pixy.demo.j2se.TestPixyMetaJ2se;
import pixy.fileprocessor.jpg.JpegExifSegmentPlugin;
import pixy.fileprocessor.jpg.JpegAdobeIRBSegmentPlugin;
import pixy.fileprocessor.jpg.JpegJFIFSegmentPlugin;
import pixy.image.tiff.FieldType;
import pixy.image.tiff.Tag;
import pixy.io.IOUtils;
import pixy.fileprocessor.jpg.JpgFileProcessor;
import pixy.string.StringUtils;
import pixy.util.FileUtils;

// @RunWith(Parameterized.class)
@RunWith(JUnitParamsRunner.class)
public class MetadataRegressionTests {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataRegressionTests.class);
	private static final File OUTDIR = new File("./build/testresults/metadata");

	// resources in pixymeta-j2se-demo/resources/pixy.demo.j2se.images used by JUnitParamsRunner
	// for details see https://github.com/Pragmatists/JUnitParams/blob/master/src/test/java/junitparams/usage/SamplesOfUsageTest.java
	private static final String allTestFiles[] = new String[]{
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

	// used by JUnitParamsRunner fileName, dir, value
	private Object getAllFieldValuesForTest() throws IOException {
		ArrayList<Object> result = new ArrayList<>();
		for (String fileName : allTestFiles) {
			InputStream stream = null;
			try {
				stream = TestPixyMetaJ2se.class.getResourceAsStream("images/" + fileName);
				Map<MetadataType, IMetadata> metadataMap = Metadata.readMetadata(stream);
				if (metadataMap == null) continue;

				for (IMetadata exif : metadataMap.values()) {
					if (exif == null) continue;
					List<IDirectory> exifDir = exif.getMetaData();
					if (exifDir == null) continue;
					for (IDirectory dir : exifDir) {
						final List<IFieldValue> values = dir.getValues();
						if (values != null) {
							for (IFieldValue value : values) {
								if (value != null) {
									result.add(new Object[]{fileName,dir, value});
								}
							}
						}
					}
				}
			} finally {
				stream.close();

			}
		}
		return result;
	}

	@BeforeClass
	public static void initDirectories() {
		FileUtils.delete(OUTDIR, null);
		OUTDIR.mkdirs();
		JpegExifSegmentPlugin.register();
		JpegJFIFSegmentPlugin.register();
		JpegAdobeIRBSegmentPlugin.register();
	}

	@Test
	// @Parameters({"12.jpg"})
	@Parameters(method = "getAllFieldValuesForTest")
	public void declaredTypeShouldMatchFoundType(String fileName, IDirectory dir, IFieldValue value) throws IOException {
		final IFieldDefinition fieldDefinition = value.getDefinition();

		IDataType valueDataType = value.getDataType();
		IDataType defDataType = fieldDefinition.getDataType();

		if (!isCompatible(valueDataType, defDataType)) {
			String fieldDefinitionName = fieldDefinition.getName();
			if (fieldDefinition instanceof Tag) {
				fieldDefinitionName = StringUtils.toHexStringMM(((Tag) fieldDefinition).getValue()) +"-" + fieldDefinitionName;
			}

			// 0x0009-ExifVersion:Unknown => Undefined
			StringBuilder wrongExifFieldTypes = new StringBuilder()
					.append(fileName)
					.append(":")
					.append(dir.getName())
					.append(".")
					.append(fieldDefinitionName)
					.append(":").append(defDataType.getName())
					.append(" => ").append(valueDataType)
			;
			Assert.fail(wrongExifFieldTypes.toString());
		}

	}

	private boolean isCompatible(IDataType valueDataType, IDataType definitionDataType) {
		if (DefaultApiImpl.isNull(valueDataType)) return true;
		if (valueDataType == definitionDataType) return true;
		if (valueDataType.getName().toLowerCase().startsWith("un")) return true; // Unknown, Undefenied, ...

		// Long => Short is ok
		if ( (definitionDataType == FieldType.LONG) && (valueDataType == FieldType.SHORT)) return true;

		return false;
	}

	@Test
	// @Parameters({"12.jpg"})
	@Parameters(method = "getAllResourceImageNamesForTest")
	public void shouldFormat(String fileName) throws IOException
	{
		final File outdir = new File(OUTDIR, "shouldFormat");

		outdir.mkdirs();
		InputStream stream = TestPixyMetaJ2se.class.getResourceAsStream("images/" + fileName);
		Map<MetadataType, IMetadata> metadataMap = Metadata.readMetadata(stream);

		StringBuffer result = showMeta(fileName, metadataMap, outdir, true);
		stream.close();

		LOGGER.info(result.toString());

	}

	protected StringBuffer showMeta(String fileName, Map<MetadataType, IMetadata> metadataMap, File outdir,
									boolean showDetailed) throws FileNotFoundException {
		StringBuffer result = new StringBuffer();
		result.append("\n\n############\n").append(fileName).append("\n");

		for (Map.Entry<MetadataType, IMetadata> entry : metadataMap.entrySet()) {
			result.append(entry.getKey()).append("\n");

			IMetadata metaData = (showDetailed) ? entry.getValue() : null;
			 if (metaData != null) {
				 List<IDirectory> metaDir = metaData.getMetaData();
				 if (metaDir != null) {
					 for (IDirectory dir : metaDir) {
						 final List<IFieldValue> values = dir.getValues();
						 if (values != null) {
							 for (IFieldValue value : values) {
								 formatValue(result, dir, value);
							 }
						 }
					 }
					 result.append("\n----------------------\n");
				 }

				 String dbgMessage = metaData.getDebugMessage();
				 if (dbgMessage != null)
					 result.append(dbgMessage).append("\n----------------------\n");
			 }
		}

		if (showDetailed) {
			saveResultToFile(new File(outdir,fileName + ".txt"), result.toString());
		}
		return result;
	}

	private void saveResultToFile(File fileName, String result) throws FileNotFoundException {
		String charset = "UTF8";

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)));
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

	@Test
	// @Parameters({"12.jpg"})
	@Parameters(method = "getAllResourceImageNamesForTest")
	public void shouldCopyJpgFile(String fileName) throws IOException {
		if (fileName.toLowerCase().endsWith(".jpg")) {
			JpgFileProcessor doCopy = new JpgFileProcessor();

			InputStream inputStream = TestPixyMetaJ2se.class.getResourceAsStream("images/" + fileName);

			File outDir = new File(OUTDIR ,"shouldCopyJpgFile");
			outDir.mkdirs();

			final File outFile = new File(outDir, fileName);
			OutputStream outputStream = new FileOutputStream(outFile);

			try {
				doCopy.copyStream(inputStream, outputStream);
			} finally {
				inputStream.close();
				outputStream.close();

				Map<MetadataType, IMetadata> metadataMap = doCopy.getMetadataMap();
				StringBuffer result = showMeta(fileName, metadataMap, outDir, true);

				LOGGER.info(result.toString());
			}
			assertContentEqual(TestPixyMetaJ2se.class.getResourceAsStream("images/" + fileName), outFile);
		}
	}

	private void assertContentEqual(InputStream expected, File actual) throws IOException {
		byte[] actualContent = new byte[(int) actual.length()];
		FileInputStream isResult = new FileInputStream(actual);

		IOUtils.readFully(isResult, actualContent);
		isResult.close();

		byte[] expectedContent = new byte[(int) actual.length()];
		IOUtils.readFully(expected, expectedContent);
		expected.close();

		Assert.assertArrayEquals(expectedContent, actualContent);
	}


}