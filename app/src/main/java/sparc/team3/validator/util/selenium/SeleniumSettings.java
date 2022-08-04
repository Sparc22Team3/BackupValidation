package sparc.team3.validator.util.selenium;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;


public class SeleniumSettings {
    Map<String, String> titles;
    Set<HtmlTag> tags;
    Set<Login> logins;
    Set<SearchTerm> searchTerms;

    SeleniumSettings(Map<String, String> titles, Set<HtmlTag> tags, Set<Login> logins, Set<SearchTerm> searchTerms) {
        this.titles = titles;
        this.tags = tags;
        this.logins = logins;
        this.searchTerms = searchTerms;
    }

    public static SeleniumSettingsBuilder builder() {
        return new SeleniumSettingsBuilder();
    }

    public Map<String, String> getTitles() {
        return this.titles;
    }

    public Set<HtmlTag> getTags() {
        return this.tags;
    }

    public Set<Login> getLogins() {
        return this.logins;
    }

    public Set<SearchTerm> getSearchTerms() {
        return this.searchTerms;
    }

    public void setTitles(Map<String, String> titles) {
        this.titles = titles;
    }

    public void setTags(Set<HtmlTag> tags) {
        this.tags = tags;
    }

    public void setLogins(Set<Login> logins) {
        this.logins = logins;
    }

