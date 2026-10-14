package dtm.stools.component.inputfields.textarea.diagnostic;

import dtm.stools.i18n.I18n;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@Accessors(chain = true)
public class DictionarySpellChecker implements TextDiagnosticProvider {

    private static final Pattern WORD = Pattern.compile("\\p{L}+(?:['’\\-]\\p{L}+)*");
    private static final Pattern WORD_SEPARATOR = Pattern.compile("['’\\-]");
    private static final int MAX_DISTANCE = 2;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Set<String> words = ConcurrentHashMap.newKeySet();
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Map<Integer, Set<String>> wordsByLength = new ConcurrentHashMap<>();
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Map<String, List<String>> suggestionCache = new ConcurrentHashMap<>();

    private DiagnosticSeverity severity = DiagnosticSeverity.ERROR;
    private int minWordLength = 2;
    private int maxSuggestions = 5;
    private int maxWordsWithSuggestions = 50;
    private boolean ignoreUppercase = true;
    private boolean addToDictionaryAction = true;

    public DictionarySpellChecker() {
    }

    public DictionarySpellChecker(Collection<String> words) {
        addWords(words);
    }

    public static DictionarySpellChecker fromFile(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return fromStream(in, StandardCharsets.UTF_8);
        }
    }

    public static DictionarySpellChecker fromStream(InputStream in, Charset charset) throws IOException {
        DictionarySpellChecker checker = new DictionarySpellChecker();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String entry = line.strip();
                if (entry.isEmpty() || entry.startsWith("#") || entry.chars().allMatch(Character::isDigit)) continue;
                int flags = entry.indexOf('/');
                checker.addWord(flags >= 0 ? entry.substring(0, flags) : entry);
            }
        }
        return checker;
    }

    public DictionarySpellChecker addWord(String word) {
        if (word == null || word.isBlank()) return this;
        String normalized = normalize(word.strip());
        if (words.add(normalized)) {
            wordsByLength.computeIfAbsent(normalized.length(), k -> ConcurrentHashMap.newKeySet()).add(normalized);
            suggestionCache.clear();
        }
        return this;
    }

    public DictionarySpellChecker addWords(Collection<String> entries) {
        if (entries != null) entries.forEach(this::addWord);
        return this;
    }

    public DictionarySpellChecker removeWord(String word) {
        if (word == null) return this;
        String normalized = normalize(word.strip());
        if (words.remove(normalized)) {
            Set<String> bucket = wordsByLength.get(normalized.length());
            if (bucket != null) bucket.remove(normalized);
            suggestionCache.clear();
        }
        return this;
    }

    public int size() {
        return words.size();
    }

    public boolean isKnown(String word) {
        if (word == null || word.isEmpty()) return true;
        String normalized = normalize(word);
        if (words.contains(normalized)) return true;
        String[] parts = WORD_SEPARATOR.split(normalized);
        if (parts.length < 2) return false;
        for (String part : parts) {
            if (!part.isEmpty() && !words.contains(part)) return false;
        }
        return true;
    }

    public List<String> suggest(String word) {
        if (word == null || word.isEmpty() || maxSuggestions <= 0) return List.of();
        String normalized = normalize(word);
        List<String> base = suggestionCache.computeIfAbsent(normalized, this::computeSuggestions);
        List<String> adjusted = new ArrayList<>(base.size());
        for (String suggestion : base) adjusted.add(matchCase(word, suggestion));
        return adjusted;
    }

    @Override
    public List<TextDiagnostic> diagnose(String text) {
        List<TextDiagnostic> diagnostics = new ArrayList<>();
        if (text == null || text.isEmpty()) return diagnostics;
        Matcher matcher = WORD.matcher(text);
        int withSuggestions = 0;
        while (matcher.find()) {
            if (Thread.currentThread().isInterrupted()) break;
            String word = matcher.group();
            if (word.length() < minWordLength) continue;
            if (ignoreUppercase && word.length() > 1 && isUppercase(word)) continue;
            if (isKnown(word)) continue;
            List<TextDiagnosticAction> actions = new ArrayList<>();
            if (withSuggestions < maxWordsWithSuggestions) {
                withSuggestions++;
                for (String suggestion : suggest(word)) actions.add(TextDiagnosticAction.replaceWith(suggestion));
            }
            if (addToDictionaryAction) {
                actions.add(TextDiagnosticAction.of(text("addToDictionary", "Adicionar ao dicionário"), (area, diagnostic) -> {
                    addWord(word);
                    area.runDiagnostics();
                }));
            }
            String message = text("unknownWord", "Palavra desconhecida: \"{word}\"").replace("{word}", word);
            diagnostics.add(new TextDiagnostic(matcher.start(), matcher.end(), severity, message, "spelling", actions));
        }
        return diagnostics;
    }

    private List<String> computeSuggestions(String word) {
        record Candidate(String word, int distance) {
        }
        List<Candidate> candidates = new ArrayList<>();
        for (int length = Math.max(1, word.length() - MAX_DISTANCE); length <= word.length() + MAX_DISTANCE; length++) {
            Set<String> bucket = wordsByLength.get(length);
            if (bucket == null) continue;
            for (String entry : bucket) {
                int distance = distance(word, entry, MAX_DISTANCE);
                if (distance <= MAX_DISTANCE) candidates.add(new Candidate(entry, distance));
            }
        }
        candidates.sort(Comparator.comparingInt(Candidate::distance)
                .thenComparingInt(c -> Math.abs(c.word().length() - word.length()))
                .thenComparing(Candidate::word));
        List<String> result = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (result.size() >= maxSuggestions) break;
            result.add(candidate.word());
        }
        return List.copyOf(result);
    }

    static int distance(String a, String b, int limit) {
        int n = a.length();
        int m = b.length();
        if (Math.abs(n - m) > limit) return limit + 1;
        int[] previousPrevious = new int[m + 1];
        int[] previous = new int[m + 1];
        int[] current = new int[m + 1];
        for (int j = 0; j <= m; j++) previous[j] = j;
        for (int i = 1; i <= n; i++) {
            current[0] = i;
            int rowMin = current[0];
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                char cb = b.charAt(j - 1);
                int cost = ca == cb ? 0 : 1;
                int value = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1), previous[j - 1] + cost);
                if (i > 1 && j > 1 && ca == b.charAt(j - 2) && a.charAt(i - 2) == cb) {
                    value = Math.min(value, previousPrevious[j - 2] + 1);
                }
                current[j] = value;
                rowMin = Math.min(rowMin, value);
            }
            if (rowMin > limit) return limit + 1;
            int[] recycled = previousPrevious;
            previousPrevious = previous;
            previous = current;
            current = recycled;
        }
        return previous[m];
    }

    private static String matchCase(String original, String suggestion) {
        if (original.length() > 1 && isUppercase(original)) return suggestion.toUpperCase(Locale.ROOT);
        if (Character.isUpperCase(original.charAt(0))) {
            return suggestion.substring(0, 1).toUpperCase(Locale.ROOT) + suggestion.substring(1);
        }
        return suggestion;
    }

    private static boolean isUppercase(String word) {
        boolean hasLetter = false;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
                if (!Character.isUpperCase(c)) return false;
            }
        }
        return hasLetter;
    }

    private static String normalize(String word) {
        return word.replace('’', '\'').toLowerCase(Locale.ROOT);
    }

    private static String text(String key, String fallback) {
        return I18n.getText(DictionarySpellChecker.class, key, fallback);
    }
}
