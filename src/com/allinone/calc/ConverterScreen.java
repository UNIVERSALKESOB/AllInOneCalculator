package com.allinone.calc;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class ConverterScreen implements Screen {

    private static final String[] CATEGORIES = {
            "Length", "Weight", "Temperature", "Area", "Volume", "Speed", "Time", "Data"
    };

    private static final String[][] UNITS = {
            {"m · Meter", "km · Kilometer", "cm · Centimeter", "mm · Millimeter",
                    "mi · Mile", "yd · Yard", "ft · Foot", "in · Inch", "nmi · Nautical mile"},
            {"kg · Kilogram", "g · Gram", "mg · Milligram", "t · Tonne",
                    "lb · Pound", "oz · Ounce", "st · Stone"},
            {"°C · Celsius", "°F · Fahrenheit", "K · Kelvin"},
            {"m² · Square meter", "km² · Square kilometer", "cm² · Square centimeter",
                    "ha · Hectare", "ac · Acre", "ft² · Square foot", "in² · Square inch",
                    "yd² · Square yard"},
            {"L · Liter", "mL · Milliliter", "m³ · Cubic meter", "gal · Gallon (US)",
                    "qt · Quart (US)", "pt · Pint (US)", "cup · Cup (US)", "floz · Fluid ounce",
                    "tbsp · Tablespoon", "tsp · Teaspoon"},
            {"m/s · Meter/second", "km/h · Kilometer/hour", "mph · Mile/hour",
                    "kn · Knot", "ft/s · Foot/second"},
            {"s · Second", "min · Minute", "h · Hour", "d · Day",
                    "wk · Week", "mo · Month", "yr · Year"},
            {"B · Byte", "KB · Kilobyte", "MB · Megabyte", "GB · Gigabyte",
                    "TB · Terabyte", "bit · Bit"}
    };

    private static final double[][] FACTORS = {
            {1, 1000, 0.01, 0.001, 1609.344, 0.9144, 0.3048, 0.0254, 1852},
            {1, 0.001, 1e-6, 1000, 0.45359237, 0.028349523125, 6.35029318},
            {0, 0, 0},
            {1, 1e6, 1e-4, 1e4, 4046.8564224, 0.09290304, 0.00064516, 0.83612736},
            {1, 0.001, 1000, 3.785411784, 0.946352946, 0.473176473, 0.2365882365,
                    0.0295735295625, 0.01478676478125, 0.00492892159375},
            {1, 0.2777777777778, 0.44704, 0.5144444444444, 0.3048},
            {1, 60, 3600, 86400, 604800, 2629800, 31557600},
            {1, 1024, 1048576, 1073741824, 1099511627776.0, 0.125}
    };

    private final Context ctx;
    private final View root;
    private final Spinner catSpinner;
    private final Spinner fromSpinner;
    private final Spinner toSpinner;
    private final EditText input;
    private final TextView result;
    private final TextView resultUnit;
    private final TextView rate;

    private int category = 0;

    public ConverterScreen(Context c) {
        this.ctx = c;

        LinearLayout col = Ui.column(c, 0);
        int p = Ui.dp(c, 14);
        col.setPadding(p, Ui.dp(c, 8), p, p);

        LinearLayout catCard = Ui.card(c, "Category");
        catSpinner = Ui.spinner(c, CATEGORIES);
        catCard.addView(catSpinner, new LinearLayout.LayoutParams(-1, -2));
        col.addView(catCard, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout conv = Ui.card(c, "Convert");
        LinearLayout fromTo = Ui.row(c);
        fromTo.setGravity(Gravity.CENTER_VERTICAL);

        fromSpinner = Ui.spinner(c, UNITS[0]);
        toSpinner = Ui.spinner(c, UNITS[0]);
        fromSpinner.setSelection(0);
        toSpinner.setSelection(1);

        fromTo.addView(fromSpinner, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView swap = Ui.key(c, "⇄", Ui.OP_BG, Ui.ACCENT, 18);
        LinearLayout.LayoutParams swapParams = new LinearLayout.LayoutParams(Ui.dp(c, 46), Ui.dp(c, 46));
        swapParams.setMargins(Ui.dp(c, 8), 0, Ui.dp(c, 8), 0);
        fromTo.addView(swap, swapParams);
        fromTo.addView(toSpinner, new LinearLayout.LayoutParams(0, -2, 1f));
        conv.addView(fromTo, new LinearLayout.LayoutParams(-1, -2));

        input = Ui.field(c, "Enter value");
        input.setText("1");
        conv.addView(input, Ui.lp(-1, -2, 0, 12, 0, 0, c));

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
        conv.addView(panel, Ui.lp(-1, -2, 0, 12, 0, 0, c));

        rate = Ui.text(c, "", 12, Ui.MUTED, false);
        rate.setPadding(0, Ui.dp(c, 8), 0, 0);
        conv.addView(rate);

        col.addView(conv, Ui.lp(-1, -2, 0, 12, 0, 0, c));

        TextView note = Ui.text(c, "8 categories · 60+ units · temperature handled separately",
                12, Ui.MUTED, false);
        note.setPadding(Ui.dp(c, 4), Ui.dp(c, 8), Ui.dp(c, 4), 0);
        col.addView(note);

        root = Ui.scroll(c, col);

        catSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setCategory(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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

        Ui.watch(input, new Runnable() {
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

        compute();
    }

    @Override
    public View view() {
        return root;
    }

    private void setCategory(int index) {
        if (index < 0 || index >= UNITS.length || index == category) {
            return;
        }
        category = index;
        fromSpinner.setAdapter(Ui.adapter(ctx, UNITS[index]));
        toSpinner.setAdapter(Ui.adapter(ctx, UNITS[index]));
        fromSpinner.setSelection(0);
        toSpinner.setSelection(Math.min(1, UNITS[index].length - 1));
        compute();
    }

    private boolean isTemp() {
        return category == 2;
    }

    private static String code(String label) {
        int i = label.indexOf(" · ");
        return i > 0 ? label.substring(0, i) : label;
    }

    private double toCelsius(double v, int unit) {
        switch (unit) {
            case 0: return v;
            case 1: return (v - 32.0) * 5.0 / 9.0;
            default: return v - 273.15;
        }
    }

    private double fromCelsius(double c, int unit) {
        switch (unit) {
            case 0: return c;
            case 1: return c * 9.0 / 5.0 + 32.0;
            default: return c + 273.15;
        }
    }

    private void compute() {
        double v = Ui.parse(input.getText().toString());
        int f = fromSpinner.getSelectedItemPosition();
        int t = toSpinner.getSelectedItemPosition();
        if (f < 0 || t < 0) {
            return;
        }
        if (!Ui.valid(v)) {
            result.setText("—");
            resultUnit.setText("enter a number");
            rate.setText("");
            return;
        }
        double res;
        double one;
        if (isTemp()) {
            res = fromCelsius(toCelsius(v, f), t);
            one = fromCelsius(toCelsius(1, f), t);
        } else {
            res = v * FACTORS[category][f] / FACTORS[category][t];
            one = FACTORS[category][f] / FACTORS[category][t];
        }
        result.setText(Ui.fmt(res));
        resultUnit.setText(UNITS[category][t]);
        rate.setText("1 " + code(UNITS[category][f]) + " = " + Ui.fmt(one) + " " + code(UNITS[category][t]));
    }
}
