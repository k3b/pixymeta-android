package pixy.meta;

import java.util.List;

public interface IMetadataDirectory {
	/**
     * Provides the name of the directory, for display purposes.  E.g. <code>Exif</code>
     *
     * @return the name of the directory
     */
    String getName();

    /**
     * @return sub-directories that belong to this Directory or null if there are no sub-directories
     */
	List<IMetadataDirectory> getSubdirectories();

    /**
     * @return Tags that belong to this Directory or null if there are no tags
     */
    List<IMetadataTag> getTags();
}
