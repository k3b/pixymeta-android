package pixy.meta;

public interface IMetadataTag {
	/**
     * Provides the name of the Tag, for display purposes.  E.g. <code>Aperture</code>
     *
     * @return the name of the Tag
     */
    String getName();

	/**
     * Provides a String the name of the Tag, for display purposes.  E.g. <code>5.6</code> for name=Aperture
     *
     * @return the value of the Tag
     */
    String getValue();
	
}
