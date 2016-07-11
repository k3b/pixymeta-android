package pixy.meta.iptc;

import pixy.api.IDataType;
import pixy.api.IFieldDefinition;

public interface IPTCTag extends IFieldDefinition {
	public int getTag();
	public String getName();
	public boolean allowMultiple();
	public String getDataAsString(byte[] data);

}