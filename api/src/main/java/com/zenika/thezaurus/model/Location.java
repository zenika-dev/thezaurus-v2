package com.zenika.thezaurus.model;

public class Location {
    private String city;
    private String country;
    private String address;
    private String postalCode;

    public Location() {}

    public Location(String city, String country, String address, String postalCode) {
        this.city = city;
        this.country = country;
        this.address = address;
        this.postalCode = postalCode;
    }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
}
