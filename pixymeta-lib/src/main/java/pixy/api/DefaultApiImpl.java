package pixy.api;

import java.util.ArrayList;
import java.util.List;

/**
 * This can be used as IDataType, IFieldDefinition or as IFieldValue
 * Created by k3b on 17.06.2016.
 */
public class DefaultApiImpl implements IDataType, IFieldDefinition, IFieldValue, IDirectory {
    private String name;
    private IFieldDefinition fieldDefinition;
    private String value;

    public static DefaultApiImpl UNKNOWN = new DefaultApiImpl("UNKNOWN");
    private List<IFieldValue> values = null;

    /** simuate IDataType, IDirectory or IFieldDefinition */
    public DefaultApiImpl(String name) {
        this.name = name;
        // this.value = value;
    }

    /** simualte IFieldValue */
    public DefaultApiImpl(IFieldDefinition fieldDefinition, String value) {
        this.fieldDefinition = fieldDefinition;
        this.value = value;
    }

    public DefaultApiImpl(String name, List<IFieldValue> values) {

        this.name = name;
        this.values = values;
    }

    @Override
    public IDirectory setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<IFieldValue> getValues() {
        if (values == null) values = new ArrayList<>();
        return values;
    }

    @Override
    public IFieldDefinition getDefinition() {
        return fieldDefinition;
    }

    @Override
    public String getValueAsString() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public IDataType getDataType() {
        return UNKNOWN;
    }

    public static boolean isNull(Object candidate) {
        return ((candidate == null) || (candidate == UNKNOWN));
    }
}
