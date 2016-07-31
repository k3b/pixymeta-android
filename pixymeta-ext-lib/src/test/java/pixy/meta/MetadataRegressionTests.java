package pixy.meta;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import pixy.api.IMetadata;
import pixy.demo.j2se.TestPixyMetaJ2se;
import pixy.fileprocessor.jpg.report.MetaDataReport;
import pixy.io.IOUtils;
import pixy.fileprocessor.jpg.JpgFileProcessor;
import pixy.meta.jpeg.JPEGMeta;
import pixy.util.FileUtils;

/**
 * Copyright (C) 2016 by k3b.
 *
 * Regression test that verifies that the extracted metadata is the same as on the last run.
 *
 * Each image of the testimage-collection pixymeta-j2se-demo/src/main/resources/pixy/demo/j2se/images/imagefile.ext
 * has a corresponding pixymeta-j2se-demo/src/main/resources/pixy/demo/j2se/imageMetaExpected/imagefile.ext.txt
 * with the expected result
 *
 */
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
	// @Parameters({"iptc.tif"})
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

		result = showMetaAndVerify(fileName, metadataMap, outdir, true, null, null);
		stream.close();

		LOGGER.info(result.toString());

	}

	protected StringBuffer showMetaAndVerify(String fileName, Map<MetadataType, IMetadata> metadataMap, File outdir,
											 boolean showDetailed, InputStream resultComparePath,
											 String additionalInfo) throws FileNotFoundException {
		StringBuffer result = new StringBuffer();
		result.append("\n\n############\n").append(fileName).append("\n");

		if (additionalInfo != null) {
			result.append(additionalInfo).append("\n");
		}

		MetaDataReport reporter = new MetaDataReport();
		reporter.getReport(metadataMap, result);

		String resultString = result.toString();
		if (showDetailed) {
			saveResultToFile(new File(outdir,fileName + ".txt"), resultString);
		}

		if (resultComparePath != null) {
			String expected = readAll(resultComparePath);
			Assert.assertEquals(fileName, expected, resultString);
		}
		return result;
	}

	private void saveResultToFile(File fileName, String result) throws FileNotFoundException {
		PrintWriter out = null;
		try {
			out = new PrintWriter(fileName, "UTF-8");
			out.print(result);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} finally {
			out.flush();
			out.close();
		}
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

	@Test
	// @Parameters({"bedroom_arithmetic.jpg"})
	// @Parameters({"sea.jpg"}) // gps
	// @Parameters({"12.jpg"})
	// @Parameters({"app13.jpg"})
	@Parameters(method = "getAllResourceImageNamesForTest")
	public void shouldCopyJpgFile(String fileName) throws IOException {
		if (fileName.toLowerCase().endsWith(".jpg")) {

			InputStream inputStream = TestPixyMetaJ2se.class.getResourceAsStream("images/" + fileName);
			Assert.assertNotNull("open images/" + fileName, inputStream);

			JpgFileProcessor doCopy = new JpgFileProcessor(inputStream);
			doCopy.setDebugMessageBuffer(new StringBuilder());

			File outDir = new File(OUTDIR ,"shouldCopyJpgFile");
			outDir.mkdirs();

			final File outFile = new File(outDir, fileName);
			OutputStream outputStream = new FileOutputStream(outFile);

			try {
				doCopy.load();
				doCopy.save(outputStream);
			} finally {
				inputStream.close();
				outputStream.close();

				Map<MetadataType, IMetadata> metadataMap = doCopy.getMetadataMap();

				final String expectedResultPath = "imageMetaExpected/" + fileName + ".txt";
				InputStream expectedResultInputStream = TestPixyMetaJ2se.class.getResourceAsStream(expectedResultPath);
				StringBuffer result = showMetaAndVerify(fileName, metadataMap, outDir, true, expectedResultInputStream, doCopy.getDebugMessage());

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