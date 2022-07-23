package sparc.team3.validator.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AWS security group id and name
 */
public final class SecurityGroup {
    private final String id;
    private final String name;
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SecurityGroup(@JsonProperty("id") String id, @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "SecurityGroup{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
