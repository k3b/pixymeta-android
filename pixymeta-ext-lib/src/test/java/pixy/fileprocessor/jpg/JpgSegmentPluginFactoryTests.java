package pixy.fileprocessor.jpg;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import pixy.api.IFieldDefinition;
import pixy.api.IMetadata;
import pixy.meta.MetadataType;
import pixy.meta.exif.ExifImageTag;
import pixy.meta.exif.ExifSubTag;
import pixy.meta.exif.GPSTag;
import pixy.meta.image.Comments;
import pixy.meta.iptc.IPTCApplicationTag;
import pixy.meta.jpeg.JPEGMeta;

/**
 * Copyright (C) 2016 by k3b.
 *
 * Created by k3b on 11.07.2016.
 */
@RunWith(JUnitParamsRunner.class)
public class JpgSegmentPluginFactoryTests {
    @BeforeClass
    public static void initDirectories() {
        JPEGMeta.register();
    }

    private static final IFieldDefinition allExampleTags[] = new IFieldDefinition[]{
        Comments.CommentTag,

        ExifImageTag.DATETIME,
        ExifSubTag.LENS_Make,
        GPSTag.GPS_ALTITUDE,

        IPTCApplicationTag.KEY_WORDS
    };

    // used by JUnitParamsRunner
    private IFieldDefinition[] getExampleTags() {
        return allExampleTags;
    }

    @Test
    @Parameters(method = "getExampleTags")
    public void shouldFindDefinition(IFieldDefinition tag)  {
        JpgSegmentPluginFactory found = JpgSegmentPluginFactory.find(tag.getClass());
        Assert.assertNotNull(tag.getName() + ":" + tag.getClass().getSimpleName(), found);
    }

    private static final MetadataType[] allExampleMetaDefs = new MetadataType[]{
            MetadataType.COMMENT,
            MetadataType.EXIF,
            MetadataType.ICC_PROFILE
    };

    // used by JUnitParamsRunner
    private MetadataType[] getExampleMetaDefs() {
        return allExampleMetaDefs;
    }

/*
    @Test
    @Parameters(method = "getExampleMetaDefs")
    public void shouldFindDefinition(MetadataType tag)  {
        JpgSegmentPluginFactory found = JpgSegmentPluginFactory.find(tag.getClass());
        Assert.assertNotNull(tag.getName() + ":" + tag.getClass().getSimpleName(), found);
    }
*/
}
