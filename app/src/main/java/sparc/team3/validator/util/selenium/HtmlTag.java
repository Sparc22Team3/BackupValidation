package sparc.team3.validator.util.selenium;

public class HtmlTag{
    String tag;
    String value;
    String entrypoint;

    public HtmlTag(String tag, String value, String entrypoint) {
        this.tag = tag;
        this.value = value;
        this.entrypoint = entrypoint;
    }

    public static HtmlTagBuilder builder() {
        return new HtmlTagBuilder();
    }

    public String getTag() {
        return this.tag;
    }

    public String getValue() {
        return this.value;
    }

    public String getEntrypoint() {
        return this.entrypoint;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setEntrypoint(String entrypoint) {
        this.entrypoint = entrypoint;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof HtmlTag)) return false;
        final HtmlTag other = (HtmlTag) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$tag = this.getTag();
        final Object other$tag = other.getTag();
        if (this$tag == null ? other$tag != null : !this$tag.equals(other$tag)) return false;
        final Object this$value = this.getValue();
        final Object other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
        final Object this$entrypoint = this.getEntrypoint();
        final Object other$entrypoint = other.getEntrypoint();
        if (this$entrypoint == null ? other$entrypoint != null : !this$entrypoint.equals(other$entrypoint))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof HtmlTag;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $tag = this.getTag();
        result = result * PRIME + ($tag == null ? 43 : $tag.hashCode());
        final Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 43 : $value.hashCode());
        final Object $entrypoint = this.getEntrypoint();
        result = result * PRIME + ($entrypoint == null ? 43 : $entrypoint.hashCode());
        return result;
    }

    public String toString() {
        return "HtmlTag(tag=" + this.getTag() + ", value=" + this.getValue() + ", entrypoint=" + this.getEntrypoint() + ")";
    }

    public static class HtmlTagBuilder {
        private String tag;
        private String value;
        private String entrypoint;

        HtmlTagBuilder() {
        }

        public HtmlTagBuilder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public HtmlTagBuilder value(String value) {
            this.value = value;
            return this;
        }

        public HtmlTagBuilder entrypoint(String entrypoint) {
            this.entrypoint = entrypoint;
            return this;
        }

        public HtmlTag build() {
            return new HtmlTag(tag, value, entrypoint);
        }

        public String toString() {
            return "HtmlTag.HtmlTagBuilder(tag=" + this.tag + ", value=" + this.value + ", entrypoint=" + this.entrypoint + ")";
        }
    }
}
