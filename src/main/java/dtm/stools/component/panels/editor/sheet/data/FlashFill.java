package dtm.stools.component.panels.editor.sheet.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public final class FlashFill {
    private FlashFill() {}

    public static Optional<List<String>> infer(List<String> sources, List<String> examples) {
        if (examples.isEmpty()) return Optional.empty();
        for (Function<String, String> candidate : candidates(sources.getFirst(), examples.getFirst())) {
            boolean ok = true;
            for (int k = 0; k < examples.size(); k++) {
                String got;
                try { got = candidate.apply(sources.get(k)); } catch (RuntimeException e) { ok = false; break; }
                if (!examples.get(k).equals(got)) { ok = false; break; }
            }
            if (!ok) continue;
            List<String> out = new ArrayList<>();
            for (int k = examples.size(); k < sources.size(); k++) {
                try { out.add(candidate.apply(sources.get(k))); } catch (RuntimeException e) { out.add(""); }
            }
            return Optional.of(out);
        }
        return Optional.empty();
    }

    private static List<Function<String, String>> candidates(String source, String example) {
        List<Function<String, String>> list = new ArrayList<>();
        list.add(s -> s.toUpperCase(Locale.ROOT));
        list.add(s -> s.toLowerCase(Locale.ROOT));
        list.add(FlashFill::proper);
        for (String delimiter : new String[]{" ", ",", ";", "-", "_", "@", ".", "/", "|", ":"}) {
            String[] parts = source.split(java.util.regex.Pattern.quote(delimiter), -1);
            for (int i = 0; i < parts.length; i++) {
                int idx = i;
                list.add(s -> token(s, delimiter, idx));
                list.add(s -> token(s, delimiter, idx).toUpperCase(Locale.ROOT));
                list.add(s -> proper(token(s, delimiter, idx)));
                list.add(s -> token(s, delimiter, idx).isEmpty() ? "" : token(s, delimiter, idx).substring(0, 1));
                list.add(s -> token(s, delimiter, idx).isEmpty() ? "" : token(s, delimiter, idx).substring(0, 1).toUpperCase(Locale.ROOT));
                for (int j = i + 1; j < parts.length; j++) {
                    int jdx = j;
                    for (String joiner : new String[]{" ", "", ", ", ".", "-", "_"}) {
                        list.add(s -> token(s, delimiter, idx) + joiner + token(s, delimiter, jdx));
                        list.add(s -> token(s, delimiter, jdx) + joiner + token(s, delimiter, idx));
                        list.add(s -> token(s, delimiter, jdx) + joiner + token(s, delimiter, idx).charAt(0) + ".");
                        list.add(s -> (token(s, delimiter, idx) + joiner + token(s, delimiter, jdx)).toLowerCase(Locale.ROOT));
                    }
                }
            }
            list.add(s -> { String[] p = s.split(java.util.regex.Pattern.quote(delimiter)); StringBuilder b = new StringBuilder(); for (String x : p) if (!x.isEmpty()) b.append(Character.toUpperCase(x.charAt(0))); return b.toString(); });
        }
        list.add(s -> s.replaceAll("\\D", ""));
        list.add(s -> s.replaceAll("[^\\p{L} ]", "").strip());
        int start = source.indexOf(example);
        if (start >= 0) {
            int fromEnd = source.length() - start;
            list.add(s -> s.substring(start, Math.min(s.length(), start + example.length())));
            list.add(s -> s.substring(Math.max(0, s.length() - fromEnd), Math.max(0, s.length() - fromEnd) + example.length()));
            list.add(s -> s.length() > start ? s.substring(start) : "");
        }
        return list;
    }

    private static String token(String s, String delimiter, int index) {
        String[] parts = s.split(java.util.regex.Pattern.quote(delimiter), -1);
        if (index >= parts.length) throw new IllegalArgumentException();
        return parts[index].strip();
    }

    private static String proper(String s) {
        StringBuilder b = new StringBuilder();
        boolean up = true;
        for (char c : s.toCharArray()) { b.append(up && Character.isLetter(c) ? Character.toUpperCase(c) : Character.toLowerCase(c)); up = !Character.isLetter(c); }
        return b.toString();
    }
}
