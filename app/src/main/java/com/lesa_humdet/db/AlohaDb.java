package com.lesa_humdet.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class AlohaDb extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "countries";
    SQLiteDatabase db = null;
    public AlohaDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void iniDb(SQLiteDatabase db_p) {
        db = db_p;
        try {
            String CREATE_COUNTRY_TABLE = "CREATE TABLE country( id INTEGER, name TEXT) " ;
            String CREATE_REGION_TABLE = "CREATE TABLE region(id INTEGER, country_id INTEGER, name TEXT) " ;
            String CREATE_CITY_TABLE = "CREATE TABLE city(id INTEGER,region_id INTEGER,name TEXT) " ;
            db.execSQL(CREATE_COUNTRY_TABLE);
            db.execSQL(CREATE_REGION_TABLE);
            db.execSQL(CREATE_CITY_TABLE);
        }catch (SQLiteException e){}

    }

    public void close() {
        db.close();
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void addCountry(Country country) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", country.getId());
        values.put("name", country.getName());
        db.insert("country", null, values);
        db.close(); // Closing database connection
    }
    public void addRegion(Region region) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", region.getId());
        values.put("country_id", region.getCountry_id());
        values.put("name", region.getName());
        db.insert("region", null, values);
        db.close(); // Closing database connection
    }
    public void addCity(City city) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", city.getId());
        values.put("region_id", city.getRegion_id());
        values.put("name", city.getName());
        db.insert("city", null, values);
        db.close(); // Closing database connection
    }


    public List<Country> getAllCountry() {
        List<Country> list = new ArrayList<Country>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("country", new String[] { "id","name" }, null,null, null, null, null, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                Country country = new Country();
                country.setId(cursor.getInt(0));
                country.setName(cursor.getString(1));
                list.add(country);
                cursor.moveToNext();
            }
        }
        return list;
    }

    public List<Region> getRegions(int country_id) {
        List<Region> list = new ArrayList<Region>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("region", new String[] { "id","name" }, "country_id" + "=?", new String[] { String.valueOf(country_id) }, null, null, null, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                Region region = new Region();
                region.setId(cursor.getInt(0));
                region.setName(cursor.getString(1));
                list.add(region);
                cursor.moveToNext();
            }
        }
        return list;
    }
    public List<City> getCities(int region_id) {
        List<City> list = new ArrayList<City>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("city", new String[] { "id","name" }, "region_id" + "=?", new String[] { String.valueOf(region_id) }, null, null, null, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                City city = new City();
                city.setId(cursor.getInt(0));
                city.setName(cursor.getString(1));
                list.add(city);
                cursor.moveToNext();
            }
        }
        return list;
    }

    public Country getCountry(int id) {
        try{
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.query("country", new String[] { "id","name" }, "id" + "=?",
                    new String[] { String.valueOf(id) }, null, null, null, null);
            if (cursor != null)
                cursor.moveToFirst();
            Country country = new Country();
            country.setId(cursor.getInt(0));
            country.setName(cursor.getString(1));
            return country;
        }catch (Exception e){}
        return null;
    }

}































