package org.keycloak.workflow.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Tiny, safe expression evaluator. Supports the operators {@code ==}, {@code !=},
 * {@code &lt;}, {@code &lt;=}, {@code &gt;}, {@code &gt;=} and boolean combinations
 * with {@code &&} / {@code ||}. Variables are pre-substituted from the context map.
 *
 * Purposefully limited — no method calls, no reflection — so unverified
 * expressions persisted in the workflow definition cannot execute arbitrary code.
 */
public final class ExpressionEval {

    private ExpressionEval() {}

    /** @return numeric score 0–100. Missing/invalid expression returns 0. */
    public static int evalRisk(String expression, Map<String, Object> ctx) {
        if (expression == null || expression.isBlank()) return 0;
        try {
            // Risk expressions take the form: "score := <integer arithmetic>".
            // For safety we only allow a comma-separated list of "<token>:<int>" rules
            // where the first matching token wins. Example:
            //   "target=dba-admin:90,target=dba-readonly:40,default:10"
            String[] rules = expression.split(",");
            Integer fallback = null;
            for (String r : rules) {
                String[] kv = r.trim().split(":");
                if (kv.length != 2) continue;
                int score = clamp(Integer.parseInt(kv[1].trim()));
                if (kv[0].trim().equals("default")) { fallback = score; continue; }
                if (matches(kv[0].trim(), ctx)) return score;
            }
            return fallback != null ? fallback : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** @return true if the step should be skipped. False on parse error (fail-closed). */
    public static boolean evalSkip(String expression, Map<String, Object> ctx) {
        if (expression == null || expression.isBlank()) return false;
        try {
            // Boolean expression: AND-of-ORs.
            //   "risk<30"
            //   "risk<30 || targetId==low-impact"
            //   "country==FR && risk<50"
            for (String and : split(expression, "&&")) {
                boolean orMatch = false;
                for (String or : split(and, "||")) {
                    if (evalBoolean(or.trim(), ctx)) { orMatch = true; break; }
                }
                if (!orMatch) return false;
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean evalBoolean(String expr, Map<String, Object> ctx) {
        for (String op : new String[]{"<=", ">=", "==", "!=", "<", ">"}) {
            int i = expr.indexOf(op);
            if (i > 0) {
                String lhs = expr.substring(0, i).trim();
                String rhs = expr.substring(i + op.length()).trim();
                Object lv = ctx.get(lhs);
                if (lv == null) return false;
                return compare(String.valueOf(lv), rhs, op);
            }
        }
        return false;
    }

    private static boolean matches(String selector, Map<String, Object> ctx) {
        // selector: key=value
        int i = selector.indexOf('=');
        if (i <= 0) return false;
        String k = selector.substring(0, i).trim();
        String v = selector.substring(i + 1).trim();
        Object value = ctx.get(k);
        return value != null && String.valueOf(value).equals(v);
    }

    private static boolean compare(String l, String r, String op) {
        try {
            double ln = Double.parseDouble(l);
            double rn = Double.parseDouble(r);
            return switch (op) {
                case "<"  -> ln < rn;
                case "<=" -> ln <= rn;
                case ">"  -> ln > rn;
                case ">=" -> ln >= rn;
                case "==" -> ln == rn;
                case "!=" -> ln != rn;
                default   -> false;
            };
        } catch (NumberFormatException nfe) {
            return switch (op) {
                case "==" -> l.equals(r);
                case "!=" -> !l.equals(r);
                default   -> false;
            };
        }
    }

    private static String[] split(String s, String sep) {
        return s.split(java.util.regex.Pattern.quote(sep));
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }

    public static Map<String, Object> context(String targetType, String targetId,
                                              String requesterId, Integer risk,
                                              Map<String, String> attrs) {
        Map<String, Object> m = new HashMap<>();
        m.put("targetType", targetType);
        m.put("targetId", targetId);
        m.put("target", targetType + ":" + targetId);
        m.put("requester", requesterId);
        if (risk != null) m.put("risk", risk);
        if (attrs != null) attrs.forEach((k, v) -> m.put("attr." + k, v));
        return m;
    }
}