    public void setSearchTerms(Set<SearchTerm> searchTerms) {
        this.searchTerms = searchTerms;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SeleniumSettings)) return false;
        final SeleniumSettings other = (SeleniumSettings) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$titles = this.getTitles();
        final Object other$titles = other.getTitles();
        if (this$titles == null ? other$titles != null : !this$titles.equals(other$titles)) return false;
        final Object this$tags = this.getTags();
        final Object other$tags = other.getTags();
        if (this$tags == null ? other$tags != null : !this$tags.equals(other$tags)) return false;
        final Object this$logins = this.getLogins();
        final Object other$logins = other.getLogins();
        if (this$logins == null ? other$logins != null : !this$logins.equals(other$logins)) return false;
        final Object this$searchTerms = this.getSearchTerms();
        final Object other$searchTerms = other.getSearchTerms();
        if (this$searchTerms == null ? other$searchTerms != null : !this$searchTerms.equals(other$searchTerms))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SeleniumSettings;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $titles = this.getTitles();
        result = result * PRIME + ($titles == null ? 43 : $titles.hashCode());
        final Object $tags = this.getTags();
        result = result * PRIME + ($tags == null ? 43 : $tags.hashCode());
        final Object $logins = this.getLogins();
        result = result * PRIME + ($logins == null ? 43 : $logins.hashCode());
        final Object $searchTerms = this.getSearchTerms();
        result = result * PRIME + ($searchTerms == null ? 43 : $searchTerms.hashCode());
        return result;
    }

    public String toString() {
        return "SeleniumSettings(titles=" + this.getTitles() + ", tags=" + this.getTags() + ", logins=" + this.getLogins() + ", searchTerms=" + this.getSearchTerms() + ")";
    }

    public static class SeleniumSettingsBuilder {
        private ArrayList<String> titles$key;
        private ArrayList<String> titles$value;
        private ArrayList<HtmlTag> tags;
        private ArrayList<Login> logins;
        private ArrayList<SearchTerm> searchTerms;

        SeleniumSettingsBuilder() {
        }

        public SeleniumSettingsBuilder title(String titleKey, String titleValue) {
            if (this.titles$key == null) {
                this.titles$key = new ArrayList<String>();
                this.titles$value = new ArrayList<String>();
            }
            this.titles$key.add(titleKey);
            this.titles$value.add(titleValue);
            return this;
        }

        public SeleniumSettingsBuilder titles(Map<? extends String, ? extends String> titles) {
            if (this.titles$key == null) {
                this.titles$key = new ArrayList<String>();
                this.titles$value = new ArrayList<String>();
            }
            for (final Map.Entry<? extends String, ? extends String> $lombokEntry : titles.entrySet()) {
                this.titles$key.add($lombokEntry.getKey());
                this.titles$value.add($lombokEntry.getValue());
            }
            return this;
        }

        public SeleniumSettingsBuilder clearTitles() {
            if (this.titles$key != null) {
                this.titles$key.clear();
                this.titles$value.clear();
            }
            return this;
        }

        public SeleniumSettingsBuilder tag(HtmlTag tag) {
            if (this.tags == null) this.tags = new ArrayList<HtmlTag>();
            this.tags.add(tag);
            return this;
        }

        public SeleniumSettingsBuilder tags(Collection<? extends HtmlTag> tags) {
            if (this.tags == null) this.tags = new ArrayList<HtmlTag>();
            this.tags.addAll(tags);
            return this;
        }

        public SeleniumSettingsBuilder clearTags() {
            if (this.tags != null)
                this.tags.clear();
            return this;
        }

        public SeleniumSettingsBuilder login(Login login) {
            if (this.logins == null) this.logins = new ArrayList<Login>();
            this.logins.add(login);
            return this;
        }

        public SeleniumSettingsBuilder logins(Collection<? extends Login> logins) {
            if (this.logins == null) this.logins = new ArrayList<Login>();
            this.logins.addAll(logins);
            return this;
        }

        public SeleniumSettingsBuilder clearLogins() {
            if (this.logins != null)
                this.logins.clear();
            return this;
        }

        public SeleniumSettingsBuilder searchTerm(SearchTerm searchTerm) {
            if (this.searchTerms == null) this.searchTerms = new ArrayList<SearchTerm>();
            this.searchTerms.add(searchTerm);
            return this;
        }

        public SeleniumSettingsBuilder searchTerms(Collection<? extends SearchTerm> searchTerms) {
            if (this.searchTerms == null) this.searchTerms = new ArrayList<SearchTerm>();
            this.searchTerms.addAll(searchTerms);
            return this;
        }

        public SeleniumSettingsBuilder clearSearchTerms() {
            if (this.searchTerms != null)
                this.searchTerms.clear();
            return this;
        }

        public SeleniumSettings build() {
            Map<String, String> titles;
            switch (this.titles$key == null ? 0 : this.titles$key.size()) {
                case 0:
                    titles = java.util.Collections.emptyMap();
                    break;
                case 1:
                    titles = java.util.Collections.singletonMap(this.titles$key.get(0), this.titles$value.get(0));
                    break;
                default:
                    titles = new java.util.LinkedHashMap<String, String>(this.titles$key.size() < 1073741824 ? 1 + this.titles$key.size() + (this.titles$key.size() - 3) / 3 : Integer.MAX_VALUE);
                    for (int $i = 0; $i < this.titles$key.size(); $i++)
                        titles.put(this.titles$key.get($i), (String) this.titles$value.get($i));
                    titles = java.util.Collections.unmodifiableMap(titles);
            }
            Set<HtmlTag> tags;
            switch (this.tags == null ? 0 : this.tags.size()) {
                case 0:
                    tags = java.util.Collections.emptySet();
                    break;
                case 1:
                    tags = java.util.Collections.singleton(this.tags.get(0));
                    break;
                default:
                    tags = new java.util.LinkedHashSet<HtmlTag>(this.tags.size() < 1073741824 ? 1 + this.tags.size() + (this.tags.size() - 3) / 3 : Integer.MAX_VALUE);
                    tags.addAll(this.tags);
                    tags = java.util.Collections.unmodifiableSet(tags);
            }
            Set<Login> logins;
            switch (this.logins == null ? 0 : this.logins.size()) {
                case 0:
                    logins = java.util.Collections.emptySet();
                    break;
                case 1:
                    logins = java.util.Collections.singleton(this.logins.get(0));
                    break;
                default:
                    logins = new java.util.LinkedHashSet<Login>(this.logins.size() < 1073741824 ? 1 + this.logins.size() + (this.logins.size() - 3) / 3 : Integer.MAX_VALUE);
                    logins.addAll(this.logins);
                    logins = java.util.Collections.unmodifiableSet(logins);
            }
            Set<SearchTerm> searchTerms;
            switch (this.searchTerms == null ? 0 : this.searchTerms.size()) {
                case 0:
                    searchTerms = java.util.Collections.emptySet();
                    break;
                case 1:
                    searchTerms = java.util.Collections.singleton(this.searchTerms.get(0));
                    break;
                default:
                    searchTerms = new java.util.LinkedHashSet<SearchTerm>(this.searchTerms.size() < 1073741824 ? 1 + this.searchTerms.size() + (this.searchTerms.size() - 3) / 3 : Integer.MAX_VALUE);
                    searchTerms.addAll(this.searchTerms);
                    searchTerms = java.util.Collections.unmodifiableSet(searchTerms);
            }

            return new SeleniumSettings(titles, tags, logins, searchTerms);
        }

        public String toString() {
            return "SeleniumSettings.SeleniumSettingsBuilder(titles$key=" + this.titles$key + ", titles$value=" + this.titles$value + ", tags=" + this.tags + ", logins=" + this.logins + ", searchTerms=" + this.searchTerms + ")";
        }
    }
}
