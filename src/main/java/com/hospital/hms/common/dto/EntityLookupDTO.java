package com.hospital.hms.common.dto;

public class EntityLookupDTO {
    private final Long id;
    private final String name;

    public EntityLookupDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
