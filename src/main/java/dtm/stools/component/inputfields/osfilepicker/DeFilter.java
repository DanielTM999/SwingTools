package dtm.stools.component.inputfields.osfilepicker;

public record DeFilter(String name, String[] ext) {

    public DeFilter {
        if (name == null) name = "";
        if (ext == null) ext = new String[0];
    }

    public static DeFilter of(String name, String... ext) {
        return new DeFilter(name, ext);
    }
}
