package org.nemesis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    ADMIN("admin"),
    USER("user");

    private final String name;

    Role(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @JsonCreator
    public static Role fromString(String value) {
        if (value == null) return null;
        for (Role role : Role.values())
            if (role.name.equalsIgnoreCase(value))
                return role;
        return null; // nothing matched
    }
}