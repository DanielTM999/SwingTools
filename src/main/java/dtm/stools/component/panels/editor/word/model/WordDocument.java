package dtm.stools.component.panels.editor.word.model;

import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public final class WordDocument {
    private static final Pattern GRAPHEME = Pattern.compile("\\X");
    private final List<WordBlock> blocks;
    private final WordPageSettings pageSettings;
    private final WordParts parts;
    private final List<WordParagraph> paragraphs;
    private final List<int[]> paths;
    private final Map<UUID,int[]> tablePaths;
    private final int[] starts;
    private final String text;
    private int hash;

    public WordDocument(List<? extends WordBlock> blocks, WordPageSettings pageSettings) { this(blocks,pageSettings,WordParts.EMPTY); }
    public WordDocument(List<? extends WordBlock> blocks, WordPageSettings pageSettings, WordParts parts) {
        List<WordBlock> copy = new ArrayList<>(Objects.requireNonNull(blocks));
        copy.forEach(Objects::requireNonNull);
        if (copy.isEmpty()) throw new IllegalArgumentException("A document must contain a paragraph");
        if (!(copy.getLast() instanceof WordParagraph)) copy.add(WordParagraph.of(""));
        this.blocks = List.copyOf(copy);
        this.pageSettings = Objects.requireNonNull(pageSettings);
        this.parts = Objects.requireNonNull(parts);
        List<WordParagraph> flat = new ArrayList<>(); List<int[]> flatPaths = new ArrayList<>(); Map<UUID,int[]> tables = new HashMap<>();
        Set<UUID> ids = new HashSet<>();
        flatten(this.blocks,new int[0],flat,flatPaths,tables,ids);
        this.paragraphs = List.copyOf(flat);
        this.paths = List.copyOf(flatPaths);
        this.tablePaths = Map.copyOf(tables);
        this.starts = new int[flat.size()];
        StringBuilder b = new StringBuilder(); int n = 0;
        for (int i = 0; i < flat.size(); i++) {
            if (i > 0) { b.append('\n'); n++; }
            starts[i] = n; String t = flat.get(i).text(); b.append(t); n += t.length();
        }
        this.text = b.toString();
    }
    private static void flatten(List<WordBlock> container, int[] prefix, List<WordParagraph> flat, List<int[]> flatPaths, Map<UUID,int[]> tables, Set<UUID> ids) {
        for (int i = 0; i < container.size(); i++) {
            WordBlock block = container.get(i);
            if (!ids.add(block.id())) throw new IllegalArgumentException(block instanceof WordParagraph ? "Duplicate paragraph identity" : "Duplicate block identity");
            int[] path = Arrays.copyOf(prefix,prefix.length+1); path[prefix.length] = i;
            if (block instanceof WordParagraph p) { flat.add(p); flatPaths.add(path); }
            else if (block instanceof WordTable table) {
                tables.put(table.id(),path);
                for (int r = 0; r < table.rows().size(); r++) {
                    WordTableRow row = table.rows().get(r);
                    for (int c = 0; c < row.cells().size(); c++) {
                        int[] cellPrefix = Arrays.copyOf(path,path.length+2); cellPrefix[path.length] = r; cellPrefix[path.length+1] = c;
                        flatten(row.cells().get(c).blocks(),cellPrefix,flat,flatPaths,tables,ids);
                    }
                }
            }
        }
    }

    public static WordDocument empty() { return fromText(""); }
    public static WordDocument fromText(String text) {
        return new WordDocument(Arrays.stream(normalize(text).split("\n",-1)).map(WordParagraph::of).toList(),WordPageSettings.A4);
    }
    public static String normalize(String text) {
        return Objects.requireNonNull(text).replace("\r\n","\n").replace('\r','\n').replace(WordObjectRun.TEXT,"");
    }

    public List<WordBlock> blocks() { return blocks; }
    public List<WordParagraph> paragraphs() { return paragraphs; }
    public WordPageSettings pageSettings() { return pageSettings; }
    public WordParts parts() { return parts; }
    public WordStyleSheet styles() { return parts.styles(); }
    public WordResources resources() { return parts.resources(); }
    public String text() { return text; }
    public int length() { return text.length(); }
    public WordDocument withBlocks(List<? extends WordBlock> value) { return new WordDocument(value,pageSettings,parts); }
    public WordDocument withPageSettings(WordPageSettings value) { return new WordDocument(blocks,value,parts); }
    public WordDocument withParts(WordParts value) { return new WordDocument(blocks,pageSettings,value); }
    public WordDocument withResource(WordResource resource) { return withParts(parts.withResources(parts.resources().with(resource))); }

    public int paragraphStart(int index) { return starts[index]; }
    public int paragraphEnd(int index) { return starts[index] + paragraphs.get(index).length(); }
    public int paragraphIndex(int offset) {
        if (offset < 0 || offset > length()) throw new IndexOutOfBoundsException(offset);
        int low = 0, high = starts.length - 1;
        while (low < high) { int mid = (low + high + 1) >>> 1; if (starts[mid] <= offset) low = mid; else high = mid - 1; }
        return low;
    }
    public WordParagraph paragraphAt(int offset) { return paragraphs.get(paragraphIndex(offset)); }
    public int paragraphIndexOf(UUID id) {
        for (int i = 0; i < paragraphs.size(); i++) if (paragraphs.get(i).id().equals(id)) return i;
        return -1;
    }
    public WordTextStyle styleAt(int offset) { int i = paragraphIndex(offset); return paragraphs.get(i).styleAt(offset - starts[i]); }
    public WordPosition positionOf(int offset) { int i = paragraphIndex(offset); return new WordPosition(paragraphs.get(i).id(),offset - starts[i]); }
    public int offsetOf(WordPosition position) {
        int i = paragraphIndexOf(position.paragraphId());
        if (i < 0) throw new IllegalArgumentException("Unknown paragraph");
        return starts[i] + Math.min(position.offset(),paragraphs.get(i).length());
    }
    public boolean isBoundary(int offset) {
        if (offset == 0 || offset == text.length()) return true;
        if (offset < 0 || offset > text.length()) return false;
        var matcher = GRAPHEME.matcher(text); matcher.region(Math.max(0,lineStart(offset)),text.length());
        while (matcher.find()) { if (matcher.end() == offset) return true; if (matcher.end() > offset) return false; }
        return false;
    }
    public int previousBoundary(int offset) {
        int start = lineStart(Math.max(0,offset-1));
        var m = GRAPHEME.matcher(text); m.region(start,text.length()); int previous = start;
        while (m.find()) { if (m.end() >= offset) return previous; previous = m.end(); }
        return previous;
    }
    public int nextBoundary(int offset) {
        var m = GRAPHEME.matcher(text); m.region(lineStart(offset),text.length());
        while (m.find()) if (m.end() > offset) return m.end();
        return length();
    }
    private int lineStart(int offset) { int i = text.lastIndexOf('\n',Math.min(offset,text.length())-1); return i < 0 ? 0 : i; }
    public void checkRange(int start, int end) {
        if (start < 0 || end < start || end > length() || !isBoundary(start) || !isBoundary(end))
            throw new IllegalArgumentException("Range must use grapheme boundaries");
    }

    public int[] pathOf(int paragraphIndex) { return paths.get(paragraphIndex).clone(); }
    public boolean sameContainer(int start, int end) {
        int[] a = paths.get(paragraphIndex(start)), b = paths.get(paragraphIndex(end));
        return a.length == b.length && Arrays.equals(a,0,a.length-1,b,0,b.length-1);
    }
    public int depth(int offset) { return (paths.get(paragraphIndex(offset)).length - 1) / 3; }
    public WordObjectRun objectAt(int offset) {
        if (offset < 0 || offset >= length() || text.charAt(offset) != WordObjectRun.PLACEHOLDER) return null;
        int i = paragraphIndex(offset); return paragraphs.get(i).objectAt(offset - starts[i]);
    }
    public List<WordObjectRef> objects() {
        List<WordObjectRef> result = new ArrayList<>();
        for (int i = 0; i < paragraphs.size(); i++) {
            int offset = starts[i];
            for (WordInline inline : paragraphs.get(i).runs()) { if (inline instanceof WordObjectRun o) result.add(new WordObjectRef(offset,o)); offset += inline.length(); }
        }
        return result;
    }
    public Optional<WordObjectRef> findObject(String id) { return objects().stream().filter(r -> r.object().id().equals(id)).findFirst(); }
    public boolean hasStructure(int start, int end) {
        if (text.substring(start,end).indexOf(WordObjectRun.PLACEHOLDER) >= 0) return true;
        int first = paragraphIndex(start), last = paragraphIndex(end);
        if (!sameContainer(start,end)) return true;
        int[] a = paths.get(first), b = paths.get(last);
        List<WordBlock> container = container(a);
        for (int k = a[a.length-1]; k <= b[b.length-1]; k++) if (!(container.get(k) instanceof WordParagraph)) return true;
        return false;
    }

    public WordDocument replace(int start, int end, String input, WordTextStyle insertionStyle) {
        checkRange(start,end);
        if (hasStructure(start,end)) throw new IllegalArgumentException("Range contains structured content; use replaceContent or delete explicitly");
        return replaceContent(start,end,input,insertionStyle);
    }
    public WordDocument replaceContent(int start, int end, String input, WordTextStyle insertionStyle) {
        checkRange(start,end); Objects.requireNonNull(insertionStyle);
        String[] lines = normalize(input).split("\n",-1);
        WordParagraphStyle style = paragraphs.get(paragraphIndex(start)).style();
        List<WordBlock> middle = new ArrayList<>();
        for (String line : lines) middle.add(new WordParagraph(UUID.randomUUID(),List.of(new WordRun(line,insertionStyle)),style));
        return splice(start,end,middle,false,parts);
    }
    public WordDocument delete(int start, int end) {
        checkRange(start,end);
        return splice(start,end,List.of(new WordParagraph(UUID.randomUUID(),List.of(),paragraphs.get(paragraphIndex(start)).style())),false,parts);
    }
    public WordDocument replace(int start, int end, WordDocument fragment) {
        checkRange(start,end); Objects.requireNonNull(fragment);
        List<WordBlock> middle = new ArrayList<>();
        for (WordBlock block : fragment.blocks()) middle.add(renew(block));
        if (!(middle.getFirst() instanceof WordParagraph)) middle.addFirst(new WordParagraph(UUID.randomUUID(),List.of(),paragraphs.get(paragraphIndex(start)).style()));
        WordParts merged = parts.withResources(parts.resources().merge(fragment.resources())).withStyles(parts.styles().merge(fragment.styles()));
        WordNumbering numbering = merged.numbering();
        for (WordListDefinition d : fragment.parts().numbering().lists()) if (numbering.get(d.id()).isEmpty()) numbering = numbering.with(d);
        return splice(start,end,middle,true,merged.withNumbering(numbering));
    }
    public WordDocument insertObject(int offset, WordInlineObject object) {
        checkRange(offset,offset);
        int i = paragraphIndex(offset); WordParagraph p = paragraphs.get(i); int local = offset - starts[i];
        List<WordInline> runs = new ArrayList<>(p.slice(0,local));
        runs.add(new WordObjectRun(object,p.styleAt(Math.max(0,local-1))));
        runs.addAll(p.slice(local,p.length()));
        return replaceParagraphs(Map.of(p.id(),p.withRuns(runs)));
    }
    public WordDocument replaceObject(int offset, WordInlineObject object) {
        WordObjectRun current = objectAt(offset);
        if (current == null) throw new IllegalArgumentException("No object at offset " + offset);
        int i = paragraphIndex(offset); WordParagraph p = paragraphs.get(i); int local = offset - starts[i];
        List<WordInline> runs = new ArrayList<>(p.slice(0,local));
        runs.add(current.withObject(object));
        runs.addAll(p.slice(local+1,p.length()));
        return replaceParagraphs(Map.of(p.id(),p.withRuns(runs)));
    }
    public WordDocument insertBlocks(int offset, List<? extends WordBlock> inserted) {
        checkRange(offset,offset);
        int i = paragraphIndex(offset); WordParagraph p = paragraphs.get(i); int local = offset - starts[i];
        int[] path = paths.get(i); int index = path[path.length-1];
        List<WordBlock> fresh = List.copyOf(inserted);
        return withContainer(path,container -> {
            List<WordBlock> copy = new ArrayList<>(container);
            if (local == 0) copy.addAll(index,fresh);
            else if (local == p.length()) copy.addAll(index+1,fresh);
            else {
                copy.set(index,p.withRuns(p.slice(0,local)).withSectionBreak(null));
                List<WordBlock> tail = new ArrayList<>(fresh);
                tail.add(new WordParagraph(UUID.randomUUID(),p.slice(local,p.length()),p.style(),List.of(),p.sectionBreak()));
                copy.addAll(index+1,tail);
            }
            return copy;
        });
    }
    public WordDocument fragment(int start, int end) {
        checkRange(start,end);
        int first = paragraphIndex(start), last = paragraphIndex(end);
        List<WordBlock> result = new ArrayList<>();
        if (sameContainer(start,end)) {
            int[] a = paths.get(first), b = paths.get(last);
            List<WordBlock> container = container(a);
            int i = a[a.length-1], j = b[b.length-1];
            for (int k = i; k <= j; k++) {
                WordBlock block = container.get(k);
                if (k == i || k == j) {
                    WordParagraph p = (WordParagraph)block;
                    int from = k == i ? start - starts[first] : 0, to = k == j ? end - starts[last] : p.length();
                    result.add(new WordParagraph(UUID.randomUUID(),p.slice(from,to),p.style()));
                } else result.add(renew(block));
            }
        } else {
            for (int k = first; k <= last; k++) {
                WordParagraph p = paragraphs.get(k); int from = k == first ? start - starts[k] : 0, to = k == last ? end - starts[k] : p.length();
                result.add(new WordParagraph(UUID.randomUUID(),p.slice(from,to),p.style()));
            }
        }
        return new WordDocument(result,pageSettings,parts.withHeaders(WordHeaders.EMPTY).withComments(List.of()));
    }
    public WordDocument format(int start, int end, UnaryOperator<WordTextStyle> formatter) {
        checkRange(start,end); if (start == end) return this;
        Map<UUID,WordParagraph> changed = new HashMap<>();
        for (int k = paragraphIndex(start); k <= paragraphIndex(end); k++) {
            WordParagraph p = paragraphs.get(k);
            int from = Math.max(0,start - starts[k]), to = Math.min(p.length(),end - starts[k]);
            if (from >= to) continue;
            List<WordInline> runs = new ArrayList<>(p.slice(0,from));
            for (WordInline run : p.slice(from,to)) runs.add(run.withStyle(formatter.apply(run.style())));
            runs.addAll(p.slice(to,p.length()));
            changed.put(p.id(),p.withRuns(runs));
        }
        return replaceParagraphs(changed);
    }
    public WordDocument formatParagraphs(int start, int end, UnaryOperator<WordParagraphStyle> formatter) {
        checkRange(start,end);
        int first = paragraphIndex(start), last = paragraphIndex(end > start ? end-1 : end);
        Map<UUID,WordParagraph> changed = new HashMap<>();
        for (int i = first; i <= last; i++) changed.put(paragraphs.get(i).id(),paragraphs.get(i).withStyle(formatter.apply(paragraphs.get(i).style())));
        return replaceParagraphs(changed);
    }
    public WordDocument mapParagraphs(int start, int end, UnaryOperator<WordParagraph> mapper) {
        checkRange(start,end);
        int first = paragraphIndex(start), last = paragraphIndex(end > start ? end-1 : end);
        Map<UUID,WordParagraph> changed = new HashMap<>();
        for (int i = first; i <= last; i++) changed.put(paragraphs.get(i).id(),Objects.requireNonNull(mapper.apply(paragraphs.get(i))));
        return replaceParagraphs(changed);
    }
    public WordDocument replaceParagraphs(Map<UUID,WordParagraph> replacements) {
        if (replacements.isEmpty()) return this;
        return transformBlocks(block -> block instanceof WordParagraph p && replacements.containsKey(p.id()) ? List.of(replacements.get(p.id())) : null);
    }
    public WordDocument mapAllParagraphs(UnaryOperator<WordParagraph> mapper) {
        Map<UUID,WordParagraph> changed = new HashMap<>();
        for (WordParagraph p : paragraphs) { WordParagraph next = mapper.apply(p); if (!next.equals(p)) changed.put(p.id(),next); }
        return replaceParagraphs(changed);
    }
    public WordDocument replaceBlock(UUID id, List<? extends WordBlock> replacement) {
        List<WordBlock> value = List.copyOf(replacement);
        boolean[] found = {false};
        WordDocument result = transformBlocks(block -> { if (block.id().equals(id)) { found[0] = true; return value; } return null; });
        if (!found[0]) throw new IllegalArgumentException("Unknown block " + id);
        return result;
    }
    public WordDocument updateTable(UUID id, UnaryOperator<WordTable> operation) {
        WordTable table = findTable(id).orElseThrow(() -> new IllegalArgumentException("Unknown table " + id));
        return replaceBlock(id,List.of(Objects.requireNonNull(operation.apply(table))));
    }
    public Optional<WordTable> findTable(UUID id) {
        int[] path = tablePaths.get(id);
        if (path == null) return Optional.empty();
        return Optional.of((WordTable)container(path).get(path[path.length-1]));
    }
    public List<WordTable> tables() {
        List<WordTable> result = new ArrayList<>();
        tablePaths.values().stream().sorted(Arrays::compare).forEach(p -> result.add((WordTable)container(p).get(p[p.length-1])));
        return result;
    }
    public Optional<WordTableLocation> tableAt(int offset) {
        int[] path = paths.get(paragraphIndex(offset));
        return path.length < 4 ? Optional.empty() : tableAt(offset,(path.length-1)/3-1);
    }
    public Optional<WordTableLocation> tableAt(int offset, int depth) {
        int[] path = paths.get(paragraphIndex(offset));
        if (depth < 0 || path.length < 3*depth+4) return Optional.empty();
        int[] tablePath = Arrays.copyOf(path,3*depth+1);
        WordTable table = (WordTable)container(tablePath).get(tablePath[tablePath.length-1]);
        int row = path[3*depth+1], cell = path[3*depth+2];
        return Optional.of(new WordTableLocation(table,row,cell,table.rows().get(row).columnOf(cell)));
    }
    public int[] cellRange(UUID tableId, int row, int cell) {
        int[] tablePath = tablePaths.get(tableId);
        if (tablePath == null) throw new IllegalArgumentException("Unknown table");
        int[] prefix = Arrays.copyOf(tablePath,tablePath.length+2); prefix[tablePath.length] = row; prefix[tablePath.length+1] = cell;
        int first = -1, last = -1;
        for (int i = 0; i < paths.size(); i++) {
            int[] p = paths.get(i);
            if (p.length > prefix.length && Arrays.equals(p,0,prefix.length,prefix,0,prefix.length)) { if (first < 0) first = i; last = i; }
        }
        if (first < 0) throw new IllegalArgumentException("Unknown cell");
        return new int[]{starts[first],paragraphEnd(last)};
    }
    public int[] tableRange(UUID tableId) {
        WordTable table = findTable(tableId).orElseThrow();
        int[] first = cellRange(tableId,0,0);
        int lastRow = table.rows().size()-1;
        int[] last = cellRange(tableId,lastRow,table.rows().get(lastRow).cells().size()-1);
        return new int[]{first[0],last[1]};
    }
    public Set<String> usedResourceIds() {
        Set<String> ids = new HashSet<>();
        for (WordObjectRef ref : objects()) {
            if (ref.object() instanceof WordImage image) ids.add(image.resourceId());
            if (ref.object() instanceof WordCustomObject custom && custom.previewResourceId() != null) ids.add(custom.previewResourceId());
        }
        for (List<WordBlock> part : parts.headers().parts().values()) collectResources(part,ids);
        return ids;
    }
    private static void collectResources(List<WordBlock> blocks, Set<String> ids) {
        for (WordBlock b : blocks) {
            if (b instanceof WordParagraph p) for (WordInline i : p.runs()) if (i instanceof WordObjectRun o && o.object() instanceof WordImage image) ids.add(image.resourceId());
            if (b instanceof WordTable t) for (WordTableRow r : t.rows()) for (WordTableCell c : r.cells()) collectResources(c.blocks(),ids);
        }
    }

    private List<WordBlock> container(int[] path) {
        List<WordBlock> current = blocks;
        for (int d = 0; d < path.length - 1; d += 3) {
            WordTable t = (WordTable)current.get(path[d]);
            current = t.rows().get(path[d+1]).cells().get(path[d+2]).blocks();
        }
        return current;
    }
    private WordDocument withContainer(int[] paragraphPath, UnaryOperator<List<WordBlock>> operation) {
        return new WordDocument(update(blocks,paragraphPath,0,operation),pageSettings,parts);
    }
    private static List<WordBlock> update(List<WordBlock> container, int[] path, int depth, UnaryOperator<List<WordBlock>> operation) {
        if (depth + 1 >= path.length) return operation.apply(container);
        WordTable table = (WordTable)container.get(path[depth]);
        int r = path[depth+1], c = path[depth+2];
        WordTableRow row = table.rows().get(r); WordTableCell cell = row.cells().get(c);
        List<WordBlock> copy = new ArrayList<>(container);
        copy.set(path[depth],table.withRow(r,row.withCell(c,cell.withBlocks(update(cell.blocks(),path,depth+3,operation)))));
        return copy;
    }
    private WordDocument splice(int start, int end, List<WordBlock> middle, boolean ownStyles, WordParts nextParts) {
        int first = paragraphIndex(start), last = paragraphIndex(end);
        int[] a = paths.get(first), b = paths.get(last);
        if (!sameContainer(start,end)) throw new IllegalArgumentException("Range crosses table cells; use cell operations");
        WordParagraph pa = paragraphs.get(first), pb = paragraphs.get(last);
        List<WordInline> prefix = pa.slice(0,start - starts[first]), suffix = pb.slice(end - starts[last],pb.length());
        int i = a[a.length-1], j = b[b.length-1];
        List<WordBlock> merged = new ArrayList<>();
        if (!(middle.getLast() instanceof WordParagraph)) { middle = new ArrayList<>(middle); middle.add(new WordParagraph(UUID.randomUUID(),List.of(),pa.style())); }
        for (int k = 0; k < middle.size(); k++) {
            WordBlock block = middle.get(k);
            boolean isFirst = k == 0, isLast = k == middle.size()-1;
            if (!isFirst && !isLast) { merged.add(block); continue; }
            WordParagraph p = (WordParagraph)block;
            List<WordInline> runs = new ArrayList<>();
            if (isFirst) runs.addAll(prefix);
            runs.addAll(p.runs());
            if (isLast) runs.addAll(suffix);
            WordParagraphStyle style = isFirst ? pa.style() : ownStyles ? p.style() : pa.style();
            merged.add(new WordParagraph(isFirst ? pa.id() : p.id(),runs,style,isFirst ? pa.bookmarks() : p.bookmarks(),isLast ? pb.sectionBreak() : null));
        }
        List<WordBlock> result = update(blocks,a,0,container -> {
            List<WordBlock> copy = new ArrayList<>(container.subList(0,i));
            copy.addAll(merged);
            copy.addAll(container.subList(j+1,container.size()));
            return copy;
        });
        return new WordDocument(result,pageSettings,nextParts);
    }
    private WordDocument transformBlocks(Function<WordBlock,List<WordBlock>> operation) {
        List<WordBlock> next = transform(blocks,operation);
        return next == blocks ? this : new WordDocument(next,pageSettings,parts);
    }
    private static List<WordBlock> transform(List<WordBlock> container, Function<WordBlock,List<WordBlock>> operation) {
        List<WordBlock> result = new ArrayList<>(); boolean changed = false;
        for (WordBlock block : container) {
            List<WordBlock> replaced = operation.apply(block);
            if (replaced != null) { result.addAll(replaced); changed = true; continue; }
            if (block instanceof WordTable table) {
                List<WordTableRow> rows = new ArrayList<>(); boolean tableChanged = false;
                for (WordTableRow row : table.rows()) {
                    List<WordTableCell> cells = new ArrayList<>(); boolean rowChanged = false;
                    for (WordTableCell cell : row.cells()) {
                        List<WordBlock> inner = transform(cell.blocks(),operation);
                        if (inner != cell.blocks()) { cells.add(cell.withBlocks(inner)); rowChanged = true; } else cells.add(cell);
                    }
                    rows.add(rowChanged ? row.withCells(cells) : row); tableChanged |= rowChanged;
                }
                if (tableChanged) { result.add(table.withRows(rows)); changed = true; continue; }
            }
            result.add(block);
        }
        return changed ? result : container;
    }
    public static WordBlock renew(WordBlock block) {
        return switch (block) {
            case WordParagraph p -> {
                List<WordInline> runs = new ArrayList<>();
                for (WordInline inline : p.runs()) runs.add(inline instanceof WordObjectRun o ? o.withObject(o.object().withId(WordIds.next())) : inline);
                yield new WordParagraph(UUID.randomUUID(),runs,p.style(),List.of(),p.sectionBreak());
            }
            case WordTable t -> {
                List<WordTableRow> rows = new ArrayList<>();
                for (WordTableRow row : t.rows()) {
                    List<WordTableCell> cells = new ArrayList<>();
                    for (WordTableCell cell : row.cells()) cells.add(cell.withId(UUID.randomUUID()).withBlocks(cell.blocks().stream().map(WordDocument::renew).toList()));
                    rows.add(row.withId(UUID.randomUUID()).withCells(cells));
                }
                yield t.withId(UUID.randomUUID()).withRows(rows);
            }
            case WordOpaqueBlock o -> new WordOpaqueBlock(UUID.randomUUID(),o.label(),o.xml(),o.previewText());
            case WordTableOfContents toc -> new WordTableOfContents(UUID.randomUUID(),toc.title(),toc.maxLevel());
            default -> block;
        };
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof WordDocument d && d.hashCode() == hashCode() && d.blocks.equals(blocks) && d.pageSettings.equals(pageSettings) && d.parts.equals(parts);
    }
    @Override public int hashCode() {
        int h = hash;
        if (h == 0) { h = Objects.hash(blocks,pageSettings,parts); hash = h == 0 ? 1 : h; }
        return hash;
    }
    @Override public String toString() { return "WordDocument[" + paragraphs.size() + " paragraphs, " + length() + " chars]"; }
}
