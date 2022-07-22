package team3.util;

public class SecurityGroup {
    private final String id;
    private final String name;

    public SecurityGroup(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
