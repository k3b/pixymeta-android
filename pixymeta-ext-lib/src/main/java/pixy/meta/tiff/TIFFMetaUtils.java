/**
 * Copyright (c) 2015 by Wen Yu.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Any modifications to this file must keep this entire header intact.
 *
 * Change History - most recent changes go on top of previous changes
 *
 * TIFFMetaUtils.java
 *
 * Who   Date       Description
 * ====  =========  ==========================================================
 * WY    06Jul2015  Added insertXMP(InputSream, OutputStream, XMP)
 * WY    15Apr2015  Changed the argument type for insertIPTC() and insertIRB()
 * WY    07Apr2015  Removed insertICCProfile() AWT related code
 * WY    07Apr2015  Merge Adobe AdobeIRBSegment IPTC and TIFF IPTC data if both exist
 * WY    13Mar2015  Initial creation
 */

package pixy.meta.tiff;

import pixy.image.IBitmap;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import pixy.api.IMetadata;
import pixy.meta.MetadataType;
import pixy.meta.adobe.AdobeIRBSegment;
import pixy.meta.adobe.AdobyMetadataBase;
import pixy.meta.adobe.DDB;
import pixy.meta.adobe.IRBThumbnail;
import pixy.meta.adobe.ThumbnailResource;
import pixy.meta.adobe.ImageResourceID;
import pixy.meta.exif.ExifMetaSegment;
import pixy.meta.exif.IfdMetaUtils;
import pixy.meta.exif.TiffExif;
import pixy.meta.icc.ICCProfile;
import pixy.meta.image.Comments;
import pixy.meta.iptc.IPTC;
import pixy.meta.iptc.IPTCDataSet;
import pixy.meta.xmp.XMP;
import pixy.image.exifFields.ASCIIField;
import pixy.image.exifFields.ByteField;
import pixy.image.exifFields.FieldType;
import pixy.image.exifFields.IFD;
import pixy.image.exifFields.LongField;
import pixy.image.exifFields.TiffField;
import pixy.image.exifFields.TiffTag;
import pixy.image.exifFields.UndefinedField;
import pixy.image.exifFields.TIFFImage;
import pixy.io.IOUtils;
import pixy.io.RandomAccessInputStream;
import pixy.io.RandomAccessOutputStream;
import pixy.io.ReadStrategy;
import pixy.io.ReadStrategyII;
import pixy.io.ReadStrategyMM;
import pixy.io.WriteStrategyII;
import pixy.io.WriteStrategyMM;
import pixy.string.XMLUtils;
import pixy.util.ArrayUtils;

public class TIFFMetaUtils extends IfdMetaUtils {

	// Obtain a logger instance
	private static final Logger LOGGER = LoggerFactory.getLogger(TIFFMetaUtils.class);
	
	private static Collection<IPTCDataSet> copyIPTCDataSet(Collection<IPTCDataSet> iptcs, byte[] data) throws IOException {
		IPTC iptc = new IPTC(data);
		// Shallow copy the map
		Map<String, List<IPTCDataSet>> dataSetMap = new HashMap<String, List<IPTCDataSet>>(iptc.getDataSets());
		for(IPTCDataSet set : iptcs)
			if(!set.allowMultiple())
				dataSetMap.remove(set.getName());
		for(List<IPTCDataSet> iptcList : dataSetMap.values())
			iptcs.addAll(iptcList);
		
		return iptcs;
	}
	
	/**
	 * Extracts ICC_Profile from certain page of TIFF if any
	 * 
	 * @param pageNumber page number from which to extract ICC_Profile
	 * @param rin RandomAccessInputStream for the input TIFF
	 * @return a byte array for the extracted ICC_Profile or null if none exists
	 * @throws Exception
	 */
	public static byte[] extractICCProfile(int pageNumber, RandomAccessInputStream rin) throws Exception {
		// Read pass image header
		int offset = IfdMetaUtils.readHeader(rin);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		IfdMetaUtils.readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		TiffField<?> f_iccProfile = workingPage.getField(TiffTag.ICC_PROFILE);
		if(f_iccProfile != null) {
			return (byte[])f_iccProfile.getData();
		}
		
		return null;
	}
	
