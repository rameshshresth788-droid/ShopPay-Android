package com.ramesh.shoppay;

import android.app.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.*;
import org.json.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final String PREF = "ShopPayNative";
    private final String DATA = "payments";
    private final String NOTES = "notes";
    private String selectedDate = today();
    private LinearLayout root, recordsBox, notesBox;
    private TextView dateText, cashTotal, onlineTotal, grandTotal, noteTotal;
    private final int bg = Color.rgb(9, 10, 18);
    private final int panel = Color.rgb(22, 24, 40);
    private final int purple = Color.rgb(124, 83, 235);
    private final int[] denominations = {500, 200, 100, 50, 20, 10, 5, 2, 1};

    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private JSONObject read(String key) {
        try {
            return new JSONObject(getSharedPreferences(PREF, MODE_PRIVATE)
                    .getString(key, "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private void save(String key, JSONObject data) {
        getSharedPreferences(PREF, MODE_PRIVATE).edit()
                .putString(key, data.toString()).commit();
    }

    private JSONArray entries() {
        JSONArray a = read(DATA).optJSONArray(selectedDate);
        return a == null ? new JSONArray() : a;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private TextView button(String label, int color) {
        TextView b = text(label, 16, Color.WHITE);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(shape(color, 24));
        b.setPadding(18, 16, 18, 16);
        return b;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private void gap(LinearLayout box, int height) {
        View v = new View(this);
        box.addView(v, new LinearLayout.LayoutParams(1, height));
    }

    private void section(LinearLayout box, View child) {
        child.setPadding(14, 14, 14, 14);
        child.setBackground(shape(panel, 24));
        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 8, 0, 8);
        box.addView(child, p);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        buildScreen();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bg);
        root = column();
        root.setPadding(18, 18, 18, 24);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = text("ShopPay", 30, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        root.addView(text("Simple Daily Hisaab", 17,
                Color.LTGRAY));
        gap(root, 18);

        LinearLayout datePanel = column();
        dateText = text("", 19, Color.WHITE);
        dateText.setPadding(12, 14, 12, 14);
        dateText.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            try {
                c.setTime(new SimpleDateFormat("yyyy-MM-dd",
                        Locale.US).parse(selectedDate));
            } catch (Exception ignored) {}
            new DatePickerDialog(this, (view, year, month, day) -> {
                Calendar picked = Calendar.getInstance();
                picked.set(year, month, day);
                selectedDate = new SimpleDateFormat("yyyy-MM-dd",
                        Locale.US).format(picked.getTime());
                refresh();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)).show();
        });
        datePanel.addView(dateText);
        section(root, datePanel);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        TextView cashBtn = button("+ Add Cash", 0xFFFFB82E);
        cashBtn.setTextColor(Color.BLACK);
        cashBtn.setOnClickListener(v -> addPayment("Cash"));
        TextView onlineBtn = button("+ Add Online", 0xFF087F78);
        onlineBtn.setOnClickListener(v -> addPayment("Online"));

        LinearLayout.LayoutParams half =
                new LinearLayout.LayoutParams(0, -2, 1);
        half.setMargins(3, 3, 3, 3);
        buttons.addView(cashBtn, half);
        buttons.addView(onlineBtn, half);
        section(root, buttons);

        cashTotal = text("", 21, 0xFFFFC44D);
        onlineTotal = text("", 21, 0xFF53D7C5);
        grandTotal = text("", 23, Color.WHITE);

        LinearLayout totals = column();
        totals.addView(cashTotal);
        totals.addView(onlineTotal);
        totals.addView(grandTotal);
        section(root, totals);

        TextView recordTitle = text("Today's Records", 20, Color.WHITE);
        recordTitle.setTypeface(null, Typeface.BOLD);
        root.addView(recordTitle);
        recordsBox = column();
        section(root, recordsBox);

        TextView noteTitle = text("Note Calculator", 20, Color.WHITE);
        noteTitle.setTypeface(null, Typeface.BOLD);
        root.addView(noteTitle);

        notesBox = column();
        section(root, notesBox);

        noteTotal = text("Notes Total: ₹0", 21, 0xFFFFC44D);
        section(root, noteTotal);

        root.addView(text("Data phone mein save hota hai.",
                13, Color.LTGRAY));
        refresh();
    }

    private void addPayment(String type) {
        LinearLayout form = column();
        form.setPadding(10, 4, 10, 4);

        EditText amount = new EditText(this);
        amount.setHint("Amount (₹)");
        amount.setInputType(8194);
        amount.setTextColor(Color.BLACK);
        amount.setHintTextColor(Color.GRAY);
        amount.setBackgroundColor(Color.WHITE);
        form.addView(amount);

        Spinner gender = new Spinner(this);
        String[] options = {"Male", "Female"};
        gender.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, options));
        form.addView(gender);

        new AlertDialog.Builder(this)
                .setTitle("Add " + type + " Payment")
                .setView(form)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        double value = Double.parseDouble(
                                amount.getText().toString().trim());
                        if (!Double.isFinite(value) || value <= 0) {
                            Toast.makeText(this, "Sahi amount bharein",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONObject all = read(DATA);
                        JSONArray list = all.optJSONArray(selectedDate);
                        if (list == null) list = new JSONArray();

                        JSONObject row = new JSONObject();
                        row.put("id", UUID.randomUUID().toString());
                        row.put("type", type);
                        row.put("amount", Math.round(value * 100.0) / 100.0);
                        row.put("gender", gender.getSelectedItem().toString());
                        row.put("time", new SimpleDateFormat("hh:mm a",
                                Locale.getDefault()).format(new Date()));

                        list.put(row);
                        all.put(selectedDate, list);
                        save(DATA, all);
                        refresh();

                        Toast.makeText(this, "Payment saved",
                                Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Amount check karein",
                                Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void refresh() {
        if (dateText == null) return;

        dateText.setText("Date: " + selectedDate
                + "     📅 Change Date");
        recordsBox.removeAllViews();
        notesBox.removeAllViews();

        double cash = 0, online = 0;
        JSONArray list = entries();

        for (int i = 0; i < list.length(); i++) {
            JSONObject row = list.optJSONObject(i);
            if (row == null) continue;

            double amount = row.optDouble("amount", 0);
            if ("Cash".equals(row.optString("type"))) cash += amount;
            else online += amount;

            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            TextView info = text(row.optString("type") + " ₹"
                    + amount + "\n" + row.optString("gender")
                    + " • " + row.optString("time"), 15, Color.WHITE);
            info.setGravity(Gravity.START);
            line.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

            TextView del = button("Delete", 0xFF9E3548);
            del.setOnClickListener(v ->
                    new AlertDialog.Builder(this)
                    .setMessage("Entry delete karein?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (d, w) -> {
                        JSONArray old = entries();
                        JSONArray newer = new JSONArray();
                        for (int k = 0; k < old.length(); k++) {
                            JSONObject x = old.optJSONObject(k);
                            if (x != null && !x.optString("id")
                                    .equals(row.optString("id"))) newer.put(x);
                        }
                        JSONObject all = read(DATA);
                        try { all.put(selectedDate, newer); }
                        catch (Exception ignored) {}
                        save(DATA, all);
                        refresh();
                    }).show());
            line.addView(del);
            recordsBox.addView(line);
            gap(recordsBox, 8);
        }

        cashTotal.setText("Offline Cash: ₹" + cash);
        onlineTotal.setText("Online Payment: ₹" + online);
        grandTotal.setText("Total Payment: ₹" + (cash + online));

        JSONObject allNotes = read(NOTES);
        JSONObject dayNotes = allNotes.optJSONObject(selectedDate);
        if (dayNotes == null) dayNotes = new JSONObject();

        final JSONObject currentNotes = dayNotes;
        final double[] noteSum = {0};

        for (int denomination : denominations) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text("₹" + denomination, 17, Color.WHITE);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));

            EditText count = new EditText(this);
            count.setSingleLine(true);
            count.setHint("Count");
            count.setTextColor(Color.WHITE);
            count.setHintTextColor(Color.LTGRAY);
            count.setInputType(2);
            count.setText(String.valueOf(currentNotes.optInt(
                    String.valueOf(denomination), 0)));
            row.addView(count, new LinearLayout.LayoutParams(110, -2));

            TextView subtotal = text("", 14, 0xFFFFC44D);
            row.addView(subtotal, new LinearLayout.LayoutParams(95, -2));
            Runnable calculate = () -> {
                int n = currentNotes.optInt(String.valueOf(denomination), 0);
                subtotal.setText("₹" + (denomination * n));
                double sum = 0;
                for (int d : denominations)
                    sum += d * currentNotes.optInt(String.valueOf(d), 0);
                noteTotal.setText("Notes Total: ₹" + sum);
            };
            calculate.run();

            count.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int st,
                                              int c, int a) {}
                public void onTextChanged(CharSequence s, int st,
                                          int before, int countChars) {
                    int n;
                    try { n = Math.max(0, Integer.parseInt(s.toString())); }
                    catch (Exception e) { n = 0; }
                    try {
                        currentNotes.put(String.valueOf(denomination), n);
                        JSONObject all = read(NOTES);
                        all.put(selectedDate, currentNotes);
                        save(NOTES, all);
                    } catch (Exception ignored) {}
                    calculate.run();
                }
                public void afterTextChanged(Editable e) {}
            });

            notesBox.addView(row);
        }
    }
}
