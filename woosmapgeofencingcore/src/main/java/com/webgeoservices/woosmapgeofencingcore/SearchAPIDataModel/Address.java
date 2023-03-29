package com.webgeoservices.woosmapgeofencingcore.SearchAPIDataModel;

public class Address {
    private Object lines;
    private String country_code;
    private String city;
    private String zipcode;

    public Object getLines() {
        return lines;
    }

    public void setLines(Object value) {
        this.lines = value;
    }

    public String getCountryCode() {
        return country_code;
    }

    public void setCountryCode(String value) {
        this.country_code = value;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String value) {
        this.city = value;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String value) {
        this.zipcode = value;
    }
}
