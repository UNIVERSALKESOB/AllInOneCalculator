# All-in-One Calculator

A fully offline Android calculator app with 9 tools, built without Gradle —
straight `aapt2` + `javac` + `d8` + `apksigner`.

## Tools

| Tab | What it does |
|-----|--------------|
| Basic | + − × ÷, percent (200+10% = 220), ±, live preview |
| Scientific | sin/cos/tan, ln/log, √, x^y, x!, π, e, DEG/RAD, parentheses |
| Units | Length, weight, temperature, area, volume, speed, time, data (60+ units) |
| BMI | BMI value, category, healthy weight range for your height |
| Loan | EMI, total interest, total payment, installments |
| Age | Exact age, total days/months, next birthday, birth weekday |
| Percent | X% of Y, X is what % of Y, percentage change |
| Tip | Tip amount, total, per-person split with quick presets |
| Discount | Final price after discount + tax, amount saved |

## Project layout

```
AndroidManifest.xml       manifest (minSdk 21, targetSdk 34)
build.sh                  one-command build + sign
src/com/allinone/calc/
  MainActivity.java       header, tab chips, screen switching
  Ui.java                 programmatic UI helpers + palette
  Expr.java               recursive-descent math evaluator
  CalculatorScreen.java   basic + scientific keypads
  ConverterScreen.java    unit converter
  BmiScreen.java          BMI
  LoanScreen.java         EMI
  AgeScreen.java          age
  PercentScreen.java      percentage tools
  TipScreen.java          tip split
  DiscountScreen.java     discount + tax
res/                      theme, colors, launcher icons
tools/make_icon.py        generates launcher PNGs with zero dependencies
tools/zipalign.py         aligns stored entries when zipalign is missing
```

## Build

```bash
pkg install openjdk-17 aapt2 d8 apksigner
# android.jar is expected at $HOME/sdk/android-34-ext12/android.jar
# (override with ANDROID_JAR=/path/to/android.jar)
bash build.sh
```

Output: `out/AllInOneCalculator.apk` (signed with a local release key,
valid for ~30 years, keystore at `build/release.keystore`).

## Notes

- No internet permission, no ads, no analytics, no third-party libraries.
- Everything is framework-only Java, so the APK stays tiny (~53 KB).
- The evaluator is covered by `build/test/Test.java` sanity tests.
