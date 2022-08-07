package sparc.team3.validator.util.selenium;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class SearchTerm implements Comparable<SearchTerm> {
    String term;
    String searchCssSelector;
    String entrypoint;

    public SearchTerm() {
    }

    public SearchTerm(String term, String searchCssSelector, String entrypoint) {
        this.term = term;
        this.searchCssSelector = searchCssSelector;
        this.entrypoint = entrypoint;
    }

    public String getTerm() {
        return this.term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getSearchCssSelector() {
        return this.searchCssSelector;
    }

    public void setSearchCssSelector(String searchCssSelector) {
        this.searchCssSelector = searchCssSelector;
    }

    public String getEntrypoint() {
        return this.entrypoint;
    }

    public void setEntrypoint(String entrypoint) {
        this.entrypoint = entrypoint;
    }

    @Override
    public int compareTo(SearchTerm o) {
        if (term.compareTo(o.term) == 0) {
            if (searchCssSelector.compareTo(o.searchCssSelector) == 0) {
                return entrypoint.compareTo(o.entrypoint);
            }
            return searchCssSelector.compareTo(o.searchCssSelector);
        }
        return term.compareTo(o.term);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchTerm that = (SearchTerm) o;

        if (!Objects.equals(term, that.term)) return false;
        if (!Objects.equals(searchCssSelector, that.searchCssSelector))
            return false;
        return Objects.equals(entrypoint, that.entrypoint);
    }

    @Override
    public int hashCode() {
        int result = term != null ? term.hashCode() : 0;
        result = 31 * result + (searchCssSelector != null ? searchCssSelector.hashCode() : 0);
        result = 31 * result + (entrypoint != null ? entrypoint.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SearchTerm{" +
                "term='" + term + '\'' +
                ", searchCssSelector='" + searchCssSelector + '\'' +
                ", entrypoint='" + entrypoint + '\'' +
                ", complete=" + isComplete() +
                '}';
    }

    @JsonIgnore
    public boolean isComplete() {
        return term != null && searchCssSelector != null && entrypoint != null;
    }
}
