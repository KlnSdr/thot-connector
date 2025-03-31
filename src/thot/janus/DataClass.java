package thot.janus;

import dobby.util.json.NewJson;

import java.io.Serializable;

public interface DataClass extends Serializable {
    String getKey();

    NewJson toJson();
}
