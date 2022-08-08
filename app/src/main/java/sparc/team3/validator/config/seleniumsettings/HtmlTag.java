package sparc.team3.validator.config.seleniumsettings;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class HtmlTag implements Comparable<HtmlTag> {
    String cssSelector;
    String value;
    String entrypoint;

    public HtmlTag() {
    }

    public HtmlTag(String cssSelector, String value, String entrypoint) {
        this.cssSelector = cssSelector;
        this.value = value;
        this.entrypoint = entrypoint;
    }

    public String getCssSelector() {
        return this.cssSelector;
    }

    public void setCssSelector(String cssSelector) {
        this.cssSelector = cssSelector;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getEntrypoint() {
        return this.entrypoint;
    }

    public void setEntrypoint(String entrypoint) {
        this.entrypoint = entrypoint;
    }

    @Override
    public int compareTo(HtmlTag o) {
        if (cssSelector.compareTo(o.cssSelector) == 0) {
            if (value.compareTo(o.value) == 0) {
                return entrypoint.compareTo(o.entrypoint);
            }
            return value.compareTo(o.value);
        }
        return cssSelector.compareTo(o.cssSelector);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HtmlTag htmlTag = (HtmlTag) o;

        if (!Objects.equals(cssSelector, htmlTag.cssSelector)) return false;
        if (!Objects.equals(value, htmlTag.value)) return false;
        return Objects.equals(entrypoint, htmlTag.entrypoint);
    }

    @Override
    public int hashCode() {
        int result = cssSelector != null ? cssSelector.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (entrypoint != null ? entrypoint.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HtmlTag{" +
                "cssSelector='" + cssSelector + '\'' +
                ", value='" + value + '\'' +
                ", entrypoint='" + entrypoint + '\'' +
                ", complete=" + isComplete() +
                '}';
    }

    @JsonIgnore
    public boolean isComplete() {
        return cssSelector != null && value != null && entrypoint != null;
    }
}
