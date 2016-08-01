package pixy.api;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import pixy.api.IDirectory;
import pixy.meta.MetadataReader;
import pixy.meta.MetadataType;

/**
 * Created by k3b on 06.07.2016.
 */
public interface IMetadata extends MetadataReader {
	byte[] getData();

	MetadataType getType();

	boolean isDataRead();

	void showMetadata();

	/**
	 * Writes the metadata out to the output stream
	 *
	 * @param out OutputStream to write the metadata to
	 * @throws IOException
	 */
	void write(OutputStream out) throws IOException;

	/**
	 * @return directories that belong to this MetaData
	 * */
	List<IDirectory> getMetaData();

	IFieldValue getValue(IFieldDefinition tag);

	void merge(byte[] data);

	String getDebugMessage();
	void setDebugMessageBuffer(StringBuilder debugMessageBuffer);
}
