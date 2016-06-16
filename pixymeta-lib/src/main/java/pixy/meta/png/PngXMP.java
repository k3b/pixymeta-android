package pixy.meta.png;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import pixy.meta.IMetadataDirectory;
import pixy.meta.IMetadataTag;
import pixy.meta.MetadataDirectoryImpl;
import pixy.meta.xmp.XMP;

public class PngXMP extends XMP {

	private static final String MODUL_NAME = "Png-XMP";

	public PngXMP(String string) {
		super(MODUL_NAME, string);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void write(OutputStream os) throws IOException {
		// TODO Auto-generated method stub
	}

	private MetadataDirectoryImpl metaData = null;

	// calculate metaData on demand
	@Override
	protected IMetadataDirectory get() {
		if ((metaData == null)) {
			metaData = new MetadataDirectoryImpl().setName(MODUL_NAME);

			ensureDataRead();
			// MetadataDirectoryImpl child = new MetadataDirectoryImpl().setName(entry.getKey());
			// metaData.getSubdirectories().add(child);

			// final List<IMetadataTag> tags = child.getTags();
			// tags.add(new MetaDataTagImpl("type", thumbnail.getDataTypeAsString()));
		}
		return metaData;
	}
}