package dtm.stools.i18n;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record I18nLocaleElements(Locale locale, Set<I18nElement> elements) {
    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        I18nLocaleElements that = (I18nLocaleElements) object;
        return Objects.equals(locale, that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(locale);
    }
}
