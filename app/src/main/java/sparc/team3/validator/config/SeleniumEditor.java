package sparc.team3.validator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import sparc.team3.validator.util.CLI;
import sparc.team3.validator.config.seleniumsettings.HtmlTag;
import sparc.team3.validator.config.seleniumsettings.Login;
import sparc.team3.validator.config.seleniumsettings.SearchTerm;
import sparc.team3.validator.config.seleniumsettings.SeleniumSettings;

import java.io.IOException;
import java.util.*;

/**
 * Class to provide a builder and editor for Selenium Settings
 */
public class SeleniumEditor extends Selenium{
    Map<String, String> titleMap;
    Set<HtmlTag> htmlTagSet;
    Set<SearchTerm> searchTermSet;
    Set<Login> loginSet;

    final String titleColor = CLI.ANSI_CYAN;
    final String htmlTagColor = CLI.ANSI_PURPLE;
    final String loginColor = CLI.ANSI_GREEN;
    final String searchTermColor = CLI.ANSI_BLUE;

    public SeleniumEditor(CLI cli, String seleniumFileLocation) {
        super(cli, seleniumFileLocation);

        titleMap = new TreeMap<>();
        htmlTagSet = new TreeSet<>();
        searchTermSet = new TreeSet<>();
        loginSet = new TreeSet<>();
    }

    /**
     * Run the Builder to build new Selenium Settings file
     * @throws IOException if there is an IO error
     */
    public void runBuilder() throws IOException {
        boolean cont;

        cont = cli.promptYesOrNoColor("\nWould you like to add a page title to check?", titleColor);
        while(cont){
            buildTitleEntry();
            cont = cli.promptYesOrNoColor("Would you like to add another page title?", CLI.ANSI_YELLOW);
        }

        cont = cli.promptYesOrNoColor("\nWould you like to add an HTML tag to check?", htmlTagColor);
        while(cont){
            htmlTagSet.add(buildHtmlTag());
            cont = cli.promptYesOrNoColor("Would you like to add another HTML tag?", CLI.ANSI_YELLOW);
        }
        cont = cli.promptYesOrNoColor("\nWould you like to add a search term to check?", searchTermColor);
        while(cont){
            searchTermSet.add(buildSearchTerm());
            cont = cli.promptYesOrNoColor("Would you like to add another search term?", CLI.ANSI_YELLOW);
        }
        cont = cli.promptYesOrNoColor("\nWould you like to add a login to check?", loginColor);
        while(cont){
            loginSet.add(buildLogin());
            cont = cli.promptYesOrNoColor("Would you like to add another login?", CLI.ANSI_YELLOW);
        }

        printSets();
        if(cli.promptYesOrNo("\nWould you like to edit the settings?")) {
            editor();
            return;
        }
        saveSettings();
    }

