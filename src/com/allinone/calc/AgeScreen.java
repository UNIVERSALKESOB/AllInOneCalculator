package com.allinone.calc;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

public class AgeScreen implements Screen {

    private final Context ctx;
    private final View root;
    private final TextView dateLabel;
    private final TextView ageView;
    private final TextView subView;
    private final TextView totalDays;
    private final TextView totalMonths;
    private final TextView nextBirthday;
    private final TextView weekday;
    private Calendar birth;

    public AgeScreen(Context c) {
        this.ctx = c;

        LinearLayout col = Ui.column(c, 0);
        int p = Ui.dp(c, 14);
        col.setPadding(p, Ui.dp(c, 8), p, p);

        LinearLayout card = Ui.card(c, "Age Calculator");

        LinearLayout panel = Ui.column(c, 0);
        panel.setBackground(Ui.roundBg(Ui.SURFACE2, Ui.dp(c, 16)));
        int pp = Ui.dp(c, 16);
        panel.setPadding(pp, pp, pp, pp);
        panel.setGravity(Gravity.CENTER);

        ageView = Ui.text(c, "—", 34, Ui.ACCENT, true);
        ageView.setGravity(Gravity.CENTER);
        subView = Ui.text(c, "Pick your date of birth", 13, Ui.MUTED, false);
        subView.setGravity(Gravity.CENTER);
        panel.addView(ageView, new LinearLayout.LayoutParams(-1, -2));
        panel.addView(subView, new LinearLayout.LayoutParams(-1, -2));
        card.addView(panel, new LinearLayout.LayoutParams(-1, -2));

        dateLabel = Ui.text(c, "No date selected", 14, Ui.TEXT, false);
        dateLabel.setPadding(0, Ui.dp(c, 14), 0, Ui.dp(c, 10));
        card.addView(dateLabel);

        TextView pick = Ui.key(c, "Choose date of birth", Ui.ACCENT, Ui.ACCENT_D, 15);
        pick.setPadding(0, Ui.dp(c, 14), 0, Ui.dp(c, 14));
        card.addView(pick, new LinearLayout.LayoutParams(-1, -2));
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openPicker();
            }
        });

        totalDays = Ui.addOutput(c, card, "Total days lived");
        totalMonths = Ui.addOutput(c, card, "Total months");
        nextBirthday = Ui.addOutput(c, card, "Next birthday in");
        weekday = Ui.addOutput(c, card, "Born on");

        col.addView(card, new LinearLayout.LayoutParams(-1, -2));

        TextView note = Ui.text(c, "Days are counted from midnight to midnight.",
                12, Ui.MUTED, false);
        note.setPadding(Ui.dp(c, 4), Ui.dp(c, 10), Ui.dp(c, 4), 0);
        col.addView(note);

        root = Ui.scroll(c, col);
    }

    @Override
    public View view() {
        return root;
    }

    private void openPicker() {
        Calendar base = birth != null ? birth : Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(ctx,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        Calendar c = Calendar.getInstance();
                        c.set(year, month, day, 0, 0, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        birth = c;
                        compute();
                    }
                },
                base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void compute() {
        if (birth == null) {
            return;
        }
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        if (birth.after(today)) {
            ageView.setText("—");
            subView.setText("Date is in the future");
            subView.setTextColor(Ui.DANGER);
            return;
        }

        dateLabel.setText(String.format(Locale.US, "%02d %s %d",
                birth.get(Calendar.DAY_OF_MONTH),
                birth.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US),
                birth.get(Calendar.YEAR)));

        int y = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        int m = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH);
        int d = today.get(Calendar.DAY_OF_MONTH) - birth.get(Calendar.DAY_OF_MONTH);

        if (d < 0) {
            m--;
            Calendar previous = (Calendar) today.clone();
            previous.add(Calendar.MONTH, -1);
            d += previous.getActualMaximum(Calendar.DAY_OF_MONTH);
        }
        if (m < 0) {
            m += 12;
            y--;
        }

        ageView.setText(y + "y " + m + "m " + d + "d");
        subView.setText(String.format(Locale.US, "%d years, %d months, %d days", y, m, d));
        subView.setTextColor(Ui.MUTED);

        long days = (today.getTimeInMillis() - birth.getTimeInMillis()) / 86400000L;
        totalDays.setText(String.valueOf(days));
        totalMonths.setText(String.valueOf(y * 12 + m));

        Calendar next = (Calendar) birth.clone();
        next.set(Calendar.YEAR, today.get(Calendar.YEAR));
        next.set(Calendar.HOUR_OF_DAY, 0);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(today)) {
            next.add(Calendar.YEAR, 1);
        }
        long until = (next.getTimeInMillis() - today.getTimeInMillis()) / 86400000L;
        if (until == 0) {
            nextBirthday.setText("Today! 🎉");
        } else {
            nextBirthday.setText(until + (until == 1 ? " day" : " days"));
        }

        weekday.setText(birth.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US));
    }
}
