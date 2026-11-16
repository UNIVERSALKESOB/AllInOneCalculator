package com.allinone.calc;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DiscountScreen implements Screen {

    private final View root;
    private final EditText price;
    private final EditText discount;
    private final EditText tax;
    private final TextView payOut;
    private final TextView savedOut;
    private final TextView taxOut;

    public DiscountScreen(Context c) {
        LinearLayout col = Ui.column(c, 0);
        int p = Ui.dp(c, 14);
        col.setPadding(p, Ui.dp(c, 8), p, p);

        LinearLayout card = Ui.card(c, "Discount & Tax");

        LinearLayout panel = Ui.column(c, 0);
        panel.setBackground(Ui.roundBg(Ui.SURFACE2, Ui.dp(c, 16)));
        int pp = Ui.dp(c, 16);
        panel.setPadding(pp, pp, pp, pp);
        panel.setGravity(Gravity.CENTER);
        TextView cap = Ui.cap(c, "You pay");
        cap.setGravity(Gravity.CENTER);
        payOut = Ui.text(c, "—", 34, Ui.ACCENT, true);
        payOut.setGravity(Gravity.CENTER);
        panel.addView(cap, new LinearLayout.LayoutParams(-1, -2));
        panel.addView(payOut, new LinearLayout.LayoutParams(-1, -2));
        card.addView(panel, new LinearLayout.LayoutParams(-1, -2));

        price = Ui.addField(c, card, "Original price", "e.g. 2500", "2500");
        discount = Ui.addField(c, card, "Discount (%)", "e.g. 20", "20");
        tax = Ui.addField(c, card, "Tax / VAT (%)", "e.g. 0", "0");

        savedOut = Ui.addOutput(c, card, "You save");
        taxOut = Ui.addOutput(c, card, "Tax added");

        col.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView note = Ui.text(c, "Tax is applied after the discount, which is how most stores calculate it.",
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
        Ui.watch(price, recalc);
        Ui.watch(discount, recalc);
        Ui.watch(tax, recalc);

        compute();
    }

    @Override
    public View view() {
        return root;
    }

    private void compute() {
        double p = Ui.parse(price.getText().toString());
        double d = Ui.parse(discount.getText().toString());
        double t = Ui.parse(tax.getText().toString());

        if (!Ui.valid(p) || !Ui.valid(d) || !Ui.valid(t) || p < 0) {
            payOut.setText("—");
            savedOut.setText("—");
            taxOut.setText("—");
            return;
        }
        double saved = p * d / 100.0;
        double afterDiscount = p - saved;
        double taxAmount = afterDiscount * t / 100.0;
        double finalPrice = afterDiscount + taxAmount;

        payOut.setText(Ui.money(finalPrice));
        savedOut.setText(Ui.money(saved) + "  (" + Ui.fmt(d) + "%)");
        taxOut.setText(Ui.money(taxAmount));
    }
}
