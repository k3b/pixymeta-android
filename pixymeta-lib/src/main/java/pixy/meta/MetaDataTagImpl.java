package pixy.meta;

/**
 * Created by k3b on 15.06.2016.
 */
public class MetaDataTagImpl implements  IMetadataTag {
    private final String name;
    private final String value;

    public MetaDataTagImpl(String name, String value) {
        this.name = name;

        this.value = value;
    }

    /**
     * Provides the name of the Tag, for display purposes.  E.g. <code>Aperture</code>
     *
     * @return the name of the Tag
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Provides a String the name of the Tag, for display purposes.  E.g. <code>5.6</code> for name=Aperture
     *
     * @return the value of the Tag
     */
    @Override
    public String getValue() {
        return value;
    }
}
