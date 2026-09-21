package dtm.stools.defaults;

import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.utils.ImageUtils;

import javax.swing.Icon;
import java.util.EnumMap;
import java.util.Map;

/** Built-in icons used when a completion item does not provide one. */
public final class AutoCompleteIcons {
    private static final String ROOT = "drawables/autocomplete/";
    private static final Map<Style, Map<AutoCompleteItem.Kind, Icon>> ICON_SETS = Map.of(
            Style.INTELLIJ, loadIcons(""),
            Style.VISUAL_STUDIO_CODE, loadIcons("vscode/")
    );

    public enum Style {
        INTELLIJ,
        VISUAL_STUDIO_CODE
    }

    private AutoCompleteIcons() {
        throw new AssertionError("Utility class");
    }

    public static Icon forKind(AutoCompleteItem.Kind kind) {
        return forKind(Style.INTELLIJ, kind);
    }

    public static Icon forKind(Style style, AutoCompleteItem.Kind kind) {
        AutoCompleteItem.Kind resolvedKind = kind == null ? AutoCompleteItem.Kind.TEXT : kind;
        Map<AutoCompleteItem.Kind, Icon> icons = ICON_SETS.get(style == null ? Style.INTELLIJ : style);
        return icons.getOrDefault(resolvedKind, icons.get(AutoCompleteItem.Kind.TEXT));
    }

    private static Map<AutoCompleteItem.Kind, Icon> loadIcons(String stylePath) {
        EnumMap<AutoCompleteItem.Kind, Icon> icons = new EnumMap<>(AutoCompleteItem.Kind.class);
        for (AutoCompleteItem.Kind kind : AutoCompleteItem.Kind.values()) {
            String fileName = switch (kind) {
                case METHOD, FUNCTION -> "method.svg";
                case CONSTRUCTOR -> "constructor.svg";
                case FIELD, PROPERTY -> "field.svg";
                case VARIABLE, PARAMETER, VALUE, UNIT -> "variable.svg";
                case CLASS -> "class.svg";
                case INTERFACE -> "interface.svg";
                case MODULE -> "module.svg";
                case ENUM, ENUM_MEMBER -> "enum.svg";
                case KEYWORD, OPERATOR -> "keyword.svg";
                case SNIPPET -> "snippet.svg";
                case COLOR -> "color.svg";
                case FILE, REFERENCE -> "file.svg";
                case FOLDER -> "folder.svg";
                case CONSTANT -> "constant.svg";
                case STRUCT, TYPE_PARAMETER -> "type.svg";
                case EVENT -> "event.svg";
                case TEXT -> "text.svg";
            };
            icons.put(kind, ImageUtils.getIconByResourceOrThrow(
                    AutoCompleteIcons.class, ROOT + stylePath + fileName));
        }
        return Map.copyOf(icons);
    }
}
