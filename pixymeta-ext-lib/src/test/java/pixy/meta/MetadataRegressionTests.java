package pixy.meta;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import pixy.api.DefaultApiImpl;
import pixy.api.IDataType;
import pixy.api.IDirectory;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.api.IMetadata;
import pixy.demo.j2se.TestPixyMetaJ2se;
import pixy.image.exifFields.FieldType;
import pixy.meta.exif.Tag;
import pixy.io.IOUtils;
import pixy.fileprocessor.jpg.JpgFileProcessor;
import pixy.meta.jpeg.JPEGMeta;
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
	static final String allTestFiles[] = new String[]{
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
		FileUtils.delete(OUTDIR, null);
		OUTDIR.mkdirs();
		JPEGMeta.register();
	}

	@Test
	// @Parameters({"12.jpg"})
	@Parameters(method = "getAllResourceImageNamesForTest")
	public void shouldFormat(String fileName) throws IOException
	{
		final File outdir = new File(OUTDIR, "shouldFormat");

		outdir.mkdirs();
		InputStream stream = TestPixyMetaJ2se.class.getResourceAsStream("images/" + fileName);
		Map<MetadataType, IMetadata> metadataMap = null;
		StringBuffer result = null;
		try {
			metadataMap = Metadata.readMetadata(stream);
			// result.append(ex.getMessage()).append("\n").append(ex.getStackTrace()).append("\n");
		} catch (Exception ex) {
		} finally {
		}

		result = showMetaAndVerify(fileName, metadataMap, outdir, true, null);
		stream.close();

		LOGGER.info(result.toString());

	}

	protected StringBuffer showMetaAndVerify(String fileName, Map<MetadataType, IMetadata> metadataMap, File outdir,
											 boolean showDetailed, InputStream resultComparePath) throws FileNotFoundException {
		StringBuffer result = new StringBuffer();
		result.append("\n\n############\n").append(fileName).append("\n");

		final Set<MetadataType> metadataTypeSet = metadataMap.keySet();
		MetadataType[] keys = metadataTypeSet.toArray(new MetadataType[metadataTypeSet.size()]);
		Arrays.sort(keys);
		for (MetadataType key : keys) {
			result.append(key).append("\n");

			IMetadata metaData = (showDetailed) ? metadataMap.get(key) : null;
			 if (metaData != null) {
				 try {
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

				 } catch (Exception ex) {
					 result.append(ex.getMessage()).append("\n").append(ex.getStackTrace()).append("\n");
				 } finally {
					 String dbgMessage = metaData.getDebugMessage();
					 if (dbgMessage != null)
						 result.append(dbgMessage).append("\n----------------------\n");
				 }
			 }
		}

		if (showDetailed) {
			saveResultToFile(new File(outdir,fileName + ".txt"), result.toString());
		}

		if (resultComparePath != null) {
			String expected = readAll(resultComparePath);
			Assert.assertEquals(fileName, expected, result.toString());
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

	private String readAll(InputStream inStream) {
		BufferedReader in = null;

		try {
			StringBuilder builder = new StringBuilder();
			in = new BufferedReader(new InputStreamReader(inStream));
			String str;
			while ((str = in.readLine()) != null) {
				builder.append(str).append("\n");
			}
			return builder.toString();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {}
			}
		}
		return null;
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
	@Parameters({"app13.jpg"})
	// @Parameters(method = "getAllResourceImageNamesForTest")
	public void shouldCopyJpgFile(String fileName) throws IOException {
		if (fileName.toLowerCase().endsWith(".jpg")) {
			JpgFileProcessor doCopy = new JpgFileProcessor();

			InputStream inputStream = TestPixyMetaJ2se.class.getResourceAsStream("images/" + fileName);
			Assert.assertNotNull("open images/" + fileName, inputStream);

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

				final String expectedResultPath = "imageMetaExpected/" + fileName + ".txt";
				InputStream expectedResultInputStream = TestPixyMetaJ2se.class.getResourceAsStream(expectedResultPath);
				StringBuffer result = showMetaAndVerify(fileName, metadataMap, outDir, true, expectedResultInputStream);

				if (expectedResultInputStream == null) {
					Assert.fail("Cannot open compare file " + expectedResultPath);
				}

				expectedResultInputStream.close();

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