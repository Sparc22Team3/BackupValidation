package sparc.team3.validator.util.selenium;

public class SearchTerm{
    String term;
    String searchFieldID;
    String entrypoint;

    public SearchTerm(String term, String searchFieldID, String entrypoint) {
        this.term = term;
        this.searchFieldID = searchFieldID;
        this.entrypoint = entrypoint;
    }

    public static SearchTermBuilder builder() {
        return new SearchTermBuilder();
    }

    public String getTerm() {
        return this.term;
    }

    public String getSearchFieldID() {
        return this.searchFieldID;
    }

    public String getEntrypoint() {
        return this.entrypoint;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public void setSearchFieldID(String searchFieldID) {
        this.searchFieldID = searchFieldID;
    }

    public void setEntrypoint(String entrypoint) {
        this.entrypoint = entrypoint;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof SearchTerm)) return false;
        final SearchTerm other = (SearchTerm) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$term = this.getTerm();
        final Object other$term = other.getTerm();
        if (this$term == null ? other$term != null : !this$term.equals(other$term)) return false;
        final Object this$searchFieldID = this.getSearchFieldID();
        final Object other$searchFieldID = other.getSearchFieldID();
        if (this$searchFieldID == null ? other$searchFieldID != null : !this$searchFieldID.equals(other$searchFieldID))
            return false;
        final Object this$entrypoint = this.getEntrypoint();
        final Object other$entrypoint = other.getEntrypoint();
        if (this$entrypoint == null ? other$entrypoint != null : !this$entrypoint.equals(other$entrypoint))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof SearchTerm;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $term = this.getTerm();
        result = result * PRIME + ($term == null ? 43 : $term.hashCode());
        final Object $searchFieldID = this.getSearchFieldID();
        result = result * PRIME + ($searchFieldID == null ? 43 : $searchFieldID.hashCode());
        final Object $entrypoint = this.getEntrypoint();
        result = result * PRIME + ($entrypoint == null ? 43 : $entrypoint.hashCode());
        return result;
    }

    public String toString() {
        return "SearchTerm(term=" + this.getTerm() + ", searchFieldID=" + this.getSearchFieldID() + ", entrypoint=" + this.getEntrypoint() + ")";
    }

    public static class SearchTermBuilder {
        private String term;
        private String searchFieldID;
        private String entrypoint;

        SearchTermBuilder() {
        }

        public SearchTermBuilder term(String term) {
            this.term = term;
            return this;
        }

        public SearchTermBuilder searchFieldID(String searchFieldID) {
            this.searchFieldID = searchFieldID;
            return this;
        }

        public SearchTermBuilder entrypoint(String entrypoint) {
            this.entrypoint = entrypoint;
            return this;
        }

        public SearchTerm build() {
            return new SearchTerm(term, searchFieldID, entrypoint);
        }

        public String toString() {
            return "SearchTerm.SearchTermBuilder(term=" + this.term + ", searchFieldID=" + this.searchFieldID + ", entrypoint=" + this.entrypoint + ")";
        }
    }
}
