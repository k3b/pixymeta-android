package pixy.api;

import java.util.List;

/**
 * Created by k3b on 17.06.2016.
 */
public interface IDirectory {
    IDirectory setName(String name);
    String getName();
    List<IFieldValue> getValues();
}