	public static byte[] extractICCProfile(RandomAccessInputStream rin) throws Exception {
		return extractICCProfile(0, rin);
	}
	
	public static IRBThumbnail extractThumbnail(int pageNumber, RandomAccessInputStream rin) throws IOException {
		// Read pass image header
		int offset = IfdMetaUtils.readHeader(rin);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		IfdMetaUtils.readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		TiffField<?> f_photoshop = workingPage.getField(TiffTag.PHOTOSHOP);
		if(f_photoshop != null) {
			byte[] data = (byte[])f_photoshop.getData();
			AdobeIRBSegment adobeIrbSegment = new AdobeIRBSegment(data);
			if(adobeIrbSegment.containsThumbnail()) {
				IRBThumbnail thumbnail = adobeIrbSegment.getThumbnail();
				return thumbnail;					
			}		
		}
		
		return null;
	}
	
	public static IRBThumbnail extractThumbnail(RandomAccessInputStream rin) throws IOException {
		return extractThumbnail(0, rin);
	}
	
	public static void extractThumbnail(RandomAccessInputStream rin, String pathToThumbnail) throws IOException {
		IRBThumbnail thumbnail = extractThumbnail(rin);				
		if(thumbnail != null) {
			String outpath = "";
			if(pathToThumbnail.endsWith("\\") || pathToThumbnail.endsWith("/"))
				outpath = pathToThumbnail + "photoshop_thumbnail.jpg";
			else
				outpath = pathToThumbnail.replaceFirst("[.][^.]+$", "") + "_photoshop_t.jpg";
			FileOutputStream fout = new FileOutputStream(outpath);
			if(thumbnail.getDataType() == IRBThumbnail.DATA_TYPE_KJpegRGB) {
				fout.write(thumbnail.getCompressedImage());
			} else {
				IBitmap bm = thumbnail.getRawImage();
				try {
					bm.compressJPG(100, fout);
				} catch (Exception e) {
					throw new IOException("Writing thumbnail failed!");
				}
			}
			fout.close();	
		}			
	}
	
	public static void insertComments(List<String> comments, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertComments(comments, 0, rin, rout);
	}
		
	public static void insertComments(List<String> comments, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);

		StringBuilder commentsBuilder = new StringBuilder();
		
		// ASCII field allows for multiple strings
		for(String comment : comments) {
			commentsBuilder.append(comment);
			commentsBuilder.append('\0');
		}
		
		workingPage.addField(new ASCIIField(TiffTag.IMAGE_DESCRIPTION, commentsBuilder.toString()));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	public static void insertExif(RandomAccessInputStream rin, RandomAccessOutputStream rout, ExifMetaSegment exif, boolean update) throws IOException {
		insertExif(rin, rout, exif, 0, update);
	}
	
	/**
	 * Insert EXIF data with optional thumbnail IFD
	 * 
	 * @param rin input image stream
	 * @param rout output image stream
	 * @param exif EXIF wrapper instance
	 * @param pageNumber page offset where to insert EXIF (zero based)
	 * @param update True to keep the original data, otherwise false
	 * @throws Exception
	 */
	public static void insertExif(RandomAccessInputStream rin, RandomAccessOutputStream rout, ExifMetaSegment exif, int pageNumber, boolean update) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		IfdMetaUtils.readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD imageIFD = ifds.get(pageNumber);
		IFD exifSubIFD = imageIFD.getChild(TiffTag.EXIF_SUB_IFD);
		IFD gpsSubIFD = imageIFD.getChild(TiffTag.GPS_SUB_IFD);
		IFD newImageIFD = exif.getImageIFD();
		IFD newExifSubIFD = exif.getExifIFD();
		IFD newGpsSubIFD = exif.getGPSIFD();
		
