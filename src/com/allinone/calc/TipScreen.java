package com.allinone.calc;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TipScreen implements Screen {

    private final View root;
    private final EditText bill;
    private final EditText tip;
    private final EditText people;
    private final TextView tipOut;
    private final TextView totalOut;
    private final TextView eachOut;

    public TipScreen(Context c) {
        LinearLayout col = Ui.column(c, 0);
        int p = Ui.dp(c, 14);
        col.setPadding(p, Ui.dp(c, 8), p, p);

        LinearLayout card = Ui.card(c, "Tip & Bill Split");

        LinearLayout panel = Ui.column(c, 0);
        panel.setBackground(Ui.roundBg(Ui.SURFACE2, Ui.dp(c, 16)));
        int pp = Ui.dp(c, 16);
        panel.setPadding(pp, pp, pp, pp);
        panel.setGravity(Gravity.CENTER);
        TextView cap = Ui.cap(c, "Each person pays");
        cap.setGravity(Gravity.CENTER);
        eachOut = Ui.text(c, "—", 34, Ui.ACCENT, true);
        eachOut.setGravity(Gravity.CENTER);
        panel.addView(cap, new LinearLayout.LayoutParams(-1, -2));
        panel.addView(eachOut, new LinearLayout.LayoutParams(-1, -2));
        card.addView(panel, new LinearLayout.LayoutParams(-1, -2));

        bill = Ui.addField(c, card, "Bill amount", "e.g. 1200", "1200");
        tip = Ui.addField(c, card, "Tip (%)", "e.g. 10", "10");

        LinearLayout quick = Ui.row(c);
        quick.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 4));
        String[] presets = {"5%", "10%", "15%", "20%", "25%"};
        for (final String preset : presets) {
            TextView b = Ui.key(c, preset, Ui.OP_BG, Ui.ACCENT, 13);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, Ui.dp(c, 38), 1f);
            bp.rightMargin = Ui.dp(c, 6);
            quick.addView(b, bp);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tip.setText(preset.replace("%", ""));
                }
            });
        }
        card.addView(quick);

        people = Ui.addField(c, card, "Split between (people)", "e.g. 4", "4");

        tipOut = Ui.addOutput(c, card, "Tip amount");
        totalOut = Ui.addOutput(c, card, "Total with tip");

        col.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView note = Ui.text(c, "Handy for restaurants, delivery and shared bills.",
                12, Ui.MUTED, false);
        note.setPadding(Ui.dp(c, 4), Ui.dp(c, 10), Ui.dp(c, 4), 0);
        col.addView(note);

        root = Ui.scroll(c, col);

        Runnable recalc = new Runnable() {
            @Override
            public void run() {
                compute();
            }
        };
        Ui.watch(bill, recalc);
        Ui.watch(tip, recalc);
        Ui.watch(people, recalc);

        compute();
    }

    @Override
    public View view() {
        return root;
    }

    private void compute() {
        double b = Ui.parse(bill.getText().toString());
        double t = Ui.parse(tip.getText().toString());
        double n = Ui.parse(people.getText().toString());

        if (!Ui.valid(b) || !Ui.valid(t) || !Ui.valid(n) || b < 0 || n < 1) {
            eachOut.setText("—");
            tipOut.setText("—");
            totalOut.setText("—");
            return;
        }
        double tipAmount = b * t / 100.0;
        double total = b + tipAmount;
        int count = Math.max(1, (int) Math.round(n));

        eachOut.setText(Ui.money(total / count));
        tipOut.setText(Ui.money(tipAmount));
        totalOut.setText(Ui.money(total) + "  (" + count + (count == 1 ? " person)" : " people)"));
    }
}
