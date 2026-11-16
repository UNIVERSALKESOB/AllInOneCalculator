package com.allinone.calc;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BmiScreen implements Screen {

    private final View root;
    private final TextView bmiView;
    private final TextView categoryView;
    private final TextView rangeView;
    private final EditText weight;
    private final EditText height;

    public BmiScreen(Context c) {
        LinearLayout col = Ui.column(c, 0);
        int p = Ui.dp(c, 14);
        col.setPadding(p, Ui.dp(c, 8), p, p);

        LinearLayout card = Ui.card(c, "Body Mass Index");

        LinearLayout panel = Ui.column(c, 0);
        panel.setBackground(Ui.roundBg(Ui.SURFACE2, Ui.dp(c, 16)));
        int pp = Ui.dp(c, 16);
        panel.setPadding(pp, pp, pp, pp);
        panel.setGravity(Gravity.CENTER);

        bmiView = Ui.text(c, "—", 44, Ui.ACCENT, true);
        bmiView.setGravity(Gravity.CENTER);
        categoryView = Ui.text(c, "Enter your weight and height", 14, Ui.MUTED, false);
        categoryView.setGravity(Gravity.CENTER);
        categoryView.setPadding(0, Ui.dp(c, 4), 0, 0);
        panel.addView(bmiView, new LinearLayout.LayoutParams(-1, -2));
        panel.addView(categoryView, new LinearLayout.LayoutParams(-1, -2));
        card.addView(panel, new LinearLayout.LayoutParams(-1, -2));

        weight = Ui.addField(c, card, "Weight (kg)", "e.g. 68", "68");
        height = Ui.addField(c, card, "Height (cm)", "e.g. 172", "172");

        rangeView = Ui.text(c, "", 13, Ui.MUTED, false);
        rangeView.setPadding(0, Ui.dp(c, 12), 0, 0);
        card.addView(rangeView);

        col.addView(card, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout info = Ui.card(c, "Reference");
        info.addView(desc(c, "Underweight", "below 18.5", Ui.WARN));
        info.addView(desc(c, "Normal", "18.5 – 24.9", Ui.ACCENT));
        info.addView(desc(c, "Overweight", "25.0 – 29.9", 0xFFFFB86B));
        info.addView(desc(c, "Obese", "30.0 and above", Ui.DANGER));
        col.addView(info, Ui.lp(-1, -2, 0, 12, 0, 0, c));

        TextView note = Ui.text(c, "BMI is a rough screening tool — it does not measure body fat directly.",
                12, Ui.MUTED, false);
        note.setPadding(Ui.dp(c, 4), Ui.dp(c, 10), Ui.dp(c, 4), 0);
        col.addView(note);

        root = Ui.scroll(c, col);

        Ui.watch(weight, new Runnable() {
            @Override
            public void run() {
                compute();
            }
        });
        Ui.watch(height, new Runnable() {
            @Override
            public void run() {
                compute();
            }
        });

        compute();
    }

    private View desc(Context c, String title, String range, int color) {
        LinearLayout row = Ui.row(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, Ui.dp(c, 6), 0, Ui.dp(c, 6));
        TextView dot = Ui.text(c, "●", 10, color, false);
        row.addView(dot);
        TextView name = Ui.text(c, title, 13, Ui.TEXT, false);
        name.setPadding(Ui.dp(c, 8), 0, 0, 0);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView r = Ui.text(c, range, 13, Ui.MUTED, false);
        row.addView(r);
        return row;
    }

    @Override
    public View view() {
        return root;
    }

    private void compute() {
        double kg = Ui.parse(weight.getText().toString());
        double cm = Ui.parse(height.getText().toString());
        if (!Ui.valid(kg) || !Ui.valid(cm) || kg <= 0 || cm <= 0) {
            bmiView.setText("—");
            categoryView.setText("Enter your weight and height");
            categoryView.setTextColor(Ui.MUTED);
            rangeView.setText("");
            return;
        }
        double m = cm / 100.0;
        double bmi = kg / (m * m);

        bmiView.setText(Ui.fmt(Math.round(bmi * 10.0) / 10.0));

        String cat;
        int color;
        if (bmi < 18.5) {
            cat = "Underweight";
            color = Ui.WARN;
        } else if (bmi < 25) {
            cat = "Normal weight";
            color = Ui.ACCENT;
        } else if (bmi < 30) {
            cat = "Overweight";
            color = 0xFFFFB86B;
        } else {
            cat = "Obese";
            color = Ui.DANGER;
        }
        categoryView.setText(cat);
        categoryView.setTextColor(color);

        double low = 18.5 * m * m;
        double high = 24.9 * m * m;
        rangeView.setText("Healthy weight for " + Ui.fmt(cm) + " cm: "
                + Ui.fmt(Math.round(low * 10.0) / 10.0) + " – "
                + Ui.fmt(Math.round(high * 10.0) / 10.0) + " kg");
    }
}
