package com.allinone.calc;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CurrencyScreen implements Screen {

    private static final String API = "https://open.er-api.com/v6/latest/USD";
    private static final String PREFS = "currency";
    private static final String KEY_RATES = "rates";
    private static final String KEY_TIME = "updated";

    private static final String[] CODES = {
            "USD", "EUR", "GBP", "BDT", "INR", "JPY", "CNY", "AUD", "CAD", "SGD",
            "AED", "SAR", "MYR", "PKR", "NPR", "LKR", "TRY", "KRW", "THB", "IDR",
            "PHP", "VND", "EGP", "ZAR", "CHF", "BRL", "MXN", "RUB"
    };

    private static final String[] NAMES = {
            "US Dollar", "Euro", "British Pound", "Bangladeshi Taka", "Indian Rupee",
            "Japanese Yen", "Chinese Yuan", "Australian Dollar", "Canadian Dollar",
            "Singapore Dollar", "UAE Dirham", "Saudi Riyal", "Malaysian Ringgit",
            "Pakistani Rupee", "Nepalese Rupee", "Sri Lankan Rupee", "Turkish Lira",
            "South Korean Won", "Thai Baht", "Indonesian Rupiah", "Philippine Peso",
            "Vietnamese Dong", "Egyptian Pound", "South African Rand",
            "Swiss Franc", "Brazilian Real", "Mexican Peso", "Russian Ruble"
    };

    /** Offline reference values (1 USD = x). Replaced by live rates when online. */
    private static final double[] FALLBACK = {
            1, 0.92, 0.79, 120, 84, 155, 7.2, 1.52, 1.37, 1.34,
            3.67, 3.75, 4.4, 278, 134, 300, 34, 1380, 35, 16000,
            58, 25400, 49, 18, 0.88, 5.7, 19.5, 95
    };

    private final Context ctx;
    private final View root;
    private final Spinner fromSpinner;
    private final Spinner toSpinner;
    private final EditText amount;
    private final TextView result;
    private final TextView resultUnit;
    private final TextView rateLine;
    private final TextView status;
    private final TextView refreshButton;

    private final double[] rates = new double[CODES.length];
    private boolean live = false;
    private boolean loading = false;
    private long updatedAt = 0;

    public CurrencyScreen(Context c) {
        this.ctx = c;
        loadCache();

        LinearLayout col = Ui.column(c, 0);
        int p = Ui.dp(c, 14);
        col.setPadding(p, Ui.dp(c, 8), p, p);

        LinearLayout card = Ui.card(c, "Currency Converter");

        LinearLayout fromTo = Ui.row(c);
        fromTo.setGravity(Gravity.CENTER_VERTICAL);
        fromSpinner = Ui.spinner(c, labels());
        toSpinner = Ui.spinner(c, labels());
        fromSpinner.setSelection(indexOf("USD"));
        toSpinner.setSelection(indexOf("BDT"));

        fromTo.addView(fromSpinner, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView swap = Ui.key(c, "⇄", Ui.OP_BG, Ui.ACCENT, 18);
        LinearLayout.LayoutParams swapParams = new LinearLayout.LayoutParams(Ui.dp(c, 46), Ui.dp(c, 46));
        swapParams.setMargins(Ui.dp(c, 8), 0, Ui.dp(c, 8), 0);
        fromTo.addView(swap, swapParams);
        fromTo.addView(toSpinner, new LinearLayout.LayoutParams(0, -2, 1f));
        card.addView(fromTo, new LinearLayout.LayoutParams(-1, -2));

        amount = Ui.field(c, "Amount");
        amount.setText("100");
        card.addView(amount, Ui.lp(-1, -2, 0, 12, 0, 0, c));

        LinearLayout panel = Ui.column(c, 0);
        panel.setBackground(Ui.roundBg(Ui.SURFACE2, Ui.dp(c, 16)));
        int pp = Ui.dp(c, 14);
        panel.setPadding(pp, pp, pp, pp);
        panel.setGravity(Gravity.END);
        result = Ui.text(c, "—", 30, Ui.ACCENT, true);
        result.setGravity(Gravity.END);
        resultUnit = Ui.text(c, "", 13, Ui.MUTED, false);
        resultUnit.setGravity(Gravity.END);
        panel.addView(result, new LinearLayout.LayoutParams(-1, -2));
        panel.addView(resultUnit, new LinearLayout.LayoutParams(-1, -2));
        card.addView(panel, Ui.lp(-1, -2, 0, 12, 0, 0, c));

        rateLine = Ui.text(c, "", 12, Ui.MUTED, false);
        rateLine.setPadding(0, Ui.dp(c, 8), 0, 0);
        card.addView(rateLine);

        LinearLayout footer = Ui.row(c);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(0, Ui.dp(c, 10), 0, 0);
        status = Ui.text(c, "", 11, Ui.MUTED, false);
        status.setSingleLine(true);
        footer.addView(status, new LinearLayout.LayoutParams(0, -2, 1f));

        refreshButton = Ui.key(c, "Refresh", Ui.ACCENT, Ui.ACCENT_D, 13);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(c, 96), Ui.dp(c, 36));
        footer.addView(refreshButton, bp);
        card.addView(footer);

        col.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView note = Ui.text(c,
                "Live rates from open.er-api.com. Rates are cached on the device, so the converter still works offline.",
                12, Ui.MUTED, false);
        note.setPadding(Ui.dp(c, 4), Ui.dp(c, 10), Ui.dp(c, 4), 0);
        col.addView(note);

        root = Ui.scroll(c, col);

        AdapterView.OnItemSelectedListener recalc = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                compute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        fromSpinner.setOnItemSelectedListener(recalc);
        toSpinner.setOnItemSelectedListener(recalc);

        Ui.watch(amount, new Runnable() {
            @Override
            public void run() {
                compute();
            }
        });

        swap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int f = fromSpinner.getSelectedItemPosition();
                int t = toSpinner.getSelectedItemPosition();
                fromSpinner.setSelection(t);
                toSpinner.setSelection(f);
                compute();
            }
        });

        refreshButton.setClickable(true);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refresh();
            }
        });

        showStatus();
        compute();
    }

    @Override
    public View view() {
        return root;
    }

    private String[] labels() {
        String[] out = new String[CODES.length];
        for (int i = 0; i < CODES.length; i++) {
            out[i] = CODES[i] + " · " + NAMES[i];
        }
        return out;
    }

    private int indexOf(String code) {
        for (int i = 0; i < CODES.length; i++) {
            if (CODES[i].equals(code)) {
                return i;
            }
        }
        return 0;
    }

    private void loadCache() {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String raw = p.getString(KEY_RATES, null);
        updatedAt = p.getLong(KEY_TIME, 0);

        boolean ok = false;
        if (raw != null) {
            try {
                JSONObject o = new JSONObject(raw);
                for (int i = 0; i < CODES.length; i++) {
                    if (o.has(CODES[i])) {
                        rates[i] = o.getDouble(CODES[i]);
                    }
                }
                ok = true;
            } catch (Exception ignored) {
            }
        }
        if (!ok) {
            System.arraycopy(FALLBACK, 0, rates, 0, FALLBACK.length);
            live = false;
            updatedAt = 0;
        } else {
            live = true;
        }
    }

    private void showStatus() {
        if (loading) {
            status.setText("Refreshing…");
            return;
        }
        if (live && updatedAt > 0) {
            String t = new SimpleDateFormat("d MMM, HH:mm", Locale.US).format(new Date(updatedAt));
            status.setText("Updated " + t);
            status.setTextColor(Ui.MUTED);
        } else {
            status.setText("Offline reference rates");
            status.setTextColor(Ui.WARN);
        }
    }

    private void compute() {
        int f = fromSpinner.getSelectedItemPosition();
        int t = toSpinner.getSelectedItemPosition();
        double v = Ui.parse(amount.getText().toString());
        if (f < 0 || t < 0) {
            return;
        }
        if (!Ui.valid(v)) {
            result.setText("—");
            resultUnit.setText("enter an amount");
            rateLine.setText("");
            return;
        }
        double inUsd = v / rates[f];
        double converted = inUsd * rates[t];
        result.setText(Ui.money(converted));
        resultUnit.setText(CODES[t] + " · " + NAMES[t]);
        double one = rates[t] / rates[f];
        rateLine.setText("1 " + CODES[f] + " = " + Ui.fmt(one) + " " + CODES[t]
                + (live ? "" : "  (approx)"));
    }

    private void refresh() {
        if (loading) {
            return;
        }
        loading = true;
        status.setText("Refreshing…");
        status.setTextColor(Ui.MUTED);
        refreshButton.setText("…");

        new Thread(new Runnable() {
            @Override
            public void run() {
                final double[] fetched = new double[CODES.length];
                String err = null;
                long stamp = System.currentTimeMillis();
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(API).openConnection();
                    conn.setConnectTimeout(12000);
                    conn.setReadTimeout(12000);
                    conn.setRequestProperty("Accept", "application/json");
                    int code = conn.getResponseCode();
                    if (code != 200) {
                        throw new IllegalStateException("HTTP " + code);
                    }
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    conn.disconnect();

                    JSONObject rootJson = new JSONObject(sb.toString());
                    if (!"success".equals(rootJson.optString("result"))) {
                        throw new IllegalStateException("API error");
                    }
                    JSONObject rs = rootJson.getJSONObject("rates");
                    for (int i = 0; i < CODES.length; i++) {
                        if (!rs.has(CODES[i])) {
                            throw new IllegalStateException("Missing " + CODES[i]);
                        }
                        fetched[i] = rs.getDouble(CODES[i]);
                    }
                    JSONObject store = new JSONObject();
                    for (int i = 0; i < CODES.length; i++) {
                        store.put(CODES[i], fetched[i]);
                    }
                    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putString(KEY_RATES, store.toString())
                            .putLong(KEY_TIME, stamp)
                            .apply();
                } catch (Exception e) {
                    err = e.getMessage() == null ? "network error" : e.getMessage();
                }
                final String error = err;

                root.post(new Runnable() {
                    @Override
                    public void run() {
                        loading = false;
                        refreshButton.setText("Refresh");
                        if (error == null) {
                            System.arraycopy(fetched, 0, rates, 0, fetched.length);
                            live = true;
                            updatedAt = System.currentTimeMillis();
                            compute();
                        } else {
                            status.setText("Couldn't refresh — using cached rates");
                            status.setTextColor(Ui.DANGER);
                            return;
                        }
                        showStatus();
                    }
                });
            }
        }).start();
    }
}
