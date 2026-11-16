package com.allinone.calc;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PercentScreen implements Screen {

    private final View root;

    public PercentScreen(Context c) {
        LinearLayout col = Ui.column(c, 0);
        int p = Ui.dp(c, 14);
        col.setPadding(p, Ui.dp(c, 8), p, p);

        // 1. X% of Y
        LinearLayout a = Ui.card(c, "What is X% of Y?");
        EditText a1 = Ui.addField(c, a, "X — percentage", "e.g. 15", "15");
        EditText a2 = Ui.addField(c, a, "Y — number", "e.g. 2500", "2500");
        TextView aOut = result(c, a);
        col.addView(a, new LinearLayout.LayoutParams(-1, -2));

        // 2. X is what % of Y
        LinearLayout b = Ui.card(c, "X is what percent of Y?");
        EditText b1 = Ui.addField(c, b, "X", "e.g. 45", "45");
        EditText b2 = Ui.addField(c, b, "Y", "e.g. 180", "180");
        TextView bOut = result(c, b);
        col.addView(b, Ui.lp(-1, -2, 0, 12, 0, 0, c));

        // 3. Percentage change
        LinearLayout d = Ui.card(c, "Percentage increase / decrease");
        EditText d1 = Ui.addField(c, d, "From (old value)", "e.g. 200", "200");
        EditText d2 = Ui.addField(c, d, "To (new value)", "e.g. 260", "260");
        TextView dOut = result(c, d);
        col.addView(d, Ui.lp(-1, -2, 0, 12, 0, 0, c));

        root = Ui.scroll(c, col);

        bind(a1, a2, aOut, new Calc() {
            public String run(double x, double y) {
                return Ui.fmt(x * y / 100.0);
            }
        });
        bind(b1, b2, bOut, new Calc() {
            public String run(double x, double y) {
                if (y == 0) {
                    return "—";
                }
                return Ui.fmt(x / y * 100.0) + " %";
            }
        });
        bind(d1, d2, dOut, new Calc() {
            public String run(double x, double y) {
                if (x == 0) {
                    return "—";
                }
                double change = (y - x) / Math.abs(x) * 100.0;
                String sign = change > 0 ? "+" : "";
                return sign + Ui.fmt(change) + " %";
            }
        });
    }

    private interface Calc {
        String run(double a, double b);
    }

    private TextView result(Context c, LinearLayout card) {
        TextView t = Ui.text(c, "—", 26, Ui.ACCENT, true);
        t.setGravity(Gravity.END);
        t.setPadding(0, Ui.dp(c, 14), 0, 0);
        card.addView(t, new LinearLayout.LayoutParams(-1, -2));
        return t;
    }

    private void bind(final EditText first, final EditText second, final TextView out, final Calc calc) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                double a = Ui.parse(first.getText().toString());
                double b = Ui.parse(second.getText().toString());
                if (!Ui.valid(a) || !Ui.valid(b)) {
                    out.setText("—");
                    return;
                }
                out.setText(calc.run(a, b));
            }
        };
        Ui.watch(first, r);
        Ui.watch(second, r);
        r.run();
    }

    @Override
    public View view() {
        return root;
    }
}
