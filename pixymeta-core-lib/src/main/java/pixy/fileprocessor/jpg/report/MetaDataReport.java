package pixy.fileprocessor.jpg.report;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pixy.api.DefaultApiImpl;
import pixy.api.IDataType;
import pixy.api.IDirectory;
import pixy.api.IFieldDefinition;
import pixy.api.IFieldValue;
import pixy.api.IMetadata;
import pixy.meta.MetadataType;

/**
 * Convert metadata as a string report.
 *
 * Created by k3b on 31.07.2016.
 */
public class MetaDataReport {

    /** zum sortieren der ausgabe */
    private static Comparator<Object> toStringComparator = new Comparator<Object>() {
        @Override
        public int compare(Object lhs, Object rhs) {
            return ("" + lhs).compareTo("" + rhs);
        }
    };


    public void getReport(Map<MetadataType, IMetadata> metadataMap, StringBuffer result) {
        // sort items to prevent regression errors
        final Set<MetadataType> metadataTypeSet = (metadataMap == null) ? null : metadataMap.keySet();
        if (metadataTypeSet != null) {
            MetadataType[] keys = metadataTypeSet.toArray(new MetadataType[metadataTypeSet.size()]);
            Arrays.sort(keys, toStringComparator);
            for (MetadataType key : keys) {
                result.append(key).append("\n");

                IMetadata metaData = metadataMap.get(key);
                if (isVisible(metaData)) {
                    try {
                        List<IDirectory> metaDir = metaData.getMetaData();
                        if (metaDir != null) {
                            IDirectory[] dirs = metaDir.toArray(new IDirectory[metaDir.size()]);
                            Arrays.sort(dirs, toStringComparator);

                            for (IDirectory dir : dirs) {
                                if (isVisible(dir)) {
                                    final List<IFieldValue> valueList = dir.getValues();
                                    if (valueList != null) {
                                        IFieldValue[] values = valueList.toArray(new IFieldValue[valueList.size()]);
                                        Arrays.sort(values, toStringComparator);
                                        for (IFieldValue value : values) {
                                            formatValue(result, dir, value);
                                        }
                                    }
                                }
                            }
                            result.append("\n----------------------\n");
                        }

                    } catch (Exception ex) {
                        result.append(ex.getMessage()).append("\n").append(ex.getStackTrace()).append("\n");
                    } finally {
                        String dbgMessage = metaData.getDebugMessage();
                        if ((dbgMessage != null) && (dbgMessage.length() > 0))
                            result.append("\ndebug:\n").append(dbgMessage).append("\n----------------------\n");
                    }
                }
            }
        }
    }

    // add "dirName.fieldDefinitionName[dataTypeName]=valueAsString" to result
    private void formatValue(StringBuffer result, IDirectory dir, IFieldValue fieldValue) {
        if (isVisible(fieldValue)) {
            final String dirName = dir.getName();
            final IFieldDefinition fieldDefinition = fieldValue.getDefinition();

            IDataType dataType = fieldValue.getDataType();
            if (DefaultApiImpl.isNull(dataType) && !DefaultApiImpl.isNull(fieldDefinition))
                dataType = fieldDefinition.getDataType();

            if (isVisible(fieldDefinition, dataType)) {
                final String dataTypeName = DefaultApiImpl.isNull(dataType) ? null : dataType.getName();
                final String valueAsString = (!isCompressLongValues())
                        ? fieldValue.getValueAsString()
                        : DataFormatter.abreviateValue(fieldValue.getValueAsString());
                final String fieldDefinitionName = DefaultApiImpl.isNull(fieldDefinition) ? null : fieldDefinition.getName();

                result.append("\t");

                if (dirName != null) result.append(dirName);

                if (fieldDefinitionName != null) result.append(".").append(fieldDefinitionName);
                if (dataTypeName != null) result.append("[").append(dataTypeName).append("]");
                if (valueAsString != null) result.append("=").append(valueAsString);
                result.append("\n");
            }
        }
    }

    /** how should the item be formatted */
    private boolean isCompressLongValues() {
        return true;
    }

    /** what should be visible in the report */
    protected boolean isVisible(final IDirectory dir) {
        return dir != null;
    }

    /** what should be visible in the report */
    private boolean isVisible(IFieldValue value) {
        return value != null;
    }

    /** what should be visible in the report */
    protected boolean isVisible(final IFieldDefinition fieldDefinition, final IDataType dataType) {
        return true;
    }

    /** what should be visible in the report */
    protected boolean isVisible(final IMetadata metaData) {
        return metaData != null;
    }

}
