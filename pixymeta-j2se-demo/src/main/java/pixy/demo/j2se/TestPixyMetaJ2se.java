package pixy.demo.j2se;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import pixy.image.IBitmap;
import pixy.meta.adobe.ImageResourceID;
import pixy.meta.exif.ExifImageTag;
import pixy.image.exifFields.FieldType;
import pixy.j2se.BitmapFactoryNative;
import pixy.api.IMetadata;
import pixy.meta.Metadata;
import pixy.meta.MetadataType;
import pixy.meta.adobe.AdobyMetadataBase;
import pixy.meta.adobe.IPTC_NAA;
import pixy.meta.exif.ExifMetaSegment;
import pixy.meta.exif.ExifSubTag;
import pixy.meta.exif.JpegExif;
import pixy.meta.exif.TiffExif;
import pixy.meta.iptc.IPTCApplicationTag;
import pixy.meta.iptc.IPTCFieldValue;
import pixy.meta.jpeg.JPEGMeta;
import pixy.meta.jpeg.JpegXMP;
import pixy.meta.xmp.XMP;
import pixy.util.MetadataUtils;

/**
 * Created by k3b on 01.06.2016.
 */
public class TestPixyMetaJ2se {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(TestPixyMetaJ2se.class);
    private static final String OUTDIR = "build/testresults/";

    public static void main(String[] args) throws IOException {
        BitmapFactoryNative.init();
        JPEGMeta.register();

        new File("./" + OUTDIR).mkdirs();
        InputStream fin = null;
        FileOutputStream fout = null;

        if (args.length > 0) {
            Map<MetadataType, IMetadata> metadataMap = Metadata.readMetadata(args[0]);
            LOGGER.info("Start of metadata information:");
            LOGGER.info("Total number of metadata entries: {}", metadataMap.size());

            int i = 0;
            for (Map.Entry<MetadataType, IMetadata> entry : metadataMap.entrySet()) {
                LOGGER.info("Metadata entry {} - {}", i, entry.getKey());
                entry.getValue().showMetadata();
                i++;
                LOGGER.info("-----------------------------------------");
            }
            LOGGER.info("End of metadata information.");

            if (metadataMap.get(MetadataType.XMP) != null) {
                XMP xmp = (XMP) metadataMap.get(MetadataType.XMP);
                fin = TestPixyMetaJ2se.class.getResourceAsStream("images/1.jpg");
                fout = new FileOutputStream(OUTDIR + "1-xmp-inserted.jpg");
                JpegXMP jpegXmp = new JpegXMP(xmp.getData());
                Metadata.insertXMP(fin, fout, jpegXmp);
                fin.close();
                fout.close();
            }

        }

        Metadata.extractThumbnails(TestPixyMetaJ2se.class.getResourceAsStream("images/iptc-envelope.tif"), OUTDIR + "iptc-envelope");

        fin = TestPixyMetaJ2se.class.getResourceAsStream("images/iptc-envelope.tif");
        fout = new FileOutputStream(OUTDIR + "iptc-envelope-iptc-inserted.tif");

        Metadata.insertIPTC(fin, fout, createIPTCDataSet(), true);

        fin.close();
        fout.close();

        fin = TestPixyMetaJ2se.class.getResourceAsStream("images/wizard.jpg");
        fout = new FileOutputStream(OUTDIR + "wizard-iptc-inserted.jpg");

        Metadata.insertIPTC(fin, fout, createIPTCDataSet(), true);

        fin.close();
        fout.close();

        fin = TestPixyMetaJ2se.class.getResourceAsStream("images/1.jpg");
        fout = new FileOutputStream(OUTDIR + "1-irbthumbnail-inserted.jpg");

        Metadata.insertIRBThumbnail(fin, fout, createThumbnail("images/1.jpg"));

        fin.close();
        fout.close();

        fin = TestPixyMetaJ2se.class.getResourceAsStream("images/f1.tif");
        // fin = new FileInputStream("D:\\prj\\eve\\android\\prj\\fotos-android.wrk\\downloaded\\pixymeta-android\\pixymeta-j2se-demo\\src\\main\\resources\\pixy\\demo\\j2se\\images\\f1.tif");

        fout = new FileOutputStream(OUTDIR + "f1-irbthumbnail-inserted.tif");

        //!!! this currently fails in j2se
        IBitmap thumbnail = createThumbnail("images/f1.tif");

        if (thumbnail != null) {
            Metadata.insertIRBThumbnail(fin, fout, thumbnail);
        }

        fin.close();
        fout.close();

        fin = TestPixyMetaJ2se.class.getResourceAsStream("images/exif.tif");
        fout = new FileOutputStream(OUTDIR + "exif-exif-inserted.tif");

        Metadata.insertExif(fin, fout, populateExif(TiffExif.class), true);

        fin.close();
        fout.close();

        fin = TestPixyMetaJ2se.class.getResourceAsStream("images/12.jpg");
        fout = new FileOutputStream(OUTDIR + "12-exif-inserted.jpg");

        Metadata.insertExif(fin, fout, populateExif(JpegExif.class), true);

        fin.close();
        fout.close();

        fin = TestPixyMetaJ2se.class.getResourceAsStream("images/12.jpg");
        fout = new FileOutputStream(OUTDIR + "12-metadata-removed.jpg");

        Metadata.removeMetadata(fin, fout, MetadataType.JPG_JFIF, MetadataType.JPG_ADOBE, MetadataType.IPTC, MetadataType.ICC_PROFILE, MetadataType.XMP, MetadataType.EXIF);

        fin.close();
        fout.close();

        fin = TestPixyMetaJ2se.class.getResourceAsStream("images/12.jpg");
        fout = new FileOutputStream(OUTDIR + "12-photoshop-iptc-inserted.jpg");

        Metadata.insertIRB(fin, fout, createPhotoshopIPTC(), true);

        fin.close();
        fout.close();

        fin = TestPixyMetaJ2se.class.getResourceAsStream("images/butterfly.png");
        fout = new FileOutputStream(OUTDIR + "comment-inserted.png");

        //!!! this currently fails in j2se
        try {
            Metadata.insertComments(fin, fout, Arrays.asList("Comment1", "Comment2"));
        } catch (Exception ex) {
            LOGGER.error("cannot set comment to images/butterfly.png", ex);
        }
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
        IBitmap thumbnail = null;
                InputStream fin = null;
        try {
            fin = TestPixyMetaJ2se.class.getResourceAsStream(filePath);
            thumbnail = MetadataUtils.createThumbnail(fin);

        } catch (Exception e) {
            LOGGER.error("createThumbnail('" + filePath + "') failed", e);
        }

        if (fin != null) {
            fin.close();
        }

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
