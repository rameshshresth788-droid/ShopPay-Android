package com.ramesh.shoppay;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.*;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import org.json.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ShopPay - 2100 edition UI.
 * Hisaab ka logic pehle jaisa hai; sirf look, animation, fonts, colors naye hain.
 */
public class MainActivity extends Activity {
    private final String PREF = "ShopPayNative";
    private final String DATA = "payments";
    private final String NOTES = "notes";
    private final String ACTUAL_ONLINE = "actual_online";
    private final String DAILY_SAVED = "daily_saved";

    // ---- 2100 palette ----
    private static final int INK = 0xFF070817;      // deep space background
    private static final int VIOLET = 0xFF8B5CFF;   // ultraviolet
    private static final int PINK = 0xFFFF4FD8;     // plasma pink
    private static final int CYAN = 0xFF2EF2FF;     // ion cyan
    private static final int BLUE = 0xFF3B82FF;
    private static final int AMBER = 0xFFFFB020;    // cash
    private static final int EMBER = 0xFFFF6B3D;
    private static final int MINT = 0xFF3DF5B8;     // matched / saved
    private static final int ROSE = 0xFFFF5C7A;     // shortage
    private static final int TEXT = 0xFFF2F4FF;
    private static final int MUTED = 0xFF8E94C9;

    private String selectedDate = today();
    private LinearLayout root, cashRecordsBox, onlineRecordsBox, notesBox;
    private TextView dateText, cashTotal, onlineTotal, grandTotal, noteTotal,
            cashCheck, onlineCheck, savedStatus, title, tagline;
    private EditText actualOnlineInput;
    private ImageView logoImg;
    private FrameLayout logoBox, orbit;
    private View halo;
    private AuroraView aurora;
    private float density = 1f;
    private Typeface display, body, numeric;
    private final int[] denominations = {500, 200, 100, 50, 20, 10, 5, 2, 1};

    private final ArrayList<Animator> loops = new ArrayList<>();
    private final HashMap<TextView, Double> lastValues = new HashMap<>();
    private final HashMap<TextView, ValueAnimator> counters = new HashMap<>();

    // ------------------------------------------------------------ helpers
    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private int dp(int v) {
        return Math.round(v * density);
    }