		if(newImageIFD != null) { // Copy the Image IFD fields - this is dangerous.
			imageIFD.addFields(newImageIFD.getFields());
		}
		
		if(update && exifSubIFD != null && newExifSubIFD != null) {
			exifSubIFD.addFields(newExifSubIFD.getFields());
			newExifSubIFD = exifSubIFD;
		}
		
		if(newExifSubIFD != null) {
			imageIFD.addField(new LongField(TiffTag.EXIF_SUB_IFD, new int[]{0})); // Place holder
			imageIFD.addChild(TiffTag.EXIF_SUB_IFD, newExifSubIFD);		
		}
		
		if(update && gpsSubIFD != null && newGpsSubIFD != null) {
			gpsSubIFD.addFields(newGpsSubIFD.getFields());
			newGpsSubIFD = gpsSubIFD;
		}
		
		if(newGpsSubIFD != null) {
			imageIFD.addField(new LongField(TiffTag.GPS_SUB_IFD, new int[]{0})); // Place holder
			imageIFD.addChild(TiffTag.GPS_SUB_IFD, newGpsSubIFD);		
		}
		
		int writeOffset = FIRST_WRITE_OFFSET;
		// Copy pages
		writeOffset = copyPages(ifds, writeOffset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();

		writeToStream(rout, firstIFDOffset);
	}
	
	public static void insertICCProfile(byte[] icc_profile, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertICCProfile(icc_profile, 0, rin, rout);
	}
	
