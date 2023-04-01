package com.lesa_humdet;

public class HumData {
    private int id;
    private double lat;
    private double lng;
    private String photoName;
    private String date;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "HumData{" +
                "id=" + id +
                ", lat=" + lat +
                ", lng=" + lng +
                ", photoName='" + photoName + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
