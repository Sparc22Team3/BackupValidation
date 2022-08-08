package sparc.team3.validator.util.selenium;

import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class SeleniumSettings {
    Map<String, String> titles;
    Set<HtmlTag> tags;
    Set<Login> logins;
    Set<SearchTerm> searchTerms;

    public SeleniumSettings() {
    }

    public SeleniumSettings(Map<String, String> titles, Set<HtmlTag> tags, Set<Login> logins, Set<SearchTerm> searchTerms) {
        this.titles = titles;
        this.tags = tags;
        this.logins = logins;
        this.searchTerms = searchTerms;
    }

    public Map<String, String> getTitles() {
        return this.titles;
    }

    public void setTitles(Map<String, String> titles) {
        this.titles = titles;
    }

    public Set<HtmlTag> getTags() {
        return this.tags;
    }

    public void setTags(Set<HtmlTag> tags) {
        this.tags = tags;
    }

    public Set<Login> getLogins() {
        return this.logins;
    }

    public void setLogins(Set<Login> logins) {
        this.logins = logins;
    }

    public Set<SearchTerm> getSearchTerms() {
        return this.searchTerms;
    }

    public void setSearchTerms(Set<SearchTerm> searchTerms) {
        this.searchTerms = searchTerms;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SeleniumSettings that = (SeleniumSettings) o;

        if (!Objects.equals(titles, that.titles)) return false;
        if (!Objects.equals(tags, that.tags)) return false;
        if (!Objects.equals(logins, that.logins)) return false;
        return Objects.equals(searchTerms, that.searchTerms);
    }

    @Override
    public int hashCode() {
        int result = titles != null ? titles.hashCode() : 0;
        result = 31 * result + (tags != null ? tags.hashCode() : 0);
        result = 31 * result + (logins != null ? logins.hashCode() : 0);
        result = 31 * result + (searchTerms != null ? searchTerms.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SeleniumSettings{" +
                "titles=" + titles +
                ", tags=" + tags +
                ", logins=" + logins +
                ", searchTerms=" + searchTerms +
                '}';
    }
}