	/**
	 * Insert ICC_Profile into TIFF page
	 * 
	 * @param icc_profile byte array holding the ICC_Profile
	 * @param pageNumber page offset where to insert ICC_Profile
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @throws Exception
	 */
	public static void insertICCProfile(byte[] icc_profile, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		IfdMetaUtils.readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		workingPage.addField(new UndefinedField(TiffTag.ICC_PROFILE, icc_profile));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	public static void insertIPTC(RandomAccessInputStream rin, RandomAccessOutputStream rout, Collection<IPTCDataSet> iptcs, boolean update) throws IOException {
		insertIPTC(rin, rout, 0, iptcs, update);
	}
	
	/**
	 * Insert IPTC data into TIFF image. If the original TIFF image contains IPTC data, we either keep
	 * or override them depending on the input parameter "update."
	 * <p>
	 * There is a possibility that IPTC data presents in more than one places such as a normal TIFF
	 * tag, or buried inside a Photoshop IPTC-NAA Image Resource Block (AdobeIRBSegment), or even in a XMP block.
	 * Currently this method does the following thing: if no IPTC data was found from both Photoshop or 
	 * normal IPTC tag, we insert the IPTC data with a normal IPTC tag. If IPTC data is found both as
	 * a Photoshop tag and a normal IPTC tag, depending on the "update" parameter, we will either delete
	 * the IPTC data from both places and insert the new IPTC data into the Photoshop tag or we will
	 * synchronize the two sets of IPTC data, delete the original IPTC from both places and insert the
	 * synchronized IPTC data along with the new IPTC data into the Photoshop tag. In both cases, we
	 * will keep the other IRBs from the original Photoshop tag unchanged. 
	 * 
	 * @param rin RandomAccessInputStream for the original TIFF
	 * @param rout RandomAccessOutputStream for the output TIFF with IPTC inserted
	 * @param pageNumber page offset where to insert IPTC
	 * @param iptcs A list of IPTCDataSet to insert into the TIFF image
	 * @param update whether we want to keep the original IPTC data or override it
	 *        completely new IPTC data set
	 * @throws IOException
	 */
	public static void insertIPTC(RandomAccessInputStream rin, RandomAccessOutputStream rout, int pageNumber, Collection<IPTCDataSet> iptcs, boolean update) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		IfdMetaUtils.readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
	
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		// See if we also have regular IPTC tag field
		TiffField<?> f_iptc = workingPage.removeField(TiffTag.IPTC);		
		TiffField<?> f_photoshop = workingPage.getField(TiffTag.PHOTOSHOP);
		if(f_photoshop != null) { // Read 8BIMs
			AdobeIRBSegment adobeIrbSegment = new AdobeIRBSegment((byte[])f_photoshop.getData());
			// Shallow copy the map.
			Map<Short, AdobyMetadataBase> bims = new HashMap<Short, AdobyMetadataBase>(adobeIrbSegment.get8BIM());
			AdobyMetadataBase photoshop_iptc = bims.remove(ImageResourceID.IPTC_NAA.getValue());
			if(photoshop_iptc != null) { // If we have IPTC
				if(update) { // If we need to keep the old data, copy it
					if(f_iptc != null) {// We are going to synchronize the two IPTC data
						byte[] data = null;
						if(f_iptc.getType() == FieldType.LONG)
							data = ArrayUtils.toByteArray(f_iptc.getDataAsLong(), rin.getEndian() == IOUtils.BIG_ENDIAN);
						else
							data = (byte[])f_iptc.getData();
						copyIPTCDataSet(iptcs, data);
					}
					// Now copy the Photoshop IPTC data
					copyIPTCDataSet(iptcs, photoshop_iptc.getData());
					// Remove duplicates
					iptcs = new ArrayList<IPTCDataSet>(new HashSet<IPTCDataSet>(iptcs));
				}
			}
			// Create IPTC 8BIM
			for(IPTCDataSet dataset : iptcs) {
				dataset.write(bout);
			}
			AdobyMetadataBase iptc_bim = new AdobyMetadataBase(ImageResourceID.IPTC_NAA, "iptc", bout.toByteArray());
			bout.reset();
			iptc_bim.write(bout); // Write the IPTC 8BIM first
			for(AdobyMetadataBase bim : bims.values()) // Copy the other 8BIMs if any
				bim.write(bout);
			// Add a new Photoshop tag field to TIFF
			workingPage.addField(new UndefinedField(TiffTag.PHOTOSHOP, bout.toByteArray()));
		} else { // We don't have photoshop, add IPTC to regular IPTC tag field
			if(f_iptc != null && update) {
				byte[] data = null;
				if(f_iptc.getType() == FieldType.LONG)
					data = ArrayUtils.toByteArray(f_iptc.getDataAsLong(), rin.getEndian() == IOUtils.BIG_ENDIAN);
				else
					data = (byte[])f_iptc.getData();
				copyIPTCDataSet(iptcs, data);
			}
			for(IPTCDataSet dataset : iptcs) {
				dataset.write(bout);
			}		
			workingPage.addField(new UndefinedField(TiffTag.IPTC, bout.toByteArray()));
		}
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	public static void insertIRB(RandomAccessInputStream rin, RandomAccessOutputStream rout, Collection<AdobyMetadataBase> bims, boolean update) throws IOException {
		insertIRB(rin, rout, 0, bims, update);
	}
	
	public static void insertIRB(RandomAccessInputStream rin, RandomAccessOutputStream rout, int pageNumber, Collection<AdobyMetadataBase> bims, boolean update) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		IfdMetaUtils.readIFDs(null, null, TiffTag.class, ifds, offset, rin);
	
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		
		if(update) {
			TiffField<?> f_irb = workingPage.getField(TiffTag.PHOTOSHOP);
			if(f_irb != null) {
				AdobeIRBSegment adobeIrbSegment = new AdobeIRBSegment((byte[])f_irb.getData());
				// Shallow copy the map.
	    		Map<Short, AdobyMetadataBase> bimMap = new HashMap<Short, AdobyMetadataBase>(adobeIrbSegment.get8BIM());
				for(AdobyMetadataBase bim : bims) // Replace the original data
					bimMap.put(bim.getID(), bim);
				// In case we have two ThumbnailResource AdobeIRBSegment, remove the Photoshop4.0 one
				if(bimMap.containsKey(ImageResourceID.THUMBNAIL_RESOURCE_PS4.getValue()) 
						&& bimMap.containsKey(ImageResourceID.THUMBNAIL_RESOURCE_PS5.getValue()))
					bimMap.remove(ImageResourceID.THUMBNAIL_RESOURCE_PS4.getValue());
				bims = bimMap.values();
			}
		}
		
		for(AdobyMetadataBase bim : bims)
			bim.write(bout);
		
		workingPage.addField(new UndefinedField(TiffTag.PHOTOSHOP, bout.toByteArray()));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	/**
	 * Insert a thumbnail into PHOTOSHOP private tag field
	 *  
	 * @param rin RandomAccessInputStream for the input TIFF
	 * @param rout RandomAccessOutputStream for the output TIFF
	 * @param thumbnail a IBitmap to be inserted
	 * @throws Exception
	 */
	public static void insertThumbnail(RandomAccessInputStream rin, RandomAccessOutputStream rout, IBitmap thumbnail) throws IOException {
		// Sanity check
		if(thumbnail == null) throw new IllegalArgumentException("Input thumbnail is null");
		AdobyMetadataBase bim = new ThumbnailResource(thumbnail);
		insertIRB(rin, rout, Arrays.asList(bim), true);
	}
	
	public static void insertXMP(XMP xmp, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertXMP(xmp.getData(), rin, rout);
	}
	
	public static void insertXMP(XMP xmp, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertXMP(xmp.getData(), pageNumber, rin, rout);
	}
	
	public static void insertXMP(byte[] xmp, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		insertXMP(xmp, 0, rin, rout);
	}
	
	/**
	 * Insert XMP data into TIFF image
	 * @param xmp byte array for the XMP data to be inserted
	 * @param pageNumber page offset where to insert XMP
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @throws IOException
	 */
	public static void insertXMP(byte[] xmp, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		IfdMetaUtils.readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		workingPage.addField(new UndefinedField(TiffTag.XMP, xmp));
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);	
	}
	