    private boolean motionOn() {
        try {
            return Settings.Global.getFloat(getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (Exception e) {
            return true;
        }
    }

    private void loop(Animator a) {
        loops.add(a);
        a.start();
    }

    private void loadFonts() {
        // Custom font chahiye? .ttf file ko app/src/main/assets/fonts/ mein rakhein:
        //   display.ttf (titles/buttons)  body.ttf (normal text)
        Typeface d = null, b = null;
        try { d = Typeface.createFromAsset(getAssets(), "fonts/display.ttf"); } catch (Exception ignored) {}
        try { b = Typeface.createFromAsset(getAssets(), "fonts/body.ttf"); } catch (Exception ignored) {}
        display = d != null ? d : Typeface.create("sans-serif-black", Typeface.NORMAL);
        body = b != null ? b : Typeface.create("sans-serif-medium", Typeface.NORMAL);
        numeric = Typeface.create("monospace", Typeface.BOLD);
    }

    // ------------------------------------------------------------ data
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

    // ------------------------------------------------------------ look
    /** Neon-edge glass: gradient border + translucent dark fill. */
    private LayerDrawable glass(int fill, int radiusDp) {
        GradientDrawable edge = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{0x662EF2FF, 0x668B5CFF, 0x66FF4FD8});
        edge.setCornerRadius(dp(radiusDp));
        GradientDrawable inner = new GradientDrawable();
        inner.setColor(fill);
        inner.setCornerRadius(Math.max(0, dp(radiusDp) - Math.max(1, dp(1))));
        LayerDrawable layers = new LayerDrawable(new Drawable[]{edge, inner});
        int in = Math.max(1, dp(1));
        layers.setLayerInset(1, in, in, in, in);
        return layers;
    }

    private GradientDrawable gradient(int first, int second, int radiusDp) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{first, second});
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(body);
        t.setFontFeatureSettings("kern");
        return t;
    }

    private void styleField(EditText e, int radiusDp) {
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setTypeface(body);
        e.setBackground(glass(0xFF151944, radiusDp));
    }

    private void animateIn(View view, int index) {
        if (!motionOn()) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(22));
        view.animate().alpha(1f).translationY(0f)
                .setStartDelay(Math.min(index * 55L, 440L))
                .setDuration(420L)
                .setInterpolator(new OvershootInterpolator(0.7f))
                .start();
    }

    private TextView button(String label, int c1, int c2, int textColor) {
        TextView b = text(label, 15, textColor);
        b.setTypeface(display);
        b.setLetterSpacing(0.06f);
        b.setBackground(gradient(c1, c2, 26));
        b.setPadding(dp(16), dp(14), dp(16), dp(14));
        b.setElevation(dp(5));
        if (Build.VERSION.SDK_INT >= 28) {
            b.setOutlineSpotShadowColor(c1);
            b.setOutlineAmbientShadowColor(c1);
        }
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnTouchListener((v, event) -> {
            int a = event.getAction();
            if (a == MotionEvent.ACTION_DOWN) {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                v.animate().scaleX(0.94f).scaleY(0.94f)
                        .setInterpolator(new DecelerateInterpolator())
                        .setDuration(90).start();
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f)
                        .setInterpolator(new OvershootInterpolator(3f))
                        .setDuration(260).start();
            }
            return false;
        });
        return b;
    }

    private TextView smallButton(String label, int c1, int c2) {
        TextView b = button(label, c1, c2, Color.WHITE);
        b.setTextSize(12);
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setElevation(dp(2));
        return b;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private void gap(LinearLayout box, int heightPx) {
        View v = new View(this);
        box.addView(v, new LinearLayout.LayoutParams(1, heightPx));
    }

    /** Glass card section. */
    private void section(LinearLayout box, View child) {
        child.setPadding(dp(16), dp(16), dp(16), dp(16));
        child.setBackground(glass(0xCC0F1230, 24));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(7), 0, dp(7));
        box.addView(child, p);
        animateIn(child, box.getChildCount());
    }

    /** Add a view without a card background (buttons etc). */
    private void place(LinearLayout box, View child, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(topDp), 0, dp(topDp));
        box.addView(child, p);
        animateIn(child, box.getChildCount());
    }

    private TextView sectionTitle(String label, int color) {
        TextView t = text(label, 15, color);
        t.setTypeface(display);
        t.setLetterSpacing(0.14f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(16), 0, dp(2));
        root.addView(t, p);
        return t;
    }

    private void pulse(final View v) {
        if (!motionOn()) return;
        v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(140)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f)
                        .setInterpolator(new OvershootInterpolator(3f)).setDuration(260).start())
                .start();
    }

    // ------------------------------------------------------------ text animations
    private CharSequence hidden(String full, int shown) {
        SpannableString s = new SpannableString(full);
        if (shown < full.length()) {
            s.setSpan(new ForegroundColorSpan(0x00000000), shown, full.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return s;
    }

    private void typewriter(final TextView tv, final String full, long delay) {
        if (!motionOn()) {
            tv.setText(full);
            return;
        }
        tv.setText(hidden(full, 0));
        ValueAnimator a = ValueAnimator.ofInt(0, full.length());
        a.setStartDelay(delay);
        a.setDuration(full.length() * 38L);
        a.setInterpolator(new LinearInterpolator());
        a.addUpdateListener(x -> tv.setText(hidden(full, (Integer) x.getAnimatedValue())));
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tv.setText(full);
            }
        });
        a.start();
    }

    private void startShimmer(final TextView t) {
        final float w = Math.max(1f, t.getWidth());
        final LinearGradient g = new LinearGradient(0, 0, w, 0,
                new int[]{CYAN, VIOLET, PINK}, null, Shader.TileMode.MIRROR);
        t.getPaint().setShader(g);
        t.invalidate();
        if (!motionOn()) return;
        final Matrix m = new Matrix();
        ValueAnimator a = ValueAnimator.ofFloat(0f, 2f * w);
        a.setDuration(4200);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setInterpolator(new LinearInterpolator());
        a.addUpdateListener(x -> {
            m.setTranslate((Float) x.getAnimatedValue(), 0f);
            g.setLocalMatrix(m);
            t.invalidate();
        });
        loop(a);
    }

    /** Numbers roll up/down to the new value. */
    private void setMoney(final TextView tv, final String prefix, final double to) {
        Double last = lastValues.get(tv);
        final double from = last == null ? 0 : last;
        lastValues.put(tv, to);
        ValueAnimator old = counters.get(tv);
        if (old != null) old.cancel();
        if (!motionOn() || Math.abs(to - from) < 0.005) {
            tv.setText(prefix + money(to));
            return;
        }
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(520);
        a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(x ->
                tv.setText(prefix + money(from + (to - from) * x.getAnimatedFraction())));
        a.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tv.setText(prefix + money(to));
            }
        });
        counters.put(tv, a);
        a.start();
    }

    // ------------------------------------------------------------ living background
    private class AuroraView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] tint = {VIOLET, CYAN, PINK};
        private final RadialGradient[] blobs = new RadialGradient[3];
        private float radius;
        private float t = 0f;
        private final float[] sx = new float[16], sy = new float[16], sr = new float[16];
        private final int[] sk = new int[16];

        AuroraView(Context c) {
            super(c);
            Random rnd = new Random(7);
            for (int i = 0; i < sx.length; i++) {
                sx[i] = rnd.nextFloat();
                sy[i] = rnd.nextFloat();
                sr[i] = 0.6f + rnd.nextFloat() * 1.4f;
                sk[i] = 1 + rnd.nextInt(2);
            }
        }

        ValueAnimator animator() {
            ValueAnimator a = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
            a.setDuration(26000);
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setInterpolator(new LinearInterpolator());
            a.addUpdateListener(x -> {
                t = (Float) x.getAnimatedValue();
                invalidate();
            });
            return a;
        }

        @Override
        protected void onSizeChanged(int w, int h, int ow, int oh) {
            radius = w * 1.05f;
            for (int i = 0; i < 3; i++) {
                int c = tint[i] & 0x00FFFFFF;
                blobs[i] = new RadialGradient(0, 0, radius,
                        new int[]{c | 0x66000000, c | 0x22000000, c},
                        new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w = getWidth(), h = getHeight();
            canvas.drawColor(INK);
            if (blobs[0] == null) return;
            float[] cx = {
                    w * (0.5f + 0.45f * (float) Math.cos(t)),
                    w * (0.5f + 0.40f * (float) Math.cos(t + 2.1f)),
                    w * (0.5f + 0.45f * (float) Math.sin(t + 4.2f))};
            float[] cy = {
                    h * (0.12f + 0.05f * (float) Math.sin(2 * t)),
                    h * (0.50f + 0.10f * (float) Math.sin(t + 1f)),
                    h * (0.88f + 0.05f * (float) Math.cos(2 * t))};
            for (int i = 0; i < 3; i++) {
                canvas.save();
                canvas.translate(cx[i], cy[i]);
                paint.setShader(blobs[i]);
                canvas.drawCircle(0, 0, radius, paint);
                canvas.restore();
            }
            paint.setShader(null);
            float turns = t / (float) (Math.PI * 2);
            for (int i = 0; i < sx.length; i++) {
                float prog = sy[i] - sk[i] * turns;
                prog = prog - (float) Math.floor(prog);
                float y = prog * h;
                float x = w * sx[i] + dp(6) * (float) Math.sin(t * 2 + i);
                float tw = 0.5f + 0.5f * (float) Math.sin(t * 3 + i * 1.7f);
                int base = (i % 2 == 0) ? 0x00FFFFFF : 0x002EF2FF;
                paint.setColor(base | (((int) (40 + 120 * tw)) << 24));
                canvas.drawCircle(x, y, dp(1) * sr[i] + 0.5f, paint);
            }
        }
    }

    // ------------------------------------------------------------ lifecycle
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        density = getResources().getDisplayMetrics().density;
        loadFonts();
        getWindow().setStatusBarColor(INK);
        getWindow().setNavigationBarColor(INK);
        if (Build.VERSION.SDK_INT >= 29) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }
        buildScreen();
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (Animator a : loops) a.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (Animator a : loops) a.resume();
    }

    @Override
    protected void onDestroy() {
        for (Animator a : loops) a.cancel();
        loops.clear();
        super.onDestroy();
    }

    // ------------------------------------------------------------ screen
    private void buildScreen() {
        final FrameLayout frame = new FrameLayout(this);
        aurora = new AuroraView(this);
        frame.addView(aurora, new FrameLayout.LayoutParams(-1, -1));

        final ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setVerticalScrollBarEnabled(false);
        frame.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        root = column();
        root.setPadding(dp(18), dp(22), dp(18), dp(32));
        scroll.addView(root);
        setContentView(frame);

        frame.setOnApplyWindowInsetsListener((v, insets) -> {
            scroll.setPadding(0, insets.getSystemWindowInsetTop(),
                    0, insets.getSystemWindowInsetBottom());
            return insets;
        });

        // ---- floating anime logo with halo + orbiting sparks
        logoBox = new FrameLayout(this);
        halo = new View(this);
        GradientDrawable haloShape = new GradientDrawable();
        haloShape.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        haloShape.setGradientRadius(dp(66));
        haloShape.setColors(new int[]{0x998B5CFF, 0x002EF2FF});
        halo.setBackground(haloShape);
        logoBox.addView(halo, new FrameLayout.LayoutParams(dp(132), dp(132), Gravity.CENTER));

        orbit = new FrameLayout(this);
        View dotA = new View(this);
        GradientDrawable da = new GradientDrawable();
        da.setShape(GradientDrawable.OVAL);
        da.setColor(CYAN);
        dotA.setBackground(da);
        orbit.addView(dotA, new FrameLayout.LayoutParams(dp(8), dp(8), Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        View dotB = new View(this);
        GradientDrawable db = new GradientDrawable();
        db.setShape(GradientDrawable.OVAL);
        db.setColor(PINK);
        dotB.setBackground(db);
        orbit.addView(dotB, new FrameLayout.LayoutParams(dp(6), dp(6), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL));
        logoBox.addView(orbit, new FrameLayout.LayoutParams(dp(120), dp(120), Gravity.CENTER));

        logoImg = new ImageView(this);
        logoImg.setImageResource(R.drawable.logo_anime);
        logoImg.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logoBox.addView(logoImg, new FrameLayout.LayoutParams(dp(90), dp(90), Gravity.CENTER));

        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(132), dp(132));
        logoLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(logoBox, logoLp);

        title = text("SHOPPAY", 30, Color.WHITE);
        title.setTypeface(display);
        title.setLetterSpacing(0.18f);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-2, -2);
        titleLp.gravity = Gravity.CENTER_HORIZONTAL;
        root.addView(title, titleLp);

        tagline = text("SMART DAILY HISSAAB  •  2100 EDITION", 11, MUTED);
        tagline.setLetterSpacing(0.12f);
        root.addView(tagline);
        gap(root, dp(14));

        // ---- date
        LinearLayout datePanel = column();
        dateText = text("", 16, TEXT);
        dateText.setTypeface(display);
        dateText.setPadding(dp(8), dp(6), dp(8), dp(6));
        dateText.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            try {
                c.setTime(new SimpleDateFormat("yyyy-MM-dd",
                        Locale.US).parse(selectedDate));
            } catch (Exception ignored) {}
            new DatePickerDialog(this, R.style.ShopPayDatePicker, (view, year, month, day) -> {
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

        // ---- add buttons
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        TextView cashBtn = button("+ Add Cash", AMBER, EMBER, 0xFF1A1000);
        cashBtn.setOnClickListener(v -> addPayment("Cash"));
        TextView onlineBtn = button("+ Add Online", CYAN, BLUE, 0xFF04101F);
        onlineBtn.setOnClickListener(v -> addPayment("Online"));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, -2, 1);
        half.setMargins(dp(4), dp(4), dp(4), dp(4));
        buttons.addView(cashBtn, half);
        buttons.addView(onlineBtn, half);
        place(root, buttons, 6);

        // ---- totals
        cashTotal = text("", 18, AMBER);
        cashTotal.setTypeface(numeric);
        cashTotal.setPadding(dp(8), dp(8), dp(8), dp(8));
        onlineTotal = text("", 18, CYAN);
        onlineTotal.setTypeface(numeric);
        onlineTotal.setPadding(dp(8), dp(8), dp(8), dp(8));
        grandTotal = text("", 22, Color.WHITE);
        grandTotal.setTypeface(numeric);
        grandTotal.setPadding(dp(8), dp(14), dp(8), dp(14));
        grandTotal.setBackground(gradient(0xAA6A4BFF, 0xAA1FB5D4, 18));

        LinearLayout totals = column();
        totals.addView(cashTotal);
        totals.addView(onlineTotal);
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(-1, -2);
        gl.setMargins(0, dp(6), 0, 0);
        totals.addView(grandTotal, gl);
        section(root, totals);

        sectionTitle("●  OFFLINE CASH RECORDS", AMBER);
        cashRecordsBox = column();
        section(root, cashRecordsBox);

        sectionTitle("●  ONLINE PAYMENT RECORDS", CYAN);
        onlineRecordsBox = column();
        section(root, onlineRecordsBox);

        sectionTitle("●  NOTE CALCULATOR", TEXT);
        notesBox = column();
        section(root, notesBox);

        noteTotal = text("Notes Total: ₹0", 20, AMBER);
        noteTotal.setTypeface(numeric);
        section(root, noteTotal);

        sectionTitle("●  HISAAB MILAN", 0xFFB8A5FF);

        cashCheck = text("Cash difference: ₹0", 16, TEXT);
        section(root, cashCheck);

        LinearLayout onlineCheckPanel = column();
        onlineCheckPanel.addView(text("Actual UPI / Online total", 15, TEXT));
        actualOnlineInput = new EditText(this);
        actualOnlineInput.setSingleLine(true);
        actualOnlineInput.setHint("Bank/UPI app ka total (₹)");
        actualOnlineInput.setInputType(8194);
        styleField(actualOnlineInput, 16);
        actualOnlineInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams fieldLp = new LinearLayout.LayoutParams(-1, -2);
        fieldLp.setMargins(0, dp(8), 0, 0);
        onlineCheckPanel.addView(actualOnlineInput, fieldLp);
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
        onlineCheck = text("Online difference: ₹0", 16, TEXT);
        section(root, onlineCheck);

        TextView saveToday = button("✓  SAVE TODAY", VIOLET, PINK, Color.WHITE);
        saveToday.setOnClickListener(v -> saveToday());
        place(root, saveToday, 8);
        TextView history = button("▣  VIEW SAVED HISTORY", 0xFF2A2F66, 0xFF1B1F4B, TEXT);
        history.setOnClickListener(v -> showHistory());
        place(root, history, 4);

        savedStatus = text("", 13, MUTED);
        root.addView(savedStatus);
        TextView foot = text("Data phone mein save hota hai.", 12, MUTED);
        LinearLayout.LayoutParams footLp = new LinearLayout.LayoutParams(-1, -2);
        footLp.setMargins(0, dp(6), 0, 0);
        root.addView(foot, footLp);

        refresh();
        startIntro();
    }

    private void startIntro() {
        final boolean motion = motionOn();

        if (motion) {
            loop(aurora.animator());

            // logo pops in, then floats forever
            logoImg.setScaleX(0f);
            logoImg.setScaleY(0f);
            logoImg.animate().scaleX(1f).scaleY(1f).setStartDelay(150).setDuration(800)
                    .setInterpolator(new OvershootInterpolator(2f)).start();

            ObjectAnimator floatY = ObjectAnimator.ofFloat(logoBox, View.TRANSLATION_Y, -dp(6), dp(6));
            floatY.setDuration(2600);
            floatY.setRepeatCount(ValueAnimator.INFINITE);
            floatY.setRepeatMode(ValueAnimator.REVERSE);
            floatY.setInterpolator(new AccelerateDecelerateInterpolator());
            loop(floatY);

            ObjectAnimator tilt = ObjectAnimator.ofFloat(logoImg, View.ROTATION, -3f, 3f);
            tilt.setDuration(3400);
            tilt.setRepeatCount(ValueAnimator.INFINITE);
            tilt.setRepeatMode(ValueAnimator.REVERSE);
            tilt.setInterpolator(new AccelerateDecelerateInterpolator());
            loop(tilt);

            ObjectAnimator haloX = ObjectAnimator.ofFloat(halo, View.SCALE_X, 0.9f, 1.12f);
            ObjectAnimator haloY = ObjectAnimator.ofFloat(halo, View.SCALE_Y, 0.9f, 1.12f);
            ObjectAnimator haloA = ObjectAnimator.ofFloat(halo, View.ALPHA, 0.65f, 1f);
            for (ObjectAnimator a : new ObjectAnimator[]{haloX, haloY, haloA}) {
                a.setDuration(2000);
                a.setRepeatCount(ValueAnimator.INFINITE);
                a.setRepeatMode(ValueAnimator.REVERSE);
                a.setInterpolator(new AccelerateDecelerateInterpolator());
                loop(a);
            }

            ObjectAnimator spin = ObjectAnimator.ofFloat(orbit, View.ROTATION, 0f, 360f);
            spin.setDuration(8000);
            spin.setRepeatCount(ValueAnimator.INFINITE);
            spin.setInterpolator(new LinearInterpolator());
            loop(spin);

            // grand total gently floats
            ObjectAnimator totalFloat = ObjectAnimator.ofFloat(grandTotal, View.TRANSLATION_Y, -dp(2), dp(2));
            totalFloat.setDuration(3000);
            totalFloat.setRepeatCount(ValueAnimator.INFINITE);
            totalFloat.setRepeatMode(ValueAnimator.REVERSE);
            totalFloat.setInterpolator(new AccelerateDecelerateInterpolator());
            loop(totalFloat);

            // title: letters glide together, then neon shimmer starts
            title.setAlpha(0f);
            ValueAnimator spacing = ValueAnimator.ofFloat(0.5f, 0.18f);
            spacing.setStartDelay(250);
            spacing.setDuration(1100);
            spacing.setInterpolator(new DecelerateInterpolator(1.6f));
            spacing.addUpdateListener(x -> {
                title.setLetterSpacing((Float) x.getAnimatedValue());
                title.setAlpha(Math.min(1f, x.getAnimatedFraction() * 2f));
            });
            spacing.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    title.setAlpha(1f);
                    title.setLetterSpacing(0.18f);
                    title.post(() -> startShimmer(title));
                }
            });
            spacing.start();

            typewriter(tagline, "SMART DAILY HISSAAB  •  2100 EDITION", 900);
        } else {
            title.post(() -> startShimmer(title));
        }
    }

    // ------------------------------------------------------------ actions
    private AlertDialog.Builder dlg() {
        return new AlertDialog.Builder(this, R.style.ShopPayDialog);
    }

    private void addPayment(String type) {
        LinearLayout form = column();
        form.setPadding(dp(20), dp(8), dp(20), dp(4));

        EditText amount = new EditText(this);
        amount.setHint("Amount (₹)");
        amount.setInputType(8194);
        amount.setPadding(dp(16), dp(12), dp(16), dp(12));
        styleField(amount, 16);
        form.addView(amount);

        Spinner gender = new Spinner(this);
        String[] options = {"Male", "Female"};
        gender.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, options));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, dp(8), 0, 0);
        form.addView(gender, sp);

        dlg()
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
                        pulse(grandTotal);

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
            boolean isCash = "Cash".equals(row.optString("type"));
            if (isCash) cash += amount;
            else online += amount;

            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(dp(14), dp(10), dp(10), dp(10));
            GradientDrawable chip = new GradientDrawable();
            chip.setColor(isCash ? 0x1AFFB020 : 0x1A2EF2FF);
            chip.setCornerRadius(dp(16));
            chip.setStroke(Math.max(1, dp(1)), isCash ? 0x44FFB020 : 0x442EF2FF);
            line.setBackground(chip);

            String first = row.optString("type") + " ₹" + money(amount);
            String second = row.optString("gender") + " • " + row.optString("time");
            SpannableString label = new SpannableString(first + "\n" + second);
            label.setSpan(new StyleSpan(Typeface.BOLD), 0, first.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            label.setSpan(new RelativeSizeSpan(1.18f), 0, first.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            label.setSpan(new ForegroundColorSpan(MUTED), first.length() + 1, label.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            TextView info = text("", 14, TEXT);
            info.setText(label);
            info.setGravity(Gravity.START);
            line.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

            TextView del = smallButton("Delete", ROSE, 0xFFB5179E);
            del.setOnClickListener(v ->
                    dlg()
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
            LinearLayout targetBox = isCash ? cashRecordsBox : onlineRecordsBox;
            LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(-1, -2);
            lineLp.setMargins(0, 0, 0, dp(8));
            targetBox.addView(line, lineLp);
            animateIn(line, i);
        }

        setMoney(cashTotal, "Offline Cash: ₹", cash);
        setMoney(onlineTotal, "Online Payment: ₹", online);
        setMoney(grandTotal, "GRAND TOTAL: ₹", cash + online);

        JSONObject allNotes = read(NOTES);
        JSONObject dayNotes = allNotes.optJSONObject(selectedDate);
        if (dayNotes == null) dayNotes = new JSONObject();

        final JSONObject currentNotes = dayNotes;

        for (int denomination : denominations) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView label = text("₹" + denomination, 17, TEXT);
            label.setTypeface(numeric);
            label.setGravity(Gravity.START);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));

            EditText count = new EditText(this);
            count.setSingleLine(true);
            count.setHint("Count");
            count.setInputType(2);
            count.setGravity(Gravity.CENTER);
            styleField(count, 14);
            count.setPadding(dp(12), dp(8), dp(12), dp(8));
            count.setText(String.valueOf(currentNotes.optInt(
                    String.valueOf(denomination), 0)));
            row.addView(count, new LinearLayout.LayoutParams(dp(100), -2));

            TextView subtotal = text("", 14, AMBER);
            subtotal.setTypeface(numeric);
            subtotal.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            row.addView(subtotal, new LinearLayout.LayoutParams(dp(84), -2));
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

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
            rowLp.setMargins(0, 0, 0, dp(6));
            notesBox.addView(row, rowLp);
        }
        updateReconciliation();
        if (savedStatus != null) {
            boolean saved = read(DAILY_SAVED).has(selectedDate);
            savedStatus.setText(saved
                    ? "✓ " + selectedDate + " ka hisaab saved hai" : "Aaj ka hisaab abhi save nahi hua");
            savedStatus.setTextColor(saved ? MINT : MUTED);
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
        if (Math.abs(diff) < 0.005) {
            cashCheck.setText("OFFLINE: Hisaab ₹" + money(cash) + "  •  Notes ₹" + money(counted) + "\n✓ Cash mil gaya: ₹0 difference");
            cashCheck.setTextColor(MINT);
        } else if (diff < 0) {
            cashCheck.setText("OFFLINE: Hisaab ₹" + money(cash) + "  •  Notes ₹" + money(counted) + "\n🔴 CASH KAMI: ₹" + money(-diff));
            cashCheck.setTextColor(ROSE);
        } else {
            cashCheck.setText("OFFLINE: Hisaab ₹" + money(cash) + "  •  Notes ₹" + money(counted) + "\n🟢 EXTRA CASH: ₹" + money(diff));
            cashCheck.setTextColor(CYAN);
        }

        double online = getOnlineTotal();
        double actual = read(ACTUAL_ONLINE).optDouble(selectedDate, 0);
        double odiff = actual - online;
        if (Math.abs(odiff) < 0.005) {
            onlineCheck.setText("ONLINE: App ₹" + money(online) + "  •  Actual UPI ₹" + money(actual) + "\n✓ Online mil gaya: ₹0 difference");
            onlineCheck.setTextColor(MINT);
        } else if (odiff < 0) {
            onlineCheck.setText("ONLINE: App ₹" + money(online) + "  •  Actual UPI ₹" + money(actual) + "\n🔴 ONLINE KAMI: ₹" + money(-odiff));
            onlineCheck.setTextColor(ROSE);
        } else {
            onlineCheck.setText("ONLINE: App ₹" + money(online) + "  •  Actual UPI ₹" + money(actual) + "\n🟢 EXTRA ONLINE: ₹" + money(odiff));
            onlineCheck.setTextColor(CYAN);
        }
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
            if (savedStatus != null) {
                savedStatus.setText("✓ " + selectedDate + " ka hisaab save ho gaya");
                savedStatus.setTextColor(MINT);
                pulse(savedStatus);
            }
            Toast.makeText(this, "Aaj ka hisaab save ho gaya", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Save nahi hua, dobara try karein", Toast.LENGTH_SHORT).show();
        }
    }

    private void showHistory() {
        JSONObject all = read(DAILY_SAVED);
        if (all.length() == 0) {
            dlg().setTitle("Saved History")
                    .setMessage("Abhi koi saved day nahi hai. Pehle SAVE TODAY dabayein.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        ArrayList<String> dates = new ArrayList<>();
        Iterator<String> keys = all.keys();
        while (keys.hasNext()) dates.add(keys.next());
        Collections.sort(dates, Collections.reverseOrder());
        String[] items = dates.toArray(new String[0]);
        dlg().setTitle("Saved Daily History")
                .setItems(items, (dialog, which) -> {
                    String date = items[which];
                    JSONObject r = all.optJSONObject(date);
                    if (r == null) return;
                    dlg().setTitle(date + " ka Hisaab")
                            .setMessage("Offline Cash: ₹" + money(r.optDouble("cash", 0))
                                    + "\nOnline App Total: ₹" + money(r.optDouble("online", 0))
                                    + "\nNotes Count: ₹" + money(r.optDouble("notes", 0))
                                    + "\nActual UPI: ₹" + money(r.optDouble("actualOnline", 0))
                                    + "\nGrand Total: ₹" + money(r.optDouble("grandTotal", 0))
                                    + "\nSaved at: " + r.optString("savedAt"))
                            .setPositiveButton("OK", null)
                            .setNegativeButton("DELETE HISTORY", (d, w) ->
                                    dlg()
                                            .setTitle("Delete " + date + "?")
                                            .setMessage("Kya aap is din ka saved history record delete karna chahte hain? Yeh undo nahi hoga.")
                                            .setNegativeButton("CANCEL", null)
                                            .setPositiveButton("DELETE", (confirm, cw) -> {
                                                JSONObject latest = read(DAILY_SAVED);
                                                latest.remove(date);
                                                save(DAILY_SAVED, latest);
                                                if (savedStatus != null && date.equals(selectedDate)) {
                                                    savedStatus.setText("Aaj ka hisaab abhi save nahi hua");
                                                    savedStatus.setTextColor(MUTED);
                                                }
                                                Toast.makeText(this, date + " ki history delete ho gayi", Toast.LENGTH_SHORT).show();
                                                showHistory();
                                            }).show()
                            ).show();
                }).setNegativeButton("Close", null).show();
    }
}
