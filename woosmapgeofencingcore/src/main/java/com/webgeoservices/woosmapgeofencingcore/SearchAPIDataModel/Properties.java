package com.webgeoservices.woosmapgeofencingcore.SearchAPIDataModel;

import java.util.HashMap;

public class Properties {
    private String store_id;
    private String name;
    private Object contact;
    private Address address;
    private HashMap<String, Object> user_properties;
    private Object[] tags;
    private Object[] types;
    private Double distance;
    private HashMap<String, Object> opening_hours;

    private HashMap<String, Object> open;

    public String getStoreID() {
        return store_id;
    }

    public void setStoreID(String value) {
        this.store_id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public Object getContact() {
        return contact;
    }

    public void setContact(Object value) {
        this.contact = value;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address value) {
        this.address = value;
    }

    public HashMap<String, Object> getUserProperties() {
        return user_properties;
    }

    public void setUserProperties(HashMap<String, Object> value) {
        this.user_properties = value;
    }

    public Object[] getTags() {
        return tags;
    }

    public void setTags(Object[] value) {
        this.tags = value;
    }

    public Object[] getTypes() {
        return types;
    }

    public void setTypes(Object[] value) {
        this.types = value;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double value) {
        this.distance = value;
    }

    public HashMap<String, Object> getOpen() {return open;}

    public void setOpen(HashMap<String, Object> open) {this.open = open;}

    public HashMap<String, Object> getOpeningHours() {
        return opening_hours;
    }

    public void setOpeningHours(HashMap<String, Object> value) {
        this.opening_hours = value;
    }
}
