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
    private LinearLayout root, recordsBox, cashRecordsBox, onlineRecordsBox, notesBox;
    private TextView dateText, cashTotal, onlineTotal, grandTotal, noteTotal, cashCheck, onlineCheck, savedStatus;
    private EditText actualOnlineInput;
    private final String ACTUAL_ONLINE = "actual_online";
    private final String DAILY_SAVED = "daily_saved";
    private final int bg = Color.rgb(7, 10, 20);
    private final int panel = Color.rgb(19, 25, 43);
    private final int purple = Color.rgb(117, 92, 255);
    private final int muted = Color.rgb(159, 171, 195);
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
        d.setStroke(1, 0x334F6B91);
        return d;
    }

    private GradientDrawable gradient(int first, int second, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{first, second});
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setFontFeatureSettings("kern");
        return t;
    }

    private void animateIn(View view, int index) {
        view.setAlpha(0f);
        view.setTranslationY(18f);
        view.animate().alpha(1f).translationY(0f)
                .setStartDelay(Math.min(index * 35L, 210L))
                .setDuration(260L).start();
    }

    private TextView button(String label, int color) {
        TextView b = text(label, 15, Color.WHITE);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(gradient(color, adjustColor(color, 0.78f), 22));
        b.setPadding(16, 15, 16, 15);
        b.setElevation(3f);
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP ||
                    event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
            }
            return false;
        });
        return b;
    }

    private int adjustColor(int color, float factor) {
        return Color.rgb(Math.min(255, (int)(Color.red(color) * factor)),
                Math.min(255, (int)(Color.green(color) * factor)),
                Math.min(255, (int)(Color.blue(color) * factor)));
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
        child.setPadding(16, 16, 16, 16);
        child.setBackground(shape(panel, 24));
        child.setElevation(2f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 7, 0, 7);
        box.addView(child, p);
        animateIn(child, box.getChildCount());
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xFF080D1B);
        getWindow().setNavigationBarColor(0xFF080D1B);
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
        buildScreen();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF080D1B, 0xFF11152A, 0xFF080D1B}));
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root = column();
        root.setPadding(18, 18, 18, 28);
        scroll.addView(root);
        setContentView(scroll);

        TextView logo = text("S", 25, Color.WHITE);
        logo.setTypeface(null, Typeface.BOLD);
        logo.setBackground(gradient(0xFF7C5CFF, 0xFF22D3C5, 22));
        LinearLayout.LayoutParams logoSize = new LinearLayout.LayoutParams(58, 58);
        logoSize.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(logo, logoSize);
        TextView title = text("SHOPPAY", 28, Color.WHITE);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        root.addView(title);
        TextView tagline = text("SMART DAILY HISSAAB  •  2050 EDITION", 11, muted);
        tagline.setLetterSpacing(0.08f);
        root.addView(tagline);
        gap(root, 14);

        LinearLayout datePanel = column();
        dateText = text("", 16, Color.WHITE);
        dateText.setTypeface(null, Typeface.BOLD);
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

        cashTotal = text("", 18, 0xFFFFC65C);
        cashTotal.setPadding(8, 10, 8, 10);
        onlineTotal = text("", 18, 0xFF55E6D1);
        onlineTotal.setPadding(8, 10, 8, 10);
        grandTotal = text("", 23, Color.WHITE);
        grandTotal.setTypeface(null, Typeface.BOLD);
        grandTotal.setPadding(8, 12, 8, 12);
        grandTotal.setBackground(gradient(0xFF3A2D85, 0xFF164D5B, 20));

        LinearLayout totals = column();
        totals.addView(cashTotal);
        totals.addView(onlineTotal);
        totals.addView(grandTotal);
        section(root, totals);

        TextView cashRecordTitle = text("🟠 OFFLINE CASH RECORDS", 20, 0xFFFFC65C);
        cashRecordTitle.setTypeface(null, Typeface.BOLD);
        root.addView(cashRecordTitle);
        cashRecordsBox = column();
        section(root, cashRecordsBox);

        TextView onlineRecordTitle = text("🟢 ONLINE PAYMENT RECORDS", 20, 0xFF55E6D1);
        onlineRecordTitle.setTypeface(null, Typeface.BOLD);
        root.addView(onlineRecordTitle);
        onlineRecordsBox = column();
        section(root, onlineRecordsBox);

        TextView noteTitle = text("Note Calculator", 20, Color.WHITE);
        noteTitle.setTypeface(null, Typeface.BOLD);
        root.addView(noteTitle);

        notesBox = column();
        section(root, notesBox);

        noteTotal = text("Notes Total: ₹0", 21, 0xFFFFC44D);
        section(root, noteTotal);

        TextView reconcileTitle = text("HISAAB MILAN", 20, 0xFFB8A5FF);
        reconcileTitle.setTypeface(null, Typeface.BOLD);
        root.addView(reconcileTitle);

        cashCheck = text("Cash difference: ₹0", 17, Color.WHITE);
        section(root, cashCheck);

        LinearLayout onlineCheckPanel = column();
        onlineCheckPanel.addView(text("Actual UPI / Online total", 16, Color.WHITE));
        actualOnlineInput = new EditText(this);
        actualOnlineInput.setSingleLine(true);
        actualOnlineInput.setHint("Bank/UPI app ka total (₹)");
        actualOnlineInput.setInputType(8194);
        actualOnlineInput.setTextColor(Color.WHITE);
        actualOnlineInput.setHintTextColor(Color.LTGRAY);
        actualOnlineInput.setBackground(shape(0xFF30334A, 16));
        actualOnlineInput.setPadding(14, 8, 14, 8);
        onlineCheckPanel.addView(actualOnlineInput);
        actualOnlineInput.setText(String.valueOf(read(ACTUAL_ONLINE).optDouble(selectedDate, 0)));
        actualOnlineInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                try {
                    JSONObject all = read(ACTUAL_ONLINE);
                    String val = s.toString().trim();
                    all.put(selectedDate, val.isEmpty() ? 0 : Double.parseDouble(val));
                    save(ACTUAL_ONLINE, all);
                } catch (Exception ignored) {}
                updateReconciliation();
            }
            public void afterTextChanged(Editable e) {}
        });
        section(root, onlineCheckPanel);
        onlineCheck = text("Online difference: ₹0", 17, Color.WHITE);
        section(root, onlineCheck);

        TextView saveToday = button("✓  SAVE TODAY", 0xFF7654F6);
        saveToday.setOnClickListener(v -> saveToday());
        section(root, saveToday);
        TextView history = button("▣  VIEW SAVED HISTORY", 0xFF30334A);
        history.setOnClickListener(v -> showHistory());
        section(root, history);
        savedStatus = text("", 13, Color.LTGRAY);
        root.addView(savedStatus);

        root.addView(text("Data phone mein save hota hai.", 13, Color.LTGRAY));
        refresh();
    }

    private void addPayment(String type) {
        LinearLayout form = column();
        form.setPadding(10, 4, 10, 4);

        EditText amount = new EditText(this);
        amount.setHint("Amount (₹)");
        amount.setInputType(8194);
        amount.setTextColor(Color.WHITE);
        amount.setHintTextColor(muted);
        amount.setPadding(16, 12, 16, 12);
        amount.setBackground(shape(0xFF252D45, 16));
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
        cashRecordsBox.removeAllViews();
        onlineRecordsBox.removeAllViews();
        notesBox.removeAllViews();
        if (actualOnlineInput != null) {
            actualOnlineInput.setText(String.valueOf(read(ACTUAL_ONLINE).optDouble(selectedDate, 0)));
        }

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
            LinearLayout targetBox = "Cash".equals(row.optString("type"))
                    ? cashRecordsBox : onlineRecordsBox;
            targetBox.addView(line);
            gap(targetBox, 8);
        }

        cashTotal.setText("Offline Cash: ₹" + cash);
        onlineTotal.setText("Online Payment: ₹" + online);
        grandTotal.setText("GRAND TOTAL: ₹" + money(cash + online));

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
            count.setHintTextColor(muted);
            count.setBackground(shape(0xFF252D45, 14));
            count.setPadding(12, 7, 12, 7);
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
                noteTotal.setText("Notes Total: ₹" + money(sum));
                updateReconciliation();
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
        updateReconciliation();
        if (savedStatus != null) {
            savedStatus.setText(read(DAILY_SAVED).has(selectedDate)
                    ? "✓ " + selectedDate + " ka hisaab saved hai" : "Aaj ka hisaab abhi save nahi hua");
        }
    }

    private String money(double n) {
        return String.format(Locale.US, "%.2f", n).replaceAll("\\.00$", "");
    }

    private double getCashTotal() {
        double total = 0;
        JSONArray list = entries();
        for (int i = 0; i < list.length(); i++) {
            JSONObject r = list.optJSONObject(i);
            if (r != null && "Cash".equals(r.optString("type"))) total += r.optDouble("amount", 0);
        }
        return total;
    }

    private double getOnlineTotal() {
        double total = 0;
        JSONArray list = entries();
        for (int i = 0; i < list.length(); i++) {
            JSONObject r = list.optJSONObject(i);
            if (r != null && "Online".equals(r.optString("type"))) total += r.optDouble("amount", 0);
        }
        return total;
    }

    private double getNoteTotal() {
        JSONObject notes = read(NOTES).optJSONObject(selectedDate);
        if (notes == null) return 0;
        double total = 0;
        for (int d : denominations) total += d * notes.optInt(String.valueOf(d), 0);
        return total;
    }

    private void updateReconciliation() {
        if (cashCheck == null || onlineCheck == null) return;
        double cash = getCashTotal(), counted = getNoteTotal();
        double diff = counted - cash;
        if (Math.abs(diff) < 0.005) cashCheck.setText("OFFLINE: Hisaab ₹" + money(cash) + "  •  Notes ₹" + money(counted) + "\n✓ Cash mil gaya: ₹0 difference");
        else if (diff < 0) cashCheck.setText("OFFLINE: Hisaab ₹" + money(cash) + "  •  Notes ₹" + money(counted) + "\n🔴 CASH KAMI: ₹" + money(-diff));
        else cashCheck.setText("OFFLINE: Hisaab ₹" + money(cash) + "  •  Notes ₹" + money(counted) + "\n🟢 EXTRA CASH: ₹" + money(diff));

        double online = getOnlineTotal();
        double actual = read(ACTUAL_ONLINE).optDouble(selectedDate, 0);
        double odiff = actual - online;
        if (Math.abs(odiff) < 0.005) onlineCheck.setText("ONLINE: App ₹" + money(online) + "  •  Actual UPI ₹" + money(actual) + "\n✓ Online mil gaya: ₹0 difference");
        else if (odiff < 0) onlineCheck.setText("ONLINE: App ₹" + money(online) + "  •  Actual UPI ₹" + money(actual) + "\n🔴 ONLINE KAMI: ₹" + money(-odiff));
        else onlineCheck.setText("ONLINE: App ₹" + money(online) + "  •  Actual UPI ₹" + money(actual) + "\n🟢 EXTRA ONLINE: ₹" + money(odiff));
    }

    private void saveToday() {
        try {
            JSONObject snapshot = new JSONObject();
            snapshot.put("cash", getCashTotal());
            snapshot.put("online", getOnlineTotal());
            snapshot.put("notes", getNoteTotal());
            snapshot.put("actualOnline", read(ACTUAL_ONLINE).optDouble(selectedDate, 0));
            snapshot.put("grandTotal", getCashTotal() + getOnlineTotal());
            snapshot.put("savedAt", new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date()));
            JSONObject all = read(DAILY_SAVED);
            all.put(selectedDate, snapshot);
            save(DAILY_SAVED, all);
            if (savedStatus != null) savedStatus.setText("✓ " + selectedDate + " ka hisaab save ho gaya");
            Toast.makeText(this, "Aaj ka hisaab save ho gaya", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save nahi hua, dobara try karein", Toast.LENGTH_SHORT).show();
        }
    }

    private void showHistory() {
        JSONObject all = read(DAILY_SAVED);
        if (all.length() == 0) {
            new AlertDialog.Builder(this).setTitle("Saved History")
                    .setMessage("Abhi koi saved day nahi hai. Pehle SAVE TODAY dabayein.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        ArrayList<String> dates = new ArrayList<>();
        JSONArray values = new JSONArray();
        Iterator<String> keys = all.keys();
        while (keys.hasNext()) dates.add(keys.next());
        Collections.sort(dates, Collections.reverseOrder());
        String[] items = dates.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("Saved Daily History")
                .setItems(items, (dialog, which) -> {
                    String date = items[which];
                    JSONObject r = all.optJSONObject(date);
                    if (r == null) return;
                    new AlertDialog.Builder(this).setTitle(date + " ka Hisaab")
                            .setMessage("Offline Cash: ₹" + money(r.optDouble("cash", 0))
                                    + "\nOnline App Total: ₹" + money(r.optDouble("online", 0))
                                    + "\nNotes Count: ₹" + money(r.optDouble("notes", 0))
                                    + "\nActual UPI: ₹" + money(r.optDouble("actualOnline", 0))
                                    + "\nGrand Total: ₹" + money(r.optDouble("grandTotal", 0))
                                    + "\nSaved at: " + r.optString("savedAt"))
                            .setPositiveButton("OK", null)
                            .setNegativeButton("DELETE HISTORY", (d, w) ->
                                    new AlertDialog.Builder(this)
                                            .setTitle("Delete " + date + "?")
                                            .setMessage("Kya aap is din ka saved history record delete karna chahte hain? Yeh undo nahi hoga.")
                                            .setNegativeButton("CANCEL", null)
                                            .setPositiveButton("DELETE", (confirm, cw) -> {
                                                JSONObject latest = read(DAILY_SAVED);
                                                latest.remove(date);
                                                save(DAILY_SAVED, latest);
                                                if (savedStatus != null && date.equals(selectedDate)) {
                                                    savedStatus.setText("Aaj ka hisaab abhi save nahi hua");
                                                }
                                                Toast.makeText(this, date + " ki history delete ho gayi", Toast.LENGTH_SHORT).show();
                                                showHistory();
                                            }).show()
                            ).show();
                }).setNegativeButton("Close", null).show();
    }
}
