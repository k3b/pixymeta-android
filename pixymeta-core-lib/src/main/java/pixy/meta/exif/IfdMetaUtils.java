package pixy.meta.exif;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import pixy.image.exifFields.ExifField;
import pixy.image.exifFields.ExifFieldEnum;
import pixy.image.jpeg.JpegSegmentMarker;
import pixy.image.exifFields.ASCIIField;
import pixy.image.exifFields.ByteField;
import pixy.image.exifFields.DoubleField;
import pixy.image.exifFields.FieldType;
import pixy.image.exifFields.FloatField;
import pixy.image.exifFields.IFD;
import pixy.image.exifFields.IFDField;
import pixy.image.exifFields.LongField;
import pixy.image.exifFields.RationalField;
import pixy.image.exifFields.SRationalField;
import pixy.image.exifFields.ShortField;
import pixy.image.exifFields.UndefinedField;
import pixy.io.IOUtils;
import pixy.io.RandomAccessInputStream;
import pixy.io.RandomAccessOutputStream;
import pixy.io.ReadStrategyII;
import pixy.io.ReadStrategyMM;
import pixy.io.WriteStrategyII;
import pixy.io.WriteStrategyMM;
import pixy.string.StringUtils;

/**
 * Copyright (C) 2016 by k3b.
 */
public class IfdMetaUtils {
    // Obtain a logger instance
    private static final Logger LOGGER = LoggerFactory.getLogger(IfdMetaUtils.class);
    public static final int STREAM_HEAD = 0x00;
    // Offset where to write the value of the first IFD offset
    public static final int OFFSET_TO_WRITE_FIRST_IFD_OFFSET = 0x04;
    public static final int FIRST_WRITE_OFFSET = 0x08;

    protected static int readHeader(RandomAccessInputStream rin) throws IOException {
        int offset = 0;
        // First 2 bytes determine the byte order of the file
        rin.seek(STREAM_HEAD);
        short endian = rin.readShort();
        offset += 2;

        if (endian == IOUtils.BIG_ENDIAN) {
            rin.setReadStrategy(ReadStrategyMM.getInstance());
        } else if(endian == IOUtils.LITTLE_ENDIAN) {
            rin.setReadStrategy(ReadStrategyII.getInstance());
        } else {
            rin.close();
            throw new RuntimeException("Invalid TIFF byte order");
        }

        // Read TIFF identifier
        rin.seek(offset);
        short tiff_id = rin.readShort();
        offset +=2;

        if(tiff_id!=0x2a) { //"*" 42 decimal
            rin.close();
            throw new RuntimeException("Invalid TIFF identifier");
        }

        rin.seek(offset);
        offset = rin.readInt();

        return offset;
    }

