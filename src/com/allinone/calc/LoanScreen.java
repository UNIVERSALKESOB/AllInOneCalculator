package com.allinone.calc;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LoanScreen implements Screen {

    private final View root;
    private final TextView emiView;
    private final TextView interestOut;
    private final TextView totalOut;
    private final TextView monthsOut;
    private final EditText amount;
    private final EditText rate;
    private final EditText years;

    public LoanScreen(Context c) {
        LinearLayout col = Ui.column(c, 0);
        int p = Ui.dp(c, 14);
        col.setPadding(p, Ui.dp(c, 8), p, p);

        LinearLayout card = Ui.card(c, "Loan / EMI Calculator");

        LinearLayout panel = Ui.column(c, 0);
        panel.setBackground(Ui.roundBg(Ui.SURFACE2, Ui.dp(c, 16)));
        int pp = Ui.dp(c, 16);
        panel.setPadding(pp, pp, pp, pp);
        panel.setGravity(Gravity.CENTER);

        TextView cap = Ui.cap(c, "Monthly payment");
        cap.setGravity(Gravity.CENTER);
        emiView = Ui.text(c, "—", 36, Ui.ACCENT, true);
        emiView.setGravity(Gravity.CENTER);
        panel.addView(cap, new LinearLayout.LayoutParams(-1, -2));
        panel.addView(emiView, new LinearLayout.LayoutParams(-1, -2));
        card.addView(panel, new LinearLayout.LayoutParams(-1, -2));

        amount = Ui.addField(c, card, "Loan amount", "e.g. 500000", "500000");
        rate = Ui.addField(c, card, "Annual interest rate (%)", "e.g. 9.5", "9.5");
        years = Ui.addField(c, card, "Tenure (years)", "e.g. 5", "5");

        interestOut = Ui.addOutput(c, card, "Total interest");
        totalOut = Ui.addOutput(c, card, "Total payment");
        monthsOut = Ui.addOutput(c, card, "Number of installments");

        col.addView(card, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout formula = Ui.card(c, "Formula");
        TextView f = Ui.text(c, "EMI = P × r × (1 + r)ⁿ / ((1 + r)ⁿ − 1)\nr = annual rate / 12 / 100,  n = years × 12",
                12, Ui.MUTED, false);
        f.setLineSpacing(Ui.dp(c, 3), 1f);
        formula.addView(f);
        col.addView(formula, Ui.lp(-1, -2, 0, 12, 0, 0, c));

        root = Ui.scroll(c, col);

        Runnable recalc = new Runnable() {
            @Override
            public void run() {
                compute();
            }
        };
        Ui.watch(amount, recalc);
        Ui.watch(rate, recalc);
        Ui.watch(years, recalc);

        compute();
    }

    @Override
    public View view() {
        return root;
    }

    private void compute() {
        double principal = Ui.parse(amount.getText().toString());
        double annual = Ui.parse(rate.getText().toString());
        double yrs = Ui.parse(years.getText().toString());

        if (!Ui.valid(principal) || !Ui.valid(annual) || !Ui.valid(yrs)
                || principal <= 0 || annual < 0 || yrs <= 0) {
            emiView.setText("—");
            interestOut.setText("—");
            totalOut.setText("—");
            monthsOut.setText("—");
            return;
        }

        int months = Math.max(1, (int) Math.round(yrs * 12));
        double r = annual / 12.0 / 100.0;
        double emi;
        if (r == 0) {
            emi = principal / months;
        } else {
            double pow = Math.pow(1 + r, months);
            emi = principal * r * pow / (pow - 1);
        }
        double total = emi * months;
        double interest = total - principal;

        emiView.setText(Ui.money(emi));
        interestOut.setText(Ui.money(interest));
        totalOut.setText(Ui.money(total));
        monthsOut.setText(String.valueOf(months) + " months");
    }
}
