package pixy.test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pixy.image.IBitmap;
import pixy.api.IMetadata;
import pixy.meta.adobe.ImageResourceID;
import pixy.meta.exif.ExifImageTag;
import pixy.meta.Metadata;
import pixy.meta.MetadataType;
import pixy.meta.adobe.IPTC_NAA;
import pixy.meta.adobe.AdobyMetadataBase;
import pixy.meta.exif.ExifMetaSegment;
import pixy.meta.exif.ExifSubTag;
import pixy.meta.exif.JpegExif;
import pixy.meta.exif.TiffExif;
import pixy.meta.iptc.IPTCApplicationTag;
import pixy.meta.iptc.IPTCFieldValue;
import pixy.meta.jpeg.JpegXMP;
import pixy.meta.xmp.XMP;
import pixy.image.exifFields.FieldType;
import pixy.util.MetadataUtils;

public class TestPixyMetaAndroid {
	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TestPixyMetaAndroid.class);
	
	public static void main(String[] args) throws IOException {
		Map<MetadataType, IMetadata> metadataMap = Metadata.readMetadata(args[0]);
		LOGGER.info("Start of metadata information:");
		LOGGER.info("Total number of metadata entries: {}", metadataMap.size());
		
		int i = 0;
		for(Map.Entry<MetadataType, IMetadata> entry : metadataMap.entrySet()) {
			LOGGER.info("Metadata entry {} - {}", i, entry.getKey());
			entry.getValue().showMetadata();
			i++;
			LOGGER.info("-----------------------------------------");
		}
		LOGGER.info("End of metadata information.");

		FileInputStream fin = null;
		FileOutputStream fout = null;
		
		if(metadataMap.get(MetadataType.XMP) != null) {
			XMP xmp = (XMP)metadataMap.get(MetadataType.XMP);
			fin = new FileInputStream("images/1.jpg");
			fout = new FileOutputStream("1-xmp-inserted.jpg");
			JpegXMP jpegXmp = new JpegXMP(xmp.getData());
			Metadata.insertXMP(fin, fout, jpegXmp);
			fin.close();
			fout.close();
		}
		
		Metadata.extractThumbnails("images/iptc-envelope.tif", "iptc-envelope");
	
		fin = new FileInputStream("images/iptc-envelope.tif");
		fout = new FileOutputStream("iptc-envelope-iptc-inserted.tif");
			
		Metadata.insertIPTC(fin, fout, createIPTCDataSet(), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/wizard.jpg");
		fout = new FileOutputStream("wizard-iptc-inserted.jpg");
		
		Metadata.insertIPTC(fin, fout, createIPTCDataSet(), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/1.jpg");
		fout = new FileOutputStream("1-irbthumbnail-inserted.jpg");
		
		Metadata.insertIRBThumbnail(fin, fout, createThumbnail("images/1.jpg"));
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/f1.tif");
		fout = new FileOutputStream("f1-irbthumbnail-inserted.tif");
		
		Metadata.insertIRBThumbnail(fin, fout, createThumbnail("images/f1.tif"));
		
		fin.close();
		fout.close();		

		fin = new FileInputStream("images/exif.tif");
		fout = new FileOutputStream("exif-exif-inserted.tif");
		
		Metadata.insertExif(fin, fout, populateExif(TiffExif.class), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/12.jpg");
		fout = new FileOutputStream("12-exif-inserted.jpg");

		Metadata.insertExif(fin, fout, populateExif(JpegExif.class), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/12.jpg");
		fout = new FileOutputStream("12-metadata-removed.jpg");
		
		Metadata.removeMetadata(fin, fout, MetadataType.JPG_JFIF, MetadataType.JPG_ADOBE, MetadataType.IPTC, MetadataType.ICC_PROFILE, MetadataType.XMP, MetadataType.EXIF);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/12.jpg");
		fout = new FileOutputStream("12-photoshop-iptc-inserted.jpg");
		
		Metadata.insertIRB(fin, fout, createPhotoshopIPTC(), true);
		
		fin.close();
		fout.close();
		
		fin = new FileInputStream("images/butterfly.png");
		fout = new FileOutputStream("comment-inserted.png");
		
		Metadata.insertComments(fin, fout, Arrays.asList("Comment1", "Comment2"));
		
		fin.close();
		fout.close();
	}
	
	private static IPTCFieldValue.IPTCFieldValueList createIPTCDataSet() {
		IPTCFieldValue.IPTCFieldValueList iptcs = new IPTCFieldValue.IPTCFieldValueList();
		iptcs.add(new IPTCFieldValue(IPTCApplicationTag.COPYRIGHT_NOTICE, "Copyright 2014-2016, yuwen_66@yahoo.com"));
		iptcs.add(new IPTCFieldValue(IPTCApplicationTag.CATEGORY, "ICAFE"));
		iptcs.add(new IPTCFieldValue(IPTCApplicationTag.KEY_WORDS, "Welcome 'icafe' user!"));
		
		return iptcs;
	}
	
	private static List<AdobyMetadataBase> createPhotoshopIPTC() {
		IPTC_NAA iptc = new IPTC_NAA(ImageResourceID.IPTC_NAA);
		iptc.addField(IPTCApplicationTag.COPYRIGHT_NOTICE, "Copyright 2014-2016, yuwen_66@yahoo.com");
		iptc.addField(IPTCApplicationTag.KEY_WORDS, "Welcome 'icafe' user!");
		iptc.addField(IPTCApplicationTag.CATEGORY, "ICAFE");
		
		return new ArrayList<AdobyMetadataBase>(Arrays.asList(iptc));
	}
	
	private static IBitmap createThumbnail(String filePath) throws IOException {
		FileInputStream fin = null;
		try {
			fin = new FileInputStream(filePath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		IBitmap thumbnail = MetadataUtils.createThumbnail(fin);
		
		fin.close();
		
		return thumbnail;
	}
	
	// This method is for testing only
	private static ExifMetaSegment populateExif(Class<?> exifClass) throws IOException {
		// Create an EXIF wrapper
		ExifMetaSegment exif = exifClass == (TiffExif.class)?new TiffExif() : new JpegExif();
		exif.addField(ExifImageTag.WINDOWS_XP_AUTHOR, FieldType.WINDOWSXP, "Author");
		exif.addField(ExifImageTag.WINDOWS_XP_KEYWORDS, FieldType.WINDOWSXP, "Copyright;Author");
		DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
		exif.addField(ExifSubTag.EXPOSURE_TIME, FieldType.RATIONAL, new int[] {10, 600});
		exif.addField(ExifSubTag.FNUMBER, FieldType.RATIONAL, new int[] {49, 10});
		exif.addField(ExifSubTag.ISO_SPEED_RATINGS, FieldType.SHORT, new short[]{273});
		//All four bytes should be interpreted as ASCII values - represents [0220] - new byte[]{48, 50, 50, 48}
		exif.addField(ExifSubTag.EXIF_VERSION, FieldType.UNDEFINED, "0220".getBytes());
		exif.addField(ExifSubTag.DATE_TIME_ORIGINAL, FieldType.ASCII, formatter.format(new Date()));
		exif.addField(ExifSubTag.DATE_TIME_DIGITIZED, FieldType.ASCII, formatter.format(new Date()));
		exif.addField(ExifSubTag.FOCAL_LENGTH, FieldType.RATIONAL, new int[] {240, 10});
		// Insert ThumbNailIFD
		// Since we don't provide thumbnail image, it will be created later from the input stream
		exif.setThumbnailRequired(true);
		
		return exif;
	}
}