    /**
     * Editor to edit existing Selenium Settings File
     * @throws IOException if there is an IO error
     */
    private void editor() throws IOException {

        boolean cont = cli.promptYesOrNoColor("\nWould you like to edit any of the titles?", titleColor);

        while(cont){
            printTitles(false);
            buildTitleEntry();
            cont = cli.promptYesOrNoColor("Continue editing titles?", CLI.ANSI_YELLOW);
        }

        cont = cli.promptYesOrNoColor("\nWould you like to remove any of the titles?", titleColor);

        while(cont){
            printTitles(false);
            titleMap.remove(cli.prompt("Entrypoint of title to remove?"));
            cont = cli.promptYesOrNoColor("Continue removing titles?", CLI.ANSI_YELLOW);
        }

        if(cli.promptYesOrNoColor("\nWould you like to edit or remove any of the HTML tags?", htmlTagColor)) {
            while(true){
                printHtmlTags(true);
                int selection = cli.promptNumber("Please select the number of the tag to edit or remove (0 to go back):", 0, htmlTagSet.size());
                if(selection == 0)
                    break;
                // Compensate for 0 being back
                selection--;

                ArrayList<HtmlTag> htmlTags = new ArrayList<>(htmlTagSet);
                HtmlTag tag = htmlTags.get(selection);
                htmlTagSet.remove(tag);

                if(cli.promptYesOrNoColor("Remove the html tag? (Yes to remove, No to edit)", CLI.ANSI_YELLOW))
                    continue;

                tag = buildHtmlTag(true, tag);
                htmlTagSet.add(tag);
            }
        }

        if(cli.promptYesOrNoColor("\nWould you like to edit or remove any of the logins?", loginColor)) {
            while(true){
                printLogins(true);
                int selection = cli.promptNumber("Please select the number of the login to edit or remove (0 to go back):", 0, htmlTagSet.size());
                if(selection == 0)
                    break;

                // Compensate for 0 being back
                selection--;
                ArrayList<Login> logins = new ArrayList<>(loginSet);
                Login login = logins.get(selection);
                loginSet.remove(login);

                if(cli.promptYesOrNoColor("Remove the login? (Yes to remove, No to edit)", CLI.ANSI_YELLOW))
                    continue;
                login = buildLogin(true, login);
                loginSet.add(login);
            }
        }

        if(cli.promptYesOrNoColor("\nWould you like to edit or remove any of the search terms?", searchTermColor)) {
            while(true){
                printSearchTerms(true);
                int selection = cli.promptNumber("Please select the number of the search term to edit or remove (0 to go back):", 0, htmlTagSet.size());
                if(selection == 0)
                    break;

                // Compensate for 0 being back
                selection--;
                ArrayList<SearchTerm> searchTerms = new ArrayList<>(searchTermSet);
                SearchTerm searchTerm = searchTerms.get(selection);
                searchTermSet.remove(searchTerm);

                if(cli.promptYesOrNoColor("Remove the search term? (Yes to remove, No to edit)", CLI.ANSI_YELLOW))
                    continue;
                searchTerm = buildSearchTerm(true, searchTerm);
                searchTermSet.add(searchTerm);
            }
        }

        saveSettings();
    }

    /**
     * Run the editor
     * @throws IOException
     */
    public void runEditor() throws IOException{
        SeleniumLoader loader = new SeleniumLoader(cli, seleniumFile.toString());
        SeleniumSettings seleniumSettings = loader.loadSettings();
        titleMap = seleniumSettings.getTitles();
        htmlTagSet = seleniumSettings.getTags();
        loginSet = seleniumSettings.getLogins();
        searchTermSet = seleniumSettings.getSearchTerms();
        runBuilder();
    }

    /**
     * Save the Selenium Settings file
     * @throws IOException
     */
    void saveSettings() throws IOException {

        SeleniumSettings seleniumSettings = new SeleniumSettings(titleMap, htmlTagSet, loginSet, searchTermSet);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(seleniumFile.toFile(), seleniumSettings);
        cli.out("Settings saved!", CLI.ANSI_GREEN_BACKGROUND + CLI.ANSI_BLACK);
    }

    /**
     * Prompt when a value is required
     * @param format String of format
     * @param args Objects to add to format string
     * @return String of user entry
     * @throws IOException if there is an IO error
     */
    String promptRequireValue(String format, Object... args) throws IOException {
        String response;
        while((response = cli.prompt(format, args)).isEmpty()){
            cli.outColor("Cannot be empty.\n", CLI.ANSI_RED);
        }

        return response;
    }

    /**
     * Prompt when there is a current value
     * @param format String of format
     * @param current String of current value of setting
     * @param args Objects to add to format string
     * @return String of user entry
     * @throws IOException  if there is an IO error
     */
    String promptCurrentValue(String format, String current, Object... args) throws IOException {
        String response;
        Object[] newArgs = Arrays.copyOf(args,args.length + 1);
        newArgs[newArgs.length - 1] = current;
        response = cli.prompt(format, newArgs);

        return response.isEmpty() ? current : response;
    }

    /**
     * Build a title for Selenium to check
     * @throws IOException if there is an IO error
     */
    void buildTitleEntry() throws IOException {
        String title;
        String entrypoint;
        cli.outColor("If an entrypoint has already been added, the new title will overwrite the old title.\n", CLI.ANSI_YELLOW);


        entrypoint = promptRequireValue("Entrypoint (path after domain) of page:");
        title = promptRequireValue("Title of page:");

        String oldTitle = titleMap.put(entrypoint, title);

        if(oldTitle != null)
            cli.out("Old value: %s\t\tNew Value:%s\n", oldTitle, title);
    }

