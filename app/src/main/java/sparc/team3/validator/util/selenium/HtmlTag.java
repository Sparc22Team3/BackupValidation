package sparc.team3.validator.util.selenium;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class HtmlTag implements Comparable<HtmlTag> {
    String tagID;
    String value;
    String entrypoint;

    public HtmlTag() {
    }

    public HtmlTag(String tagID, String value, String entrypoint) {
        this.tagID = tagID;
        this.value = value;
        this.entrypoint = entrypoint;
    }

    public String getTagID() {
        return this.tagID;
    }

    public void setTagID(String tagID) {
        this.tagID = tagID;
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
        if (tagID.compareTo(o.tagID) == 0) {
            if (value.compareTo(o.value) == 0) {
                return entrypoint.compareTo(o.entrypoint);
            }
            return value.compareTo(o.value);
        }
        return tagID.compareTo(o.tagID);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HtmlTag htmlTag = (HtmlTag) o;

        if (!Objects.equals(tagID, htmlTag.tagID)) return false;
        if (!Objects.equals(value, htmlTag.value)) return false;
        return Objects.equals(entrypoint, htmlTag.entrypoint);
    }

    @Override
    public int hashCode() {
        int result = tagID != null ? tagID.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (entrypoint != null ? entrypoint.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HtmlTag{" +
                "tagID='" + tagID + '\'' +
                ", value='" + value + '\'' +
                ", entrypoint='" + entrypoint + '\'' +
                ", complete=" + isComplete() +
                '}';
    }

    @JsonIgnore
    public boolean isComplete() {
        return tagID != null && value != null && entrypoint != null;
    }
}
