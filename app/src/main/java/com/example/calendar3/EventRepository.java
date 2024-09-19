package com.example.calendar3;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import java.util.List;
import java.util.ArrayList;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class EventRepository {

    private static EventDatabaseHelper dbHelper;

    public EventRepository(Context context) {
        dbHelper = new EventDatabaseHelper(context);
    }

    public void addEvent(long date, String title, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("title", title);
        values.put("description", description);
        db.insert("events", null, values);
    }

    public void updateEvent(long date, String title, String description) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("description", description);
        String selection = "date = ?";
        String[] selectionArgs = {String.valueOf(date)};
        db.update("events", values, selection, selectionArgs);
    }

    public Event getEvent(long date) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {"title", "description"};
        String selection = "date = ?";
        String[] selectionArgs = {String.valueOf(date)};
        Cursor cursor = db.query("events", projection, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            cursor.close();
            return new Event(date, title, description);
        } else {
            return null;
        }
    }
    public static List<Event> getEventsForDate(long date) {
        List<Event> events = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] projection = {"date", "title", "description"};
        String selection = "date = ?";
        String[] selectionArgs = {String.valueOf(date)};
        Cursor cursor = db.query("events", projection, selection, selectionArgs, null, null, null);

        while (cursor.moveToNext()) {
            long eventDate = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
            String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            String description = cursor.getString(cursor.getColumnIndexOrThrow("description"));
            events.add(new Event(eventDate, title, description));
        }
        cursor.close();
        return events;
    }

}
