package com.allinone.calc;

/**
 * Tiny recursive-descent math expression evaluator.
 *
 * Supports + - * / ^ , parentheses, unary minus, factorial, postfix percent
 * (with the common "200 + 10%" => 220 behaviour), implicit multiplication
 * (2pi, 3(4+5), 2sin(30)), constants pi/e and the usual functions.
 */
public final class Expr {

    private final String s;
    private final boolean deg;
    private int p;

    private Expr(String s, boolean deg) {
        this.s = s;
        this.deg = deg;
    }

    public static double eval(String src, boolean degrees) {
        if (src == null) {
            throw new IllegalArgumentException("empty");
        }
        Expr e = new Expr(src.replace('−', '-').replace('–', '-'), degrees);
        Num n = e.parseExpr();
        e.ws();
        if (e.p < e.s.length()) {
            throw new IllegalArgumentException("Unexpected: " + e.s.charAt(e.p));
        }
        return n.plain();
    }

    private static final class Num {
        final double v;
        final boolean pct;

        Num(double v, boolean pct) {
            this.v = v;
            this.pct = pct;
        }

        double plain() {
            return pct ? v / 100.0 : v;
        }
    }

    private void ws() {
        while (p < s.length() && Character.isWhitespace(s.charAt(p))) {
            p++;
        }
    }

    private Num parseExpr() {
        Num left = parseTerm();
        while (true) {
            ws();
            if (p >= s.length()) {
                return left;
            }
            char ch = s.charAt(p);
            if (ch != '+' && ch != '-') {
                return left;
            }
            p++;
            Num right = parseTerm();
            if (right.pct) {
                double base = left.plain();
                left = new Num(ch == '+' ? base + base * right.v / 100.0
                                         : base - base * right.v / 100.0, false);
            } else {
                left = new Num(ch == '+' ? left.plain() + right.v
                                         : left.plain() - right.v, false);
            }
        }
    }

    private Num parseTerm() {
        Num left = parseUnary();
        while (true) {
            ws();
            if (p >= s.length()) {
                return left;
            }
            char ch = s.charAt(p);
            if (ch == '*' || ch == '/' || ch == '×' || ch == '÷') {
                p++;
                Num right = parseUnary();
                double rv = right.plain();
                if ((ch == '/' || ch == '÷') && rv == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                left = new Num(ch == '*' || ch == '×' ? left.plain() * rv : left.plain() / rv, false);
            } else if (ch == '(' || Character.isLetter(ch) || ch == 'π' || ch == '√') {
                // implicit multiplication: 2(3+4), 2pi, 2sin(30)
                Num right = parseUnary();
                left = new Num(left.plain() * right.plain(), false);
            } else {
                return left;
            }
        }
    }

    private Num parseUnary() {
        ws();
        if (p < s.length()) {
            char ch = s.charAt(p);
            if (ch == '-') {
                p++;
                return new Num(-parseUnary().plain(), false);
            }
            if (ch == '+') {
                p++;
                return parseUnary();
            }
        }
        return parsePower();
    }

    private Num parsePower() {
        Num base = parsePostfix();
        ws();
        if (p < s.length() && s.charAt(p) == '^') {
            p++;
            Num ex = parseUnary();
            return new Num(Math.pow(base.plain(), ex.plain()), false);
        }
        return base;
    }

    private Num parsePostfix() {
        Num n = parsePrimary();
        while (true) {
            ws();
            if (p < s.length()) {
                char ch = s.charAt(p);
                if (ch == '!') {
                    p++;
                    n = new Num(factorial(n.plain()), false);
                    continue;
                }
                if (ch == '%') {
                    p++;
                    n = new Num(n.plain(), true);
                    continue;
                }
            }
            return n;
        }
    }

    private Num parsePrimary() {
        ws();
        if (p >= s.length()) {
            throw new IllegalArgumentException("Incomplete expression");
        }
        char ch = s.charAt(p);

        if (ch == '(') {
            p++;
            Num n = parseExpr();
            ws();
            if (p >= s.length() || s.charAt(p) != ')') {
                throw new IllegalArgumentException("Missing )");
            }
            p++;
            return new Num(n.plain(), false);
        }

        if (ch == '√') {
            p++;
            Num n = parsePrimary();
            double v = n.plain();
            if (v < 0) {
                throw new ArithmeticException("sqrt of negative");
            }
            return new Num(Math.sqrt(v), false);
        }

        if (Character.isDigit(ch) || ch == '.') {
            return number();
        }

        if (Character.isLetter(ch)) {
            return identifier();
        }

        throw new IllegalArgumentException("Unexpected: " + ch);
    }

    private Num number() {
        int start = p;
        boolean dot = false;
        while (p < s.length()) {
            char ch = s.charAt(p);
            if (Character.isDigit(ch)) {
                p++;
            } else if (ch == '.' && !dot) {
                dot = true;
                p++;
            } else if ((ch == 'e' || ch == 'E') && p > start && looksLikeExponent(p)) {
                p++;
                if (p < s.length() && (s.charAt(p) == '+' || s.charAt(p) == '-')) {
                    p++;
                }
                while (p < s.length() && Character.isDigit(s.charAt(p))) {
                    p++;
                }
                break;
            } else {
                break;
            }
        }
        String t = s.substring(start, p);
        if (t.equals(".")) {
            throw new IllegalArgumentException("Bad number");
        }
        return new Num(Double.parseDouble(t), false);
    }

    private boolean looksLikeExponent(int at) {
        int q = at + 1;
        if (q < s.length() && (s.charAt(q) == '+' || s.charAt(q) == '-')) {
            q++;
        }
        return q < s.length() && Character.isDigit(s.charAt(q));
    }

    private Num identifier() {
        int start = p;
        while (p < s.length() && (Character.isLetter(s.charAt(p)))) {
            p++;
        }
        String name = s.substring(start, p).toLowerCase(java.util.Locale.US);

        if (name.equals("pi") || name.equals("π")) {
            return new Num(Math.PI, false);
        }
        if (name.equals("e")) {
            return new Num(Math.E, false);
        }

        ws();
        if (p >= s.length() || s.charAt(p) != '(') {
            throw new IllegalArgumentException("Expected ( after " + name);
        }
        p++;
        double a = parseExpr().plain();
        ws();
        if (p >= s.length() || s.charAt(p) != ')') {
            throw new IllegalArgumentException("Missing )");
        }
        p++;

        switch (name) {
            case "sin": return new Num(Math.sin(toRad(a)), false);
            case "cos": return new Num(Math.cos(toRad(a)), false);
            case "tan": return new Num(Math.tan(toRad(a)), false);
            case "asin": return new Num(fromRad(Math.asin(a)), false);
            case "acos": return new Num(fromRad(Math.acos(a)), false);
            case "atan": return new Num(fromRad(Math.atan(a)), false);
            case "ln":   return new Num(Math.log(a), false);
            case "log":  return new Num(Math.log10(a), false);
            case "sqrt": return new Num(Math.sqrt(a), false);
            case "abs":  return new Num(Math.abs(a), false);
            case "exp":  return new Num(Math.exp(a), false);
            default:
                throw new IllegalArgumentException("Unknown function: " + name);
        }
    }

    private double toRad(double a) {
        return deg ? Math.toRadians(a) : a;
    }

    private double fromRad(double a) {
        return deg ? Math.toDegrees(a) : a;
    }

    private double factorial(double v) {
        if (v < 0 || v != Math.rint(v) || v > 170) {
            throw new ArithmeticException("Bad factorial");
        }
        double r = 1;
        for (int i = 2; i <= (int) v; i++) {
            r *= i;
        }
        return r;
    }
}
