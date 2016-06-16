package pixy.meta;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by k3b on 15.06.2016.
 */
public class MetadataDirectoryImpl implements IMetadataDirectory {
    private String name;
    private List<IMetadataTag> tags = new ArrayList<IMetadataTag>();
    private List<IMetadataDirectory> subdirectories = new ArrayList<IMetadataDirectory>();

    public MetadataDirectoryImpl setName(String value) {
        name = value;
        return this;
    }
    /**
     * Provides the name of the directory, for display purposes.  E.g. <code>Exif</code>
     *
     * @return the name of the directory
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return sub-directories that belong to this Directory or null if there are no sub-directories
     */
    @Override
    public List<IMetadataDirectory> getSubdirectories() {
        return subdirectories;
    }

    /**
     * @return Tags that belong to this Directory or null if there are no tags
     */
    @Override
    public List<IMetadataTag> getTags() {
        return tags;
    }
}