    /**
     * Build an HTML Tag for Selenium to check
     * @param edit boolean for if this is called from editor or builder
     * @param current String of current value, if it has one
     * @return HtmlTag for Selnium
     * @throws IOException if there is an IO error
     */
    HtmlTag buildHtmlTag(boolean edit, HtmlTag current) throws IOException {
        String tagFormat = edit ? "CSS Selector to identify and find HTML tag [%s]:" : "CSS Selector to identify and find HTML tag:";
        String valueFormat = edit ? "Expected text content inside HTML tag [%s]:" : "Expected text content inside HTML tag :";
        String entrypointFormat = edit ? "Entrypoint (path after domain) [%s]:" : "Entrypoint (path after domain):";
        String tag;
        String value;
        String entrypoint;

        if(edit && current != null) {
            tag = promptCurrentValue(tagFormat, current.getCssSelector());
            value = promptCurrentValue(valueFormat, current.getValue());
            entrypoint = promptCurrentValue(entrypointFormat, current.getEntrypoint());
        } else {
            tag = promptRequireValue(tagFormat);
            value = promptRequireValue(valueFormat);
            entrypoint = promptRequireValue(entrypointFormat);
        }

        return new HtmlTag(tag, value, entrypoint);
    }

    /**
     * Build a new Html Tag
     * @return HtmlTag for Selenium
     * @throws IOException if there is an IO error
     */
    HtmlTag buildHtmlTag() throws IOException {
        return buildHtmlTag(false, null);
    }

    /**
     * Build a Login for Selenium to check
     * @param edit boolean for if this is called from editor or builder
     * @param current String value if it has one
     * @return Login for Selenium
     * @throws IOException if there is an IO error
     */
    Login buildLogin(boolean edit, Login current) throws IOException {
        String usernameFormat = edit ? "Username [%s]:" : "Username:";
        String passwordFormat = edit ? "Password [%s]:" : "Password:";
        String usernameFieldIDFormat = edit ? "CSS Selector to identify and find Username text field [%s]:" : "CSS Selector to identify and find Username text field:";
        String passwordFieldIDFormat = edit ? "CSS Selector to identify and find Password text field  [%s]:" : "CSS Selector to identify and find Password text field:";
        String entrypointFormat = edit ? "Entrypoint (path after domain) [%s]:" : "Entrypoint (path after domain):";
        String username;
        String password;
        String usernameFieldID;
        String passwordFieldID;
        String entrypoint;

        if(edit && current != null) {
            username = promptCurrentValue(usernameFormat, current.getUsername());
            password = promptCurrentValue(passwordFormat, current.getPassword());
            usernameFieldID = promptCurrentValue(usernameFieldIDFormat, current.getUsernameCssSelector());
            passwordFieldID = promptCurrentValue(passwordFieldIDFormat, current.getPasswordCssSelector());
            entrypoint = promptCurrentValue(entrypointFormat, current.getEntrypoint());
        } else {
            username = promptRequireValue(usernameFormat);
            password = promptRequireValue(passwordFormat);
            usernameFieldID = promptRequireValue(usernameFieldIDFormat);
            passwordFieldID = promptRequireValue(passwordFieldIDFormat);
            entrypoint = promptRequireValue(entrypointFormat);
        }

        return new Login(username, password, usernameFieldID, passwordFieldID, entrypoint);
    }

    /**
     * Build a new Login
     * @return Login for Selenium
     * @throws IOException if there is an IO error
     */
    Login buildLogin() throws IOException{
        return buildLogin(false, null);
    }

