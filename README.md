# All-in-One Calculator

A fast, fully offline Android calculator with 10 tools, built without Gradle —
straight `aapt2` + `javac` + `d8` + `apksigner`, packaged for Google Play as an
`.aab` with `bundletool`.

## Tools

| Tab | What it does |
|-----|--------------|
| Basic | + − × ÷, percent (200+10% = 220), ±, live preview, history |
| Scientific | sin/cos/tan, ln/log, √, x^y, x!, π, e, DEG/RAD, parentheses |
| Units | Length, weight, temperature, area, volume, speed, time, data (60+ units) |
| Currency | 28 currencies with live rates, cached for offline use |
| BMI | BMI value, category, healthy weight range for your height |
| Loan | EMI, total interest, total payment, installments |
| Age | Exact age, total days/months, next birthday, birth weekday |
| Percent | X% of Y, X is what % of Y, percentage change |
| Tip | Tip amount, total, per-person split with quick presets |
| Discount | Final price after discount + tax, amount saved |

History keeps the last 50 calculations on the device; tap an entry to reuse its
result. Currency rates come from [open.er-api.com](https://open.er-api.com) and
are stored in `SharedPreferences`, so the currency screen keeps working offline.

## Project layout

```
AndroidManifest.xml       manifest (minSdk 21, targetSdk 34, INTERNET for rates)
build.sh                  one-command build + sign
src/com/allinone/calc/
  MainActivity.java       header, tab chips, screen switching
  Ui.java                 programmatic UI helpers + palette
  Expr.java               recursive-descent math evaluator
  CalculatorScreen.java   basic + scientific keypads + history dialog
  HistoryStore.java       last 50 calculations (SharedPreferences/JSON)
  ConverterScreen.java    unit converter
  CurrencyScreen.java     live currency rates with offline cache
  BmiScreen.java          BMI
  LoanScreen.java         EMI
  AgeScreen.java          age
  PercentScreen.java      percentage tools
  TipScreen.java          tip split
  DiscountScreen.java     discount + tax
res/                      theme, colors, launcher icons
tools/make_icon.py        generates launcher PNGs with zero dependencies
tools/zipalign.py         aligns stored entries when zipalign is missing
.github/workflows/build.yml  CI: builds and uploads the APK on every push
```

## Build

```bash
pkg install openjdk-17 aapt2 d8 apksigner
# android.jar is expected at $HOME/sdk/android-36/android.jar
# (override with ANDROID_JAR=/path/to/android.jar)
bash build.sh        # signed APK   -> out/AllInOneCalculator.apk
bash build_aab.sh    # Play bundle  -> out/AllInOneCalculator.aab
```

`build_aab.sh` needs [bundletool](https://github.com/google/bundletool/releases)
(`BUNDLETOOL_JAR`, default `~/tools/bundletool.jar`). It builds the proto-format
APK, assembles the base module, runs `bundletool build-bundle`, validates the
bundle, signs it with `jarsigner`, and finally round-trips it through
`bundletool build-apks --mode=universal` as a smoke test.

Play Store listing assets live in `store/` (`icon-512.png`).

## Signing

The app uses one permanent release key so every build updates over the previous
one instead of requiring an uninstall.

- Locally: `keys/release.jks` with its password in `keys/password.txt`
  (both git-ignored). Missing key/password? The first build generates them.
- CI: the same keystore is passed through the repository secrets
  `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD`.
- The same key is used as the Play upload key. If you enable Play App Signing,
  keep this keystore as the *upload* key: Play re-signs installs with its own
  app-signing key.
- Certificate fingerprint (SHA-256 of the signer):
  `68:3C:33:32:F1:48:E5:0B:01:AC:25:F1:A5:5E:03:FA:36:CF:45:81:13:2D:B8:3A:62:A1:B5:2F:BD:BC:1F:C5`

Keep a backup of `keys/release.jks` + `keys/password.txt`. Losing them means
future builds must be signed with a new key, which Android treats as a different
app (and Play Store will reject the upload).

## Notes

- No ads, no analytics, no third-party libraries.
- Framework-only Java keeps the APK around 65 KB.
- The math evaluator is covered by `build/test/Test.java` sanity tests.
