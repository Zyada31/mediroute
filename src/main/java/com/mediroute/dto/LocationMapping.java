package com.mediroute.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class LocationMapping {
    private List<String> locationStrings = new ArrayList<>();
    private Map<Integer, String> indexToEntity = new HashMap<>();
    private Map<String, Integer> entityToIndex = new HashMap<>();

    public void addLocation(String locationString, String entity) {
        int index = locationStrings.size();
        locationStrings.add(locationString);
        indexToEntity.put(index, entity);
        entityToIndex.put(entity, index);
    }

    public int getTotalNodes() {
        return locationStrings.size();
    }

    public String getEntityForIndex(int index) {
        return indexToEntity.get(index);
    }

    public Integer getIndexForEntity(String entity) {
        return entityToIndex.get(entity);
    }

    public List<String> getLocationStrings() {
        return locationStrings;
    }
}