    private static int readIFD(IFD parent, Tag parentTag, Class<? extends Tag> tagClass, RandomAccessInputStream rin, List<IFD> list, int offset) throws IOException {
        // Use reflection to invoke fromShort(short) fromShort
        Method fromShort = getFromShortMethod(tagClass);
        IFD tiffIFD = new IFD();
        if (tagClass != null) tiffIFD.setName("IFD[" + tagClass.getSimpleName() + "]");
        rin.seek(offset);
        int no_of_fields = rin.readShort();
        offset += 2;

        for (int i = 0; i < no_of_fields; i++) {
            rin.seek(offset);
            short tag = rin.readShort();
            offset += 2;
            rin.seek(offset);
            short type = rin.readShort();
            FieldType ftype = FieldType.fromShort(type);
            Tag ftag = getTagFromId(fromShort, tag, ftype);
            offset += 2;
            rin.seek(offset);
            int field_length = rin.readInt();
            offset += 4;
            ////// Try to read actual data.
            switch (ftype)
            {
                case BYTE:
                case UNDEFINED:
                    byte[] data = new byte[field_length];
                    rin.seek(offset);
                    if(field_length <= 4) {
                        rin.readFully(data, 0, field_length);
                    } else {
                        rin.seek(rin.readInt());
                        rin.readFully(data, 0, field_length);
                    }
                    ExifField<byte[]> byteField = null;
                    if(ftype == FieldType.BYTE)
                        byteField = new ByteField(ftag, data);
                    else
                        byteField = new UndefinedField(ftag, data);
                    addField(tiffIFD, byteField);
                    offset += 4;
                    break;
                case ASCII:
                    data = new byte[field_length];
                    if(field_length <= 4) {
                        rin.seek(offset);
                        rin.readFully(data, 0, field_length);
                    }
                    else {
                        rin.seek(offset);
                        rin.seek(rin.readInt());
                        rin.readFully(data, 0, field_length);
                    }
                    ExifField<String> ascIIField = new ASCIIField(ftag, new String(data, 0, data.length, "UTF-8"));
                    addField(tiffIFD, ascIIField);
                    offset += 4;
                    break;
                case SHORT:
                    short[] sdata = new short[field_length];
                    if(field_length == 1) {
                      rin.seek(offset);
                      sdata[0] = rin.readShort();
                      offset += 4;
                    } else if (field_length == 2) {
                        rin.seek(offset);
                        sdata[0] = rin.readShort();
                        offset += 2;
                        rin.seek(offset);
                        sdata[1] = rin.readShort();
                        offset += 2;
                    } else {
                        rin.seek(offset);
                        int toOffset = rin.readInt();
                        offset += 4;
                        for (int j = 0; j  <field_length; j++){
                            rin.seek(toOffset);
                            sdata[j] = rin.readShort();
                            toOffset += 2;
                        }
                    }
                    ExifField<short[]> shortField = new ShortField(ftag, sdata);
                    addField(tiffIFD, shortField);
                    break;
                case LONG:
                    int[] ldata = new int[field_length];
                    if(field_length == 1) {
                      rin.seek(offset);
                      ldata[0] = rin.readInt();
                      offset += 4;
                    } else {
                        rin.seek(offset);
                        int toOffset = rin.readInt();
                        offset += 4;
                        for (int j=0;j<field_length; j++){
                            rin.seek(toOffset);
                            ldata[j] = rin.readInt();
                            toOffset += 4;
                        }
                    }
                    ExifField<int[]> longField = new LongField(ftag, ldata);
                    addField(tiffIFD, longField);

                    if ((ftag == ExifImageTag.EXIF_SUB_IFD) && (ldata[0]!= 0)) {
                        readSubIFD(tiffIFD, ExifImageTag.EXIF_SUB_IFD, ExifSubTag.class, rin, null, ldata[0]);
                    } else if ((ftag == ExifImageTag.GPS_SUB_IFD) && (ldata[0] != 0)) {
                        readSubIFD(tiffIFD, ExifImageTag.GPS_SUB_IFD, GPSTag.class, rin, null, ldata[0]);
                    } else if((ftag == ExifSubTag.EXIF_INTEROPERABILITY_OFFSET) && (ldata[0] != 0)) {
                        readSubIFD(tiffIFD, ExifSubTag.EXIF_INTEROPERABILITY_OFFSET, InteropTag.class, rin, null, ldata[0]);
                    } else if (ftag == ExifImageTag.SUB_IFDS) {
                        for(int ifd = 0; ifd < ldata.length; ifd++) {
                            readSubIFD(tiffIFD, ExifImageTag.SUB_IFDS, ExifImageTag.class, rin, null, ldata[0]);
                        }
                    }
                    break;
                case FLOAT:
                    float[] fdata = new float[field_length];
                    if(field_length == 1) {
                      rin.seek(offset);
                      fdata[0] = rin.readFloat();
                      offset += 4;
                    } else {
                        rin.seek(offset);
                        int toOffset = rin.readInt();
                        offset += 4;
                        for (int j=0;j<field_length; j++){
                            rin.seek(toOffset);
                            fdata[j] = rin.readFloat();
                            toOffset += 4;
                        }
                    }
                    ExifField<float[]> floatField = new FloatField(ftag, fdata);
                    addField(tiffIFD, floatField);

                    break;
                case DOUBLE:
                    double[] ddata = new double[field_length];
                    rin.seek(offset);
                    int toOffset = rin.readInt();
                    offset += 4;
                    for (int j=0;j<field_length; j++){
                        rin.seek(toOffset);
                        ddata[j] = rin.readDouble();
                        toOffset += 8;
                    }
                    ExifField<double[]> doubleField = new DoubleField(ftag, ddata);
                    addField(tiffIFD, doubleField);

                    break;
                case RATIONAL:
                case SRATIONAL:
                    int len = 2*field_length;
                    ldata = new int[len];
                    rin.seek(offset);
                    toOffset = rin.readInt();
                    offset += 4;
                    for (int j=0;j<len; j+=2){
                        rin.seek(toOffset);
                        ldata[j] = rin.readInt();
                        toOffset += 4;
                        rin.seek(toOffset);
                        ldata[j+1] = rin.readInt();
                        toOffset += 4;
                    }
                    ExifField<int[]> rationalField = null;
                    if(ftype == FieldType.SRATIONAL) {
                        rationalField = new SRationalField(ftag, ldata);
                    } else {
                        rationalField = new RationalField(ftag, ldata);
                    }
                    addField(tiffIFD, rationalField);

                    break;
                case IFD:
                    ldata = new int[field_length];
                    if(field_length == 1) {
                      rin.seek(offset);
                      ldata[0] = rin.readInt();
                      offset += 4;
                    } else {
                        rin.seek(offset);
                        toOffset = rin.readInt();
                        offset += 4;
                        for (int j=0;j<field_length; j++){
                            rin.seek(toOffset);
                            ldata[j] = rin.readInt();
                            toOffset += 4;
                        }
                    }
                    ExifField<int[]> ifdField = new IFDField(ftag, ldata);
                    addField(tiffIFD, ifdField);
                    for(int ifd = 0; ifd < ldata.length; ifd++) {
                        readIFD(tiffIFD, ExifImageTag.SUB_IFDS, ExifImageTag.class, rin, null, ldata[0]);
                    }

                    break;
                default:
                    offset += 4;
                    break;
            }
        }
        // If this is a child IFD, add it to its parent
        if(parent != null)
            parent.addChild(parentTag, tiffIFD);
        else // Otherwise, add to the main IFD list
            list.add(tiffIFD);
        rin.seek(offset);

        return rin.readInt();
    }

