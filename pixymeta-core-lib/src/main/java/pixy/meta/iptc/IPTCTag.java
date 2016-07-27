package pixy.meta.iptc;

import pixy.api.IDataType;
import pixy.api.IFieldDefinition;

/**
 * {@link pixy.meta.iptc.IPTCTag}-s in {@link pixy.meta.iptc.IPTC} containers
 *    and stored as {@link pixy.meta.adobe.AdobeIRBSegment}
 */
public interface IPTCTag extends IFieldDefinition {
	public int getTag();
	public String getName();
	public boolean allowMultiple();
	public String getDataAsString(byte[] data);

}