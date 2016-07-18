package pixy.meta;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import pixy.api.DefaultApiImpl;
import pixy.api.IDataType;
import pixy.api.IDirectory;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.api.IMetadata;
import pixy.demo.j2se.TestPixyMetaJ2se;
import pixy.image.exifFields.FieldType;
import pixy.meta.exif.Tag;
import pixy.string.StringUtils;

/**
 * Long running regression test that verifies that found datatypes are
 * compartible to declared datatybes.
 *
 * Created by k3b on 18.07.2016.
 */
@RunWith(JUnitParamsRunner.class)
public class MetadataTypeRegressionTests {
    // used by JUnitParamsRunner fileName, dir, value
    private Object getAllFieldValuesForTest() throws IOException {
        ArrayList<Object> result = new ArrayList<>();
        for (String fileName : MetadataRegressionTests.allTestFiles) {
            InputStream stream = null;
            try {
                stream = TestPixyMetaJ2se.class.getResourceAsStream("images/" + fileName);
                Map<MetadataType, IMetadata> metadataMap = Metadata.readMetadata(stream);
                if (metadataMap == null) continue;

                for (IMetadata exif : metadataMap.values()) {
                    if (exif == null) continue;
                    List<IDirectory> exifDir = exif.getMetaData();
                    if (exifDir == null) continue;
                    for (IDirectory dir : exifDir) {
                        final List<IFieldValue> values = dir.getValues();
                        if (values != null) {
                            for (IFieldValue value : values) {
                                if (value != null) {
                                    result.add(new Object[]{fileName,dir, value});
                                }
                            }
                        }
                    }
                }
            } finally {
                stream.close();

            }
        }
        return result;
    }

    @Test
    // @Parameters({"12.jpg"})
    @Parameters(method = "getAllFieldValuesForTest")
    public void declaredTypeShouldMatchFoundType(String fileName, IDirectory dir, IFieldValue value) throws IOException {
        final IFieldDefinition fieldDefinition = value.getDefinition();

        IDataType valueDataType = value.getDataType();
        IDataType defDataType = fieldDefinition.getDataType();

        if (!isCompatible(valueDataType, defDataType)) {
            String fieldDefinitionName = fieldDefinition.getName();
            if (fieldDefinition instanceof Tag) {
                fieldDefinitionName = StringUtils.toHexStringMM(((Tag) fieldDefinition).getValue()) +"-" + fieldDefinitionName;
            }

            // 0x0009-ExifVersion:Unknown => Undefined
            StringBuilder wrongExifFieldTypes = new StringBuilder()
                    .append(fileName)
                    .append(":")
                    .append(dir.getName())
                    .append(".")
                    .append(fieldDefinitionName)
                    .append(":").append(defDataType.getName())
                    .append(" => ").append(valueDataType)
                    ;
            Assert.fail(wrongExifFieldTypes.toString());
        }

    }

    private boolean isCompatible(IDataType valueDataType, IDataType definitionDataType) {
        if (DefaultApiImpl.isNull(valueDataType)) return true;
        if (valueDataType == definitionDataType) return true;
        if (valueDataType.getName().toLowerCase().startsWith("un")) return true; // Unknown, Undefenied, ...

        // Long => Short is ok
        if ( (definitionDataType == FieldType.LONG) && (valueDataType == FieldType.SHORT)) return true;

        return false;
    }

}