    /** The same as {@link IfdMetaUtils#readIFD(IFD, Tag, Class, RandomAccessInputStream, List, int)}
     * but with internal error handling
     */
    private static int readSubIFD(IFD parent, Tag parentTag, Class<? extends Tag> tagClass,
                                  RandomAccessInputStream rin, List<IFD> list, int offset) {
        try {
            return readIFD(parent, parentTag, tagClass, rin, list, offset);
            //readIFD(tiffIFD, ExifImageTag.GPS_SUB_IFD, GPSTag.class, rin, null, ldata[0]);
        } catch(Exception e) {
            String message = "Skipping because of error: sub-IFD '" + parentTag
                    + "' for '" + tagClass.getSimpleName() +
                    ".* from '" + parent
                    + "' : " + e.getMessage();
            LOGGER.error(message,e);
            parent.removeField(parentTag);
        }
        return 0;
    }

    private static void addField(IFD targetIFD, ExifField<?> field) {
        targetIFD.addField(field);
    }

    static Tag getTagFromId(Method fromShortMethod, short tag, FieldType ftype) {
        Tag ftag = ExifImageTag.UNKNOWN;
        try {
            ftag = (Tag) fromShortMethod.invoke(null, tag);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        if (ftag == ExifImageTag.UNKNOWN) {
            ftag = new UnknownTag(tag, "(?? " + Integer.toHexString(tag&0xffff) +" ??)", ftype);
        }

        return ftag;
    }

    // Use reflection to invoke fromShort(short) fromShort
    static Method getFromShortMethod(Class<? extends Tag> tagClass) {
        Method fromShort = null;
        try {
            fromShort = tagClass.getDeclaredMethod("fromShort", short.class);
        } catch (NoSuchMethodException e) {
            LOGGER.error("enum " + tagClass.getName() +
                    " does not implement static Tag fromShort(short id) ");
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return fromShort;
    }

    protected static void readIFDs(IFD parent, Tag parentTag, Class<? extends Tag> tagClass, List<IFD> list, int offset, RandomAccessInputStream rin) throws IOException {
        // Read the IFDs into a list first
        while (offset != 0)	{
            offset = readIFD(parent, parentTag, tagClass, rin, list, offset);
        }
    }

    public static void readIFDs(List<IFD> list, RandomAccessInputStream rin) throws IOException {
        int offset = readHeader(rin);
        readIFDs(null, null, ExifImageTag.class, list, offset, rin);
    }

    public static void printIFDs(Collection<IFD> list, String indent) {
        int id = 0;
        LOGGER.info("Printing IFDs ... ");

        for(IFD currIFD : list) {
            LOGGER.info("IFD #{}", id);
            printIFD(currIFD, ExifImageTag.class, indent);
            id++;
        }
    }

    public static void printIFD(IFD currIFD, Class<? extends Tag> tagClass, String indent) {
        StringBuilder ifd = new StringBuilder();
        print(currIFD, tagClass, indent, ifd);
        LOGGER.info("\n{}", ifd);
    }

    private static void print(IFD currIFD, Class<? extends Tag> tagClass, String indent, StringBuilder ifds) {
        // Use reflection to invoke fromShort(short) fromShort
        Method fromShort = IfdMetaUtils.getFromShortMethod(tagClass);
        Collection<ExifField<?>> fields = currIFD.getFields();
        int i = 0;

        for(ExifField<?> field : fields) {
            ifds.append(indent);
            ifds.append("Field #" + i + "\n");
            ifds.append(indent);
            FieldType ftype = field.getType();
            short tag = field.getTag();
            Tag ftag = ExifImageTag.UNKNOWN;
            if(tag == ExifSubTag.PADDING.getValue()) {
                ftag = ExifSubTag.PADDING;
            } else  {
                ftag = IfdMetaUtils.getTagFromId(fromShort, tag, ftype);
            }
            if (ftag == ExifImageTag.UNKNOWN) {
                LOGGER.warn("Tag: {} {}{}{} {}", ftag, "[Value: 0x", Integer.toHexString(tag&0xffff), "]", "(Unknown)");
            } else {
                ifds.append("Tag: " + ftag + "\n");
            }
            ifds.append(indent);
            ifds.append("Field type: " + ftype + "\n");
            int field_length = field.getLength();
            ifds.append(indent);
            ifds.append("Field length: " + field_length + "\n");
            ifds.append(indent);

            String suffix = null;
            if(ftype == FieldType.SHORT || ftype == FieldType.SSHORT)
                suffix = ftag.getFieldAsString(field.getDataAsLong());
            else
                suffix = ftag.getFieldAsString(field.getData());

            ifds.append("Field value: " + field.getDataAsString() + (StringUtils.isNullOrEmpty(suffix)?"":" => " + suffix) + "\n");

            i++;
        }

        Map<Tag, IFD> children = currIFD.getChildren();

        if(children.get(ExifImageTag.EXIF_SUB_IFD) != null) {
            ifds.append(indent + "--------- ");
            ifds.append("<<ExifMetaSegment SubIFD starts>>\n");
            print(children.get(ExifImageTag.EXIF_SUB_IFD), ExifSubTag.class, indent + "--------- ", ifds);
            ifds.append(indent + "--------- ");
            ifds.append("<<ExifMetaSegment SubIFD ends>>\n");
        }

        if(children.get(ExifImageTag.GPS_SUB_IFD) != null) {
            ifds.append(indent + "--------- ");
            ifds.append("<<GPS SubIFD starts>>\n");
            print(children.get(ExifImageTag.GPS_SUB_IFD), GPSTag.class, indent + "--------- ", ifds);
            ifds.append(indent + "--------- ");
            ifds.append("<<GPS SubIFD ends>>\n");
        }
    }
    public static int retainPages(int startPage, int endPage, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
        if(startPage < 0 || endPage < 0)
            throw new IllegalArgumentException("Negative start or end page");
        else if(startPage > endPage)
            throw new IllegalArgumentException("Start page is larger than end page");

        List<IFD> list = new ArrayList<IFD>();

        int offset = copyHeader(rin, rout);

        // Step 1: read the IFDs into a list first
        IfdMetaUtils.readIFDs(null, null, ExifImageTag.class, list, offset, rin);
        // Step 2: remove pages from a multiple page TIFF
        int pagesRetained = list.size();
        List<IFD> newList = new ArrayList<IFD>();
        if(startPage <= list.size() - 1)  {
            if(endPage > list.size() - 1) endPage = list.size() - 1;
            for(int i = endPage; i >= startPage; i--) {
                newList.add(list.get(i));
            }
        }
        if(newList.size() > 0) {
            pagesRetained = newList.size();
            list.retainAll(newList);
        }
        // Reset pageNumber for the existing pages
        for(int i = 0; i < list.size(); i++) {
            list.get(i).removeField(ExifImageTag.PAGE_NUMBER);
            addField(list.get(i), new ShortField(ExifImageTag.PAGE_NUMBER, new short[]{(short)i, (short)(list.size() - 1)}));
        }
        // End of removing pages
        // Step 3: copy the remaining pages
        // 0x08 is the first write offset
        int writeOffset = FIRST_WRITE_OFFSET;
        offset = copyPages(list, writeOffset, rin, rout);
        int firstIFDOffset = list.get(0).getStartOffset();

        writeToStream(rout, firstIFDOffset);

        return pagesRetained;
    }

    // Return number of pages retained
    public static int retainPages(RandomAccessInputStream rin, RandomAccessOutputStream rout, int... pages) throws IOException {
        List<IFD> list = new ArrayList<IFD>();

        int offset = copyHeader(rin, rout);
        // Step 1: read the IFDs into a list first
        IfdMetaUtils.readIFDs(null, null, ExifImageTag.class, list, offset, rin);
        // Step 2: remove pages from a multiple page TIFF
        int pagesRetained = list.size();
        List<IFD> newList = new ArrayList<IFD>();
        Arrays.sort(pages);
        for(int i = pages.length - 1; i >= 0; i--) {
            if(pages[i] >= 0 && pages[i] < list.size())
                newList.add(list.get(pages[i]));
        }
        if(newList.size() > 0) {
            pagesRetained = newList.size();
            list.retainAll(newList);
        }
        // End of removing pages
        // Reset pageNumber for the existing pages
        for(int i = 0; i < list.size(); i++) {
            list.get(i).removeField(ExifImageTag.PAGE_NUMBER);
            addField(list.get(i), new ShortField(ExifImageTag.PAGE_NUMBER, new short[]{(short)i, (short)(list.size() - 1)}));
        }
        // Step 3: copy the remaining pages
        // 0x08 is the first write offset
        int writeOffset = FIRST_WRITE_OFFSET;
        offset = copyPages(list, writeOffset, rin, rout);
        int firstIFDOffset = list.get(0).getStartOffset();

        writeToStream(rout, firstIFDOffset);

        return pagesRetained;
    }
    protected static int copyHeader(RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
        rin.seek(STREAM_HEAD);
        // First 2 bytes determine the byte order of the file, "MM" or "II"
        short endian = rin.readShort();

        if (endian == IOUtils.BIG_ENDIAN) {
            rin.setReadStrategy(ReadStrategyMM.getInstance());
            rout.setWriteStrategy(WriteStrategyMM.getInstance());
        } else if(endian == IOUtils.LITTLE_ENDIAN) {
            rin.setReadStrategy(ReadStrategyII.getInstance());
            rout.setWriteStrategy(WriteStrategyII.getInstance());
        } else {
            rin.close();
            rout.close();
            throw new RuntimeException("Invalid TIFF byte order");
        }

        rout.writeShort(endian);
        // Read TIFF identifier
        rin.seek(0x02);
        short tiff_id = rin.readShort();

        if(tiff_id!=0x2a)//"*" 42 decimal
        {
            rin.close();
            rout.close();
            throw new RuntimeException("Invalid TIFF identifier");
        }

        rout.writeShort(tiff_id);
        rin.seek(OFFSET_TO_WRITE_FIRST_IFD_OFFSET);

        return rin.readInt();
    }

    // Copy a list of IFD and associated image data if any
    protected static int copyPages(List<IFD> list, int writeOffset, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
        // Write the first page data
        writeOffset = copyPageData(list.get(0), writeOffset, rin, rout);
        // Then write the first IFD
        writeOffset = list.get(0).write(rout, writeOffset);
        // We are going to write the remaining image pages and IFDs if any
        for(int i = 1; i < list.size(); i++) {
            writeOffset = copyPageData(list.get(i), writeOffset, rin, rout);
            // Tell the IFD to update next IFD offset for the following IFD
            list.get(i-1).setNextIFDOffset(rout, writeOffset);
            writeOffset = list.get(i).write(rout, writeOffset);
        }

        return writeOffset;
    }

    /**
     * @param offset offset to write page image data
     *
     * @return the position where to write the IFD for the current image page
     */
    private static int copyPageData(IFD ifd, int offset, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
        // Move stream pointer to the right place
        rout.seek(offset);

        // Original image data start from these offsets.
        ExifField<?> stripOffSets = ifd.removeField(ExifImageTag.STRIP_OFFSETS);

        if(stripOffSets == null)
            stripOffSets = ifd.removeField(ExifImageTag.TILE_OFFSETS);

        ExifField<?> stripByteCounts = ifd.getField(ExifImageTag.STRIP_BYTE_COUNTS);

        if(stripByteCounts == null)
            stripByteCounts = ifd.getField(ExifImageTag.TILE_BYTE_COUNTS);
		/*
		 * Make sure this will work in the case when neither STRIP_OFFSETS nor TILE_OFFSETS presents.
		 * Not sure if this will ever happen for TIFF. JPEG EXIF data do not contain these fields.
		 */
        if(stripOffSets != null) {
            int[] counts = stripByteCounts.getDataAsLong();
            int[] off = stripOffSets.getDataAsLong();
            int[] temp = new int[off.length];

            ExifField<?> exifField = ifd.getField(ExifImageTag.COMPRESSION);

            // Uncompressed image with one strip or tile (may contain wrong StripByteCounts value)
            // Bug fix for uncompressed image with one strip and wrong StripByteCounts value
            if((exifField == null ) || (exifField != null && exifField.getDataAsLong()[0] == 1)) { // Uncompressed data
                int planaryConfiguration = 1;

                exifField = ifd.getField(ExifImageTag.PLANAR_CONFIGURATTION);
                if(exifField != null) planaryConfiguration = exifField.getDataAsLong()[0];

                exifField = ifd.getField(ExifImageTag.SAMPLES_PER_PIXEL);

                int samplesPerPixel = 1;
                if(exifField != null) samplesPerPixel = exifField.getDataAsLong()[0];

                // If there is only one strip/samplesPerPixel strips for PlanaryConfiguration = 2
                if((planaryConfiguration == 1 && off.length == 1) || (planaryConfiguration == 2 && off.length == samplesPerPixel))
                {
                    int[] totalBytes2Read = getBytes2Read(ifd);

                    for(int i = 0; i < off.length; i++)
                        counts[i] = totalBytes2Read[i];
                }
            } // End of bug fix

            // We are going to write the image data first
            rout.seek(offset);

            // Copy image data from offset
            for(int i = 0; i < off.length; i++) {
                rin.seek(off[i]);
                byte[] buf = new byte[counts[i]];
                rin.readFully(buf);
                rout.write(buf);
                temp[i] = offset;
                offset += buf.length;
            }

            if(ifd.getField(ExifImageTag.STRIP_BYTE_COUNTS) != null)
                stripOffSets = new LongField(ExifImageTag.STRIP_OFFSETS, temp);
            else
                stripOffSets = new LongField(ExifImageTag.TILE_OFFSETS, temp);
            addField(ifd, stripOffSets);
        }

        // Add software field.
        String softWare = "PIXYMETA-ANDROID - https://github.com/dragon66/pixymeta-android\0";
        addField(ifd, new ASCIIField(ExifImageTag.SOFTWARE, softWare));

		/* The following are added to work with old-style JPEG compression (type 6) */
		/* One of the flavors (found in JPEG EXIF thumbnail IFD - IFD1) of the old JPEG compression contains this field */
        ExifField<?> jpegIFOffset = ifd.removeField(ExifImageTag.JPEG_INTERCHANGE_FORMAT);
        if(jpegIFOffset != null) {
            ExifField<?> jpegIFByteCount = ifd.removeField(ExifImageTag.JPEG_INTERCHANGE_FORMAT_LENGTH);
            try {
                if(jpegIFByteCount != null) {
                    rin.seek(jpegIFOffset.getDataAsLong()[0]);
                    byte[] bytes2Read = new byte[jpegIFByteCount.getDataAsLong()[0]];
                    rin.readFully(bytes2Read);
                    rout.seek(offset);
                    rout.write(bytes2Read);
                    addField(ifd, jpegIFByteCount);
                } else {
                    long startOffset = rout.getStreamPointer();
                    copyJPEGIFByteCount(rin, rout, jpegIFOffset.getDataAsLong()[0], offset);
                    long endOffset = rout.getStreamPointer();
                    addField(ifd, new LongField(ExifImageTag.JPEG_INTERCHANGE_FORMAT_LENGTH, new int[]{(int)(endOffset - startOffset)}));
                }
                jpegIFOffset = new LongField(ExifImageTag.JPEG_INTERCHANGE_FORMAT, new int[]{offset});
                addField(ifd, jpegIFOffset);
            } catch (EOFException ex) {;};
        }
		/* Another flavor of the old style JPEG compression type 6 contains separate tables */
        ExifField<?> jpegTable = ifd.removeField(ExifImageTag.JPEG_DC_TABLES);
        if(jpegTable != null) {
            try {
                addField(ifd, copyJPEGHufTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
            } catch(EOFException ex) {;}
        }

        jpegTable = ifd.removeField(ExifImageTag.JPEG_AC_TABLES);
        if(jpegTable != null) {
            try {
                addField(ifd, copyJPEGHufTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
            } catch(EOFException ex) {;}
        }

        jpegTable = ifd.removeField(ExifImageTag.JPEG_Q_TABLES);
        if(jpegTable != null) {
            try {
                addField(ifd, copyJPEGQTable(rin, rout, jpegTable, (int)rout.getStreamPointer()));
            } catch(EOFException ex) {;}
        }
		/* End of code to work with old-style JPEG compression */

        // Return the actual stream position (we may have lost track of it)
        return (int)rout.getStreamPointer();
    }

    private static ExifField<?> copyJPEGHufTable(RandomAccessInputStream rin, RandomAccessOutputStream rout, ExifField<?> field, int curPos) throws IOException
    {
        int[] data = field.getDataAsLong();
        int[] tmp = new int[data.length];

        for(int i = 0; i < data.length; i++) {
            rin.seek(data[i]);
            tmp[i] = curPos;
            byte[] htable = new byte[16];
            IOUtils.readFully(rin, htable);
            IOUtils.write(rout, htable);
            curPos += 16;

            int numCodes = 0;

            for(int j = 0; j < 16; j++) {
                numCodes += htable[j]&0xff;
            }

            curPos += numCodes;

            htable = new byte[numCodes];
            IOUtils.readFully(rin, htable);
            IOUtils.write(rout, htable);
        }

        if(ExifImageTag.fromShort(field.getTag()) == ExifImageTag.JPEG_AC_TABLES)
            return new LongField(ExifImageTag.JPEG_AC_TABLES, tmp);

        return new LongField(ExifImageTag.JPEG_DC_TABLES, tmp);
    }

    private static void copyJPEGIFByteCount(RandomAccessInputStream rin, RandomAccessOutputStream rout, int offset, int outOffset) throws IOException {
        boolean finished = false;
        int length = 0;
        short marker;
        JpegSegmentMarker emarker;

        rin.seek(offset);
        rout.seek(outOffset);
        // The very first marker should be the start_of_image marker!
        if(JpegSegmentMarker.fromShort(IOUtils.readShortMM(rin)) != JpegSegmentMarker.JPG_SEGMENT_START_OF_IMAGE_SOI) {
            return;
        }

        IOUtils.writeShortMM(rout, JpegSegmentMarker.JPG_SEGMENT_START_OF_IMAGE_SOI.getValue());

        marker = IOUtils.readShortMM(rin);

        while (!finished) {
            if (JpegSegmentMarker.fromShort(marker) == JpegSegmentMarker.JPG_SEGMENT_END_OF_IMAGE_EOI) {
                IOUtils.writeShortMM(rout, marker);
                finished = true;
            } else { // Read markers
                emarker = JpegSegmentMarker.fromShort(marker);

                switch (emarker) {
                    case JPG: // JPG and JPGn shouldn't appear in the image.
                    case JPG0:
                    case JPG13:
                    case TEM: // The only stand alone mark besides JPG_SEGMENT_START_OF_IMAGE_SOI, JPG_SEGMENT_END_OF_IMAGE_EOI, and RSTn.
                        marker = IOUtils.readShortMM(rin);
                        break;
                    case SOS:
                        marker = copyJPEGSOS(rin, rout);
                        break;
                    case JPG_SEGMENT_PADDING:
                        int nextByte = 0;
                        while((nextByte = rin.read()) == 0xff) {;}
                        marker = (short)((0xff<<8)|nextByte);
                        break;
                    default:
                        length = IOUtils.readUnsignedShortMM(rin);
                        byte[] buf = new byte[length - 2];
                        rin.read(buf);
                        IOUtils.writeShortMM(rout, marker);
                        IOUtils.writeShortMM(rout, length);
                        rout.write(buf);
                        marker = IOUtils.readShortMM(rin);
                }
            }
        }
    }

    private static ExifField<?> copyJPEGQTable(RandomAccessInputStream rin, RandomAccessOutputStream rout, ExifField<?> field, int curPos) throws IOException
    {
        byte[] qtable = new byte[64];
        int[] data = field.getDataAsLong();
        int[] tmp = new int[data.length];

        for(int i = 0; i < data.length; i++) {
            rin.seek(data[i]);
            tmp[i] = curPos;
            IOUtils.readFully(rin, qtable);
            IOUtils.write(rout, qtable);
            curPos += 64;
        }

        return new LongField(ExifImageTag.JPEG_Q_TABLES, tmp);
    }

    private static short copyJPEGSOS(RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException	{
        int len = IOUtils.readUnsignedShortMM(rin);
        byte buf[] = new byte[len - 2];
        IOUtils.readFully(rin, buf);
        IOUtils.writeShortMM(rout, JpegSegmentMarker.SOS.getValue());
        IOUtils.writeShortMM(rout, len);
        rout.write(buf);
        // Actual image data follow.
        int nextByte = 0;
        short marker = 0;

        while((nextByte = IOUtils.read(rin)) != -1)	{
            rout.write(nextByte);

            if(nextByte == 0xff)
            {
                nextByte = IOUtils.read(rin);
                rout.write(nextByte);

                if (nextByte == -1) {
                    throw new IOException("Premature end of SOS segment!");
                }

                if (nextByte != 0x00) {
                    marker = (short)((0xff<<8)|nextByte);

                    switch (JpegSegmentMarker.fromShort(marker)) {
                        case RST0:
                        case RST1:
                        case RST2:
                        case RST3:
                        case RST4:
                        case RST5:
                        case RST6:
                        case RST7:
                            continue;
                        default:
                    }
                    break;
                }
            }
        }

        if (nextByte == -1) {
            throw new IOException("Premature end of SOS segment!");
        }

        return marker;
    }

    protected static void writeToStream(RandomAccessOutputStream rout, int firstIFDOffset) throws IOException {
        // Go to the place where we should write the first IFD offset
        // and write the first IFD offset
        rout.seek(OFFSET_TO_WRITE_FIRST_IFD_OFFSET);
        rout.writeInt(firstIFDOffset);
        // Dump the data to the real output stream
        rout.seek(STREAM_HEAD);
        rout.writeToStream(rout.getLength());
        //rout.flush();
    }
    // Used to calculate how many bytes to read in case we have only one strip or tile
    private static int[] getBytes2Read(IFD ifd) {
        // Let's calculate how many bytes we are supposed to read
        ExifField<?> exifField = ifd.getField(ExifImageTag.IMAGE_WIDTH);
        int imageWidth = exifField.getDataAsLong()[0];
        exifField = ifd.getField(ExifImageTag.IMAGE_LENGTH);
        int imageHeight = exifField.getDataAsLong()[0];

        // For YCbCr image only
        int horizontalSampleFactor = 2; // Default 2X2
        int verticalSampleFactor = 2; // Not 1X1

        int photoMetric = ifd.getField(ExifImageTag.PHOTOMETRIC_INTERPRETATION).getDataAsLong()[0];

        // Correction for imageWidth and imageHeight for YCbCr image
        if(photoMetric == ExifFieldEnum.PhotoMetric.YCbCr.getValue()) {
            ExifField<?> f_YCbCrSubSampling = ifd.getField(ExifImageTag.YCbCr_SUB_SAMPLING);

            if(f_YCbCrSubSampling != null) {
                int[] sampleFactors = f_YCbCrSubSampling.getDataAsLong();
                horizontalSampleFactor = sampleFactors[0];
                verticalSampleFactor = sampleFactors[1];
            }
            imageWidth = ((imageWidth + horizontalSampleFactor - 1)/horizontalSampleFactor)*horizontalSampleFactor;
            imageHeight = ((imageHeight + verticalSampleFactor - 1)/verticalSampleFactor)*verticalSampleFactor;
        }

        int samplesPerPixel = 1;

        exifField = ifd.getField(ExifImageTag.SAMPLES_PER_PIXEL);
        if(exifField != null) {
            samplesPerPixel = exifField.getDataAsLong()[0];
        }

        int bitsPerSample = 1;

        exifField = ifd.getField(ExifImageTag.BITS_PER_SAMPLE);
        if(exifField != null) {
            bitsPerSample = exifField.getDataAsLong()[0];
        }

        int tileWidth = -1;
        int tileLength = -1;

        ExifField<?> f_tileLength = ifd.getField(ExifImageTag.TILE_LENGTH);
        ExifField<?> f_tileWidth = ifd.getField(ExifImageTag.TILE_WIDTH);

        if(f_tileWidth != null) {
            tileWidth = f_tileWidth.getDataAsLong()[0];
            tileLength = f_tileLength.getDataAsLong()[0];
        }

        int rowsPerStrip = imageHeight;
        int rowWidth = imageWidth;

        ExifField<?> f_rowsPerStrip = ifd.getField(ExifImageTag.ROWS_PER_STRIP);
        if(f_rowsPerStrip != null) rowsPerStrip = f_rowsPerStrip.getDataAsLong()[0];

        if(rowsPerStrip > imageHeight) rowsPerStrip = imageHeight;

        if(tileWidth > 0) {
            rowsPerStrip = tileLength;
            rowWidth = tileWidth;
        }

        int planaryConfiguration = 1;

        exifField = ifd.getField(ExifImageTag.PLANAR_CONFIGURATTION);
        if(exifField != null) planaryConfiguration = exifField.getDataAsLong()[0];

        int[] totalBytes2Read = new int[samplesPerPixel];

        if(planaryConfiguration == 1)
            totalBytes2Read[0] = ((rowWidth*bitsPerSample*samplesPerPixel + 7)/8)*rowsPerStrip;
        else
            totalBytes2Read[0] = totalBytes2Read[1] = totalBytes2Read[2] = ((rowWidth*bitsPerSample + 7)/8)*rowsPerStrip;

        if(photoMetric == ExifFieldEnum.PhotoMetric.YCbCr.getValue()) {
            if(samplesPerPixel != 3) samplesPerPixel = 3;

            int[] sampleBytesPerRow = new int[samplesPerPixel];
            sampleBytesPerRow[0] = (bitsPerSample*rowWidth + 7)/8;
            sampleBytesPerRow[1] = (bitsPerSample*rowWidth/horizontalSampleFactor + 7)/8;
            sampleBytesPerRow[2] = sampleBytesPerRow[1];

            int[] sampleRowsPerStrip = new int[samplesPerPixel];
            sampleRowsPerStrip[0] = rowsPerStrip;
            sampleRowsPerStrip[1] = rowsPerStrip/verticalSampleFactor;
            sampleRowsPerStrip[2]= sampleRowsPerStrip[1];

            totalBytes2Read[0] = sampleBytesPerRow[0]*sampleRowsPerStrip[0];
            totalBytes2Read[1] = sampleBytesPerRow[1]*sampleRowsPerStrip[1];
            totalBytes2Read[2] = totalBytes2Read[1];

            if(exifField != null) planaryConfiguration = exifField.getDataAsLong()[0];

            if(planaryConfiguration == 1)
                totalBytes2Read[0] = totalBytes2Read[0] + totalBytes2Read[1] + totalBytes2Read[2];
        }

        return totalBytes2Read;
    }


}