    /**
     * Build a SearchTerm for Selenium to check
     * @param edit boolean if this is called from the editor or builder
     * @param current String of current value if it has one
     * @return SearchTerm for Selenium
     * @throws IOException if there is an IO error
     */
    SearchTerm buildSearchTerm(boolean edit, SearchTerm current) throws IOException {
        String termFormat = edit ? "Search Term [%s]:" : "Search Term:";
        String searchFieldIDFormat = edit ? "CSS Selector to identify and find Search text field [%s]:" : "CSS Selector to identify and find Search text field:";
        String entrypointFormat = edit ? "Entrypoint (path after domain) [%s]:" : "Entrypoint (path after domain):";
        String term;
        String searchFieldId;
        String entrypoint;

        if(edit && current != null) {
            term = promptCurrentValue(termFormat, current.getTerm());
            searchFieldId = promptCurrentValue(searchFieldIDFormat, current.getSearchCssSelector());
            entrypoint = promptCurrentValue(entrypointFormat, current.getEntrypoint());

        } else {
            term = promptRequireValue(termFormat);
            searchFieldId = promptRequireValue(searchFieldIDFormat);
            entrypoint = promptRequireValue(entrypointFormat);
        }
        return new SearchTerm(term, searchFieldId, entrypoint);
    }

    /**
     * Build a new SearchTerm for Selenium to check
     * @return SearchTerm for Selenium
     * @throws IOException if there is an IO error
     */
    SearchTerm buildSearchTerm() throws IOException {
        return buildSearchTerm(false, null);
    }

    /**
     * Prints the sets of titles, HtmlTags, Logins, and SearchTerms
     */
    void printSets(){
        cli.outColor("\n############### Current Selenium Settings ###############\n", CLI.ANSI_YELLOW);
        printTitles(false);
        printHtmlTags(false);
        printLogins(false);
        printSearchTerms(false);
        cli.outColor("#########################################################\n\n", CLI.ANSI_YELLOW);
    }

    /**
     * Prints out the Map of Titles
     * @param showCount boolean whether include number of title before each entry
     */
    void printTitles(boolean showCount){
        int i = 1;
        cli.outColor("Titles\n", titleColor);
        for(Map.Entry<String, String> titleEntry: titleMap.entrySet()){
            cli.out("\tEntrypoint: '%s'\n" +
                    "\tTitle: '%s'\n\n", titleEntry.getKey(), titleEntry.getValue());
            i++;
        }
    }

    /**
     * Print the set of HtmlTags
     * @param showCount boolean whether to include number of HtmlTag before each entry
     */
    void printHtmlTags(boolean showCount){
        int i = 1;
        cli.outColor("HTML Tags:\n", htmlTagColor);
        for(HtmlTag tag: htmlTagSet){
            String num = showCount ? i + ": " : "";
            cli.out("\t" + num + "HTML Tag\n" +
                    "\t\tHTML Tag's CSS Selector: '%s'\n" +
                    "\t\tExpected Text Content: '%s'\n" +
                    "\t\tEntrypoint: '%s'\n", tag.getCssSelector(), tag.getValue(), tag.getEntrypoint());
            i++;
        }
    }

    /**
     * Print the set of Logins
     * @param showCount boolean whether to include number of Login before each entry
     */
    void printLogins(boolean showCount){
        int i = 1;
        cli.outColor("Logins:\n", loginColor);
        for(Login login: loginSet){
            String num = showCount ? i + ": " : "";
            cli.out("\t" + num + "Login\n" +
                    "\t\tUsername: '%s'\n" +
                    "\t\tPassword: '%s'\n" +
                    "\t\tUsername Text Field's CSS Selector: '%s'\n" +
                    "\t\tPassword Text Field's CSS Selector: '%s'\n" +
                    "\t\tEntrypoint: '%s'\n",
                    login.getUsername(), login.getPassword(), login.getUsernameCssSelector(),
                    login.getPasswordCssSelector(), login.getEntrypoint());
            i++;
        }
    }

    /**
     * Print the set of SearchTerms
     * @param showCount boolean whether to include number of Login before each entry
     */
    void printSearchTerms(boolean showCount){
        int i = 1;
        cli.outColor("Search Terms\n", searchTermColor);
        for(SearchTerm term: searchTermSet){
            String num = showCount ? i + ": " : "";
            cli.out("\t" + num + "Search Term:\n" +
                    "\t\tTerm: '%s'\n" +
                    "\t\tSearch Text Field's CSS Selector: '%s'\n" +
                    "\t\tEntrypoint: '%s'\n", term.getTerm(), term.getSearchCssSelector(), term.getEntrypoint());
            i++;
        }
    }
}
