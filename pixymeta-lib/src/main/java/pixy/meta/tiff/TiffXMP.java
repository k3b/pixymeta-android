package pixy.meta.tiff;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import pixy.meta.IMetadataDirectory;
import pixy.meta.IMetadataTag;
import pixy.meta.MetadataDirectoryImpl;
import pixy.meta.xmp.XMP;

public class TiffXMP extends XMP {

	private static final String MODUL_NAME = "Tiff-XMP";

	public TiffXMP(byte[] data) {
		super(MODUL_NAME, data);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void write(OutputStream os) throws IOException {
		// TODO Auto-generated method stub
	}
}