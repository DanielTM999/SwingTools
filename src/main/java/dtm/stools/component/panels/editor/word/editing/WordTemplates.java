package dtm.stools.component.panels.editor.word.editing;

import dtm.stools.component.panels.editor.word.model.WordDocument;
import java.util.*;
import java.util.regex.*;

public final class WordTemplates {
    private static final Pattern VARIABLE=Pattern.compile("\\$\\{([\\p{L}\\p{N}_.-]+)}");
    private record Replacement(int start,int end,String text){}
    private WordTemplates(){}
    public static Set<String> variables(WordDocument document){
        Set<String> result=new LinkedHashSet<>();Matcher matcher=VARIABLE.matcher(document.text());while(matcher.find())result.add(matcher.group(1));return Collections.unmodifiableSet(result);
    }
    public static WordDocument fill(WordDocument template,Map<String,String> values){
        Objects.requireNonNull(values);List<Replacement> replacements=new ArrayList<>();Matcher matcher=VARIABLE.matcher(template.text());
        while(matcher.find()){
            String key=matcher.group(1);if(!values.containsKey(key))throw new IllegalArgumentException("Missing template variable: "+key);
            replacements.add(new Replacement(matcher.start(),matcher.end(),Objects.requireNonNull(values.get(key),key)));
        }
        WordDocument result=template;for(int i=replacements.size()-1;i>=0;i--){var r=replacements.get(i);result=result.replace(r.start,r.end,r.text,result.styleAt(r.start));}return result;
    }
}