	public static void insertXMP(String xmp, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		Document doc = XMLUtils.createXML(xmp);
		XMLUtils.insertLeadingPI(doc, "xpacket", "begin='' id='W5M0MpCehiHzreSzNTczkc9d'");
		XMLUtils.insertTrailingPI(doc, "xpacket", "end='w'");
		byte[] xmpBytes = XMLUtils.serializeToByteArray(doc);
		insertXMP(xmpBytes, rin, rout);
	}
	
	public static Map<MetadataType, IMetadata> readMetadata(RandomAccessInputStream rin) throws IOException {
		return readMetadata(rin, 0);
	}
	
	public static Map<MetadataType, IMetadata> readMetadata(RandomAccessInputStream rin, int pageNumber) throws IOException	{
		Map<MetadataType, IMetadata> metadataMap = new HashMap<MetadataType, IMetadata>();

		int offset = IfdMetaUtils.readHeader(rin);
		List<IFD> ifds = new ArrayList<IFD>();
		IfdMetaUtils.readIFDs(null, null, TiffTag.class, ifds, offset, rin);
		
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD currIFD = ifds.get(pageNumber);
		TiffField<?> field = currIFD.getField(TiffTag.ICC_PROFILE); 
		if(field != null) { // We have found ICC_Profile
			metadataMap.put(MetadataType.ICC_PROFILE, new ICCProfile((byte[])field.getData()));
		}
		field = currIFD.getField(TiffTag.XMP);
		if(field != null) { // We have found XMP
			metadataMap.put(MetadataType.XMP, new TiffXMP((byte[])field.getData()));
		}
		field = currIFD.getField(TiffTag.PHOTOSHOP);
		if(field != null) { // We have found Photoshop AdobeIRBSegment
			AdobeIRBSegment adobeIrbSegment = new AdobeIRBSegment((byte[])field.getData());
			metadataMap.put(MetadataType.PHOTOSHOP_IRB, adobeIrbSegment);
			AdobyMetadataBase photoshop_AdobyMetadataBase = adobeIrbSegment.get8BIM(ImageResourceID.IPTC_NAA.getValue());
			if(photoshop_AdobyMetadataBase != null) { // If we have IPTC data inside Photoshop, keep it
				IPTC iptc = new IPTC(photoshop_AdobyMetadataBase.getData());
				metadataMap.put(MetadataType.IPTC, iptc);
			}
		}
		field = currIFD.getField(TiffTag.IPTC);
		if(field != null) { // We have found IPTC data
			IPTC iptc = (IPTC)(metadataMap.get(MetadataType.IPTC));
			byte[] iptcData = null;
			FieldType type = field.getType();
			if(type == FieldType.LONG)
				iptcData = ArrayUtils.toByteArray(field.getDataAsLong(), rin.getEndian() == IOUtils.BIG_ENDIAN);
			else
				iptcData = (byte[])field.getData();
			if(iptc != null) // If we have IPTC data from AdobeIRBSegment, consolidate it with the current data
				iptcData = ArrayUtils.concat(iptcData, iptc.getData());
			metadataMap.put(MetadataType.IPTC, new IPTC(iptcData));
		}		
		field = currIFD.getField(TiffTag.EXIF_SUB_IFD);
		if(field != null) { // We have found EXIF SubIFD
			metadataMap.put(MetadataType.EXIF, new TiffExif(currIFD));
		}
		field = currIFD.getField(TiffTag.IMAGE_SOURCE_DATA);
		if(field != null) {
			boolean bigEndian = (rin.getEndian() == IOUtils.BIG_ENDIAN);
			ReadStrategy readStrategy = bigEndian?ReadStrategyMM.getInstance():ReadStrategyII.getInstance();
			metadataMap.put(MetadataType.PHOTOSHOP_DDB, new DDB((byte[])field.getData(), readStrategy));
		}
		field = currIFD.getField(TiffTag.IMAGE_DESCRIPTION);
		if(field != null) { // We have Comment
			Comments comments = new pixy.meta.image.Comments();
			comments.addComment(field.getDataAsString());
			metadataMap.put(MetadataType.COMMENT, comments);
		}
		
		return metadataMap;
	}
	
	public static void removeMetadata(int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout, MetadataType ... metadataTypes) throws IOException {
		removeMetadata(new HashSet<MetadataType>(Arrays.asList(metadataTypes)), pageNumber, rin, rout);
	}
	
	public static void removeMetadata(RandomAccessInputStream rin, RandomAccessOutputStream rout, MetadataType ... metadataTypes) throws IOException {
		removeMetadata(0, rin, rout, metadataTypes);
	}
	
	/**
	 * Remove meta data from TIFF image
	 * 
	 * @param pageNumber working page from which to remove EXIF and GPS data
	 * @param rin RandomAccessInputStream for the input image
	 * @param rout RandomAccessOutputStream for the output image
	 * @throws IOException
	 */
	public static void removeMetadata(Set<MetadataType> metadataTypes, int pageNumber, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		int offset = copyHeader(rin, rout);
		// Read the IFDs into a list first
		List<IFD> ifds = new ArrayList<IFD>();
		IfdMetaUtils.readIFDs(null, null, TiffTag.class, ifds, offset, rin);
	
		if(pageNumber < 0 || pageNumber >= ifds.size())
			throw new IllegalArgumentException("pageNumber " + pageNumber + " out of bounds: 0 - " + (ifds.size() - 1));
		
		IFD workingPage = ifds.get(pageNumber);
		
		TiffField<?> metadata = null;
		
		for(MetadataType metaType : metadataTypes) {
			switch(metaType) {
				case XMP:
					workingPage.removeField(TiffTag.XMP);
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove XMP and keep the other AdobeIRBSegment data untouched.
						removeMetadataFromIRB(workingPage, data, ImageResourceID.XMP_METADATA);
					}
					break;
				case IPTC:
					workingPage.removeField(TiffTag.IPTC);
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove IPTC_NAA and keep the other AdobeIRBSegment data untouched.
						removeMetadataFromIRB(workingPage, data, ImageResourceID.IPTC_NAA);
					}
					break;
				case ICC_PROFILE:
					workingPage.removeField(TiffTag.ICC_PROFILE);
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove ICC_PROFILE and keep the other AdobeIRBSegment data untouched.
						removeMetadataFromIRB(workingPage, data, ImageResourceID.ICC_PROFILE);
					}
					break;
				case PHOTOSHOP_IRB:
					workingPage.removeField(TiffTag.PHOTOSHOP);
					break;
				case EXIF:
					workingPage.removeField(TiffTag.EXIF_SUB_IFD);
					workingPage.removeField(TiffTag.GPS_SUB_IFD);
					metadata = workingPage.removeField(TiffTag.PHOTOSHOP);
					if(metadata != null) {
						byte[] data = (byte[])metadata.getData();
						// We only remove EXIF and keep the other AdobeIRBSegment data untouched.
						removeMetadataFromIRB(workingPage, data, ImageResourceID.EXIF_DATA1, ImageResourceID.EXIF_DATA3);
					}
					break;
				default:
			}
		}
		
		offset = copyPages(ifds, offset, rin, rout);
		int firstIFDOffset = ifds.get(0).getStartOffset();	

		writeToStream(rout, firstIFDOffset);		
	}
	
	public static void removeMetadata(Set<MetadataType> metadataTypes, RandomAccessInputStream rin, RandomAccessOutputStream rout) throws IOException {
		removeMetadata(metadataTypes, 0, rin, rout);
	}
	
	private static void removeMetadataFromIRB(IFD workingPage, byte[] data, ImageResourceID ... ids) throws IOException {
		AdobeIRBSegment adobeIrbSegment = new AdobeIRBSegment(data);
		// Shallow copy the map.
		Map<Short, AdobyMetadataBase> bimMap = new HashMap<Short, AdobyMetadataBase>(adobeIrbSegment.get8BIM());
		// We only remove XMP and keep the other AdobeIRBSegment data untouched.
		for(ImageResourceID id : ids)
			bimMap.remove(id.getValue());
		if(bimMap.size() > 0) {
		   	// Write back the AdobeIRBSegment
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			for(AdobyMetadataBase bim : bimMap.values())
				bim.write(bout);
			// Add new PHOTOSHOP field
			workingPage.addField(new ByteField(TiffTag.PHOTOSHOP, bout.toByteArray()));
		}		
	}
	
	public static void write(TIFFImage tiffImage, RandomAccessOutputStream rout) throws IOException {
		RandomAccessInputStream rin = tiffImage.getInputStream();
		int offset = writeHeader(IOUtils.BIG_ENDIAN, rout);
		offset = copyPages(tiffImage.getIFDs(), offset, rin, rout);
		int firstIFDOffset = tiffImage.getIFDs().get(0).getStartOffset();	
	 
		writeToStream(rout, firstIFDOffset);
	}
	
	// Return stream offset where to write actual image data or IFD	
	private static int writeHeader(short endian, RandomAccessOutputStream rout) throws IOException {
		// Write byte order
		rout.writeShort(endian);
		// Set write strategy based on byte order
		if (endian == IOUtils.BIG_ENDIAN)
		    rout.setWriteStrategy(WriteStrategyMM.getInstance());
		else if(endian == IOUtils.LITTLE_ENDIAN)
		    rout.setWriteStrategy(WriteStrategyII.getInstance());
		else {
			throw new RuntimeException("Invalid TIFF byte order");
	    }		
		// Write TIFF identifier
		rout.writeShort(0x2a);
		
		return FIRST_WRITE_OFFSET;
	}
		
}