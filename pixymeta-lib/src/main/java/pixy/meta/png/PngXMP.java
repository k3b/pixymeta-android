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
	private MetadataDirectoryImpl get() {
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

	/**
	 * Provides the name of the directory, for display purposes.  E.g. <code>Exif</code>
	 *
	 * @return the name of the directory
	 */
	@Override
	public String getName() {
		return get().getName();
	}

	/**
	 * @return sub-directories that belong to this Directory or null if there are no sub-directories
	 */
	@Override
	public List<IMetadataDirectory> getSubdirectories() {
		return get().getSubdirectories();
	}

	/**
	 * @return Tags that belong to this Directory or null if there are no tags
	 */
	@Override
	public List<IMetadataTag> getTags() {
		return get().getTags();
	}

}