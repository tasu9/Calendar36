package com.example.calendar3;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import java.util.List;
import java.util.ArrayList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.CalendarView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.Calendar;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private TextView selectedDate;
    private TextView eventTitleView;
    private TextView eventDescriptionView;
    private TextView kakunin1;
    private long selectedDateInMillis;
    private EventRepository repository;

    private TextView eventDetailsTextView; // 変数の宣言
    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        selectedDate = view.findViewById(R.id.selectedDate);
        eventTitleView = view.findViewById(R.id.selectedDate1); // 予定
        eventDescriptionView = view.findViewById(R.id.selectedDate2); // 説明
        kakunin1 = view.findViewById(R.id.kakunin);
        repository = new EventRepository(requireContext()); // EventRepositoryの初期化

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDateInMillis = CalendarUtils.getDateInMillis(year, month, dayOfMonth);
            updateSelectedDateText(year, month, dayOfMonth);
            showAddEventDialog(); // タッチされた日付でダイアログを表示
            showEventsForDate(selectedDateInMillis);
            kakunin1.setText("CalendarFragment" + "Selected Date in Millis: " + selectedDateInMillis);
        });

        return view;
    }

    private void updateSelectedDateText(int year, int month, int dayOfMonth) {
        Event event = repository.getEvent(selectedDateInMillis);
        if (event != null) {
            selectedDate.setText("Selected Date: " + year + "/" + (month + 1) + "/" + dayOfMonth);
            eventTitleView.setText("予定: " + event.getTitle());
            eventDescriptionView.setText("説明: " + event.getDescription());
        } else {
            selectedDate.setText("Selected Date: " + year + "/" + (month + 1) + "/" + dayOfMonth);
            eventTitleView.setText("予定: なし");
            eventDescriptionView.setText("説明: なし");
        }
    }

    private void showAddEventDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_event, null);

        EditText editTextEventTitle = dialogView.findViewById(R.id.editTextEventTitle);
        EditText editTextEventDescription = dialogView.findViewById(R.id.editTextEventDescription);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Event")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = editTextEventTitle.getText().toString();
                    String description = editTextEventDescription.getText().toString();
                    saveEvent(selectedDateInMillis, title, description);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showEventsForDate(long date) {
        List<Event> events = EventRepository.getEventsForDate(date); // 日付に関連するイベントを取得
        StringBuilder details = new StringBuilder();

        for (Event event : events) {
            details.append(event.getTitle()).append(": ").append(event.getDescription()).append("\n");
        }

        eventDetailsTextView.setText(details.toString());
    }


    private void saveEvent(long date, String title, String description) {
        Event existingEvent = repository.getEvent(date);

        if (existingEvent != null) {
            repository.updateEvent(date, title, description);
            Log.d("CalendarFragment", "Event updated: " + title + " on " + date);
        } else {
            repository.addEvent(date, title, description);
            Log.d("CalendarFragment", "Event saved: " + title + " on " + date);
        }

        updateSelectedDateText(
                CalendarUtils.getYear(date),
                CalendarUtils.getMonth(date),
                CalendarUtils.getDayOfMonth(date)
        );
    }

    // 画像から抽出したデータを追加するためのメソッド
    public void addExtractedEvents(List<String> dates, List<String> announcements) {
        if (dates.size() != announcements.size()) {
            Log.w("CalendarFragment", "Dates and announcements count do not match.");
            return;
        }

        for (int i = 0; i < dates.size(); i++) {
            String dateStr = dates.get(i);
            String announcement = announcements.get(i);
            long timestamp = convertDateToTimestamp(dateStr); // 日付をUNIXタイムスタンプに変換
            saveEvent(timestamp, announcement, "自動追加されたイベント");
        }
    }

    private long convertDateToTimestamp(String dateStr) {
        // 日付文字列をUNIXタイムスタンプに変換する処理を実装
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            return date != null ? date.getTime() : 0;
        } catch (ParseException e) {
            Log.e("CalendarFragment", "日付の解析に失敗しました", e);
            return 0;
        }
    }
}
