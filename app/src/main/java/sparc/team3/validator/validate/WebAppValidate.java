package sparc.team3.validator.validate;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ec2.model.Instance;
import sparc.team3.validator.config.seleniumsettings.HtmlTag;
import sparc.team3.validator.config.seleniumsettings.Login;
import sparc.team3.validator.config.seleniumsettings.SearchTerm;
import sparc.team3.validator.config.seleniumsettings.SeleniumSettings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

public class WebAppValidate implements Callable<Boolean> {

    private final Instance instance;
    private final Logger logger;
    private final SeleniumSettings settings;


    public WebAppValidate(Instance instance, SeleniumSettings settings) {

        logger = LoggerFactory.getLogger(WebAppValidate.class);
        this.instance = instance;
        this.settings = settings;

    }

    /**
     * Entry point for Web App Validation tests
     * @return Boolean if all tests passed
     */
    @Override
    public Boolean call() {
        return validateWebFunctionality();
    }

    /**
     * Validates web app functionality based on user input.
     * <p>
     * Need to watch out for An invalid or illegal selector was specified
     * <p>
     * Requires entrypoint of the main page.
     *
     * @return Boolean if all tests pass
     */
    public Boolean validateWebFunctionality() {

        //Check if user provided valid settings
        if (settings == null) {

            logger.warn("Incorrect settings provided to web app validator.");
            return false;
        }

        // initialize webdriver
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--headless");
        options.addArguments("--disable-dev-shm-usage");

        //CHRIS: UPDATE FILE PATH HERE
        WebDriverManager.chromedriver().setup();
        ChromeDriverService service = new ChromeDriverService.Builder()
                //.usingDriverExecutable(new File("/home/sparcDev/chromedriver"))
                .build();

        WebDriver driver = new ChromeDriver(service, options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        //get main page url 
        String url = instance.publicDnsName();
        url = "http://" + url;
        driver.get(url);

        boolean titlesPassed = true;
        boolean tagsPassed = true;
        boolean loginPassed = true;
        boolean searchPassed = true;

        //Validate title pages specified by user. 
        if (settings.getTitles() != null && !settings.getTitles().isEmpty()) {

            for (Entry<String, String> entry : settings.getTitles().entrySet()) {
                //method does not grab any web elements.
                if (!(checkTitle(driver, entry.getKey(), entry.getValue()))) {
                    logger.warn("Web App Validation Error: {} did not match expected title {}", entry.getKey(), entry.getValue());
                    titlesPassed = false;
                }
            }
            if(titlesPassed)
                logger.info("Validated specified page titles");
        }

        //Random link test, thread safe RNG.
        // if (!checkLink(driver)) {
        //   logger.warn("Validation Error: Corrupted hyperlink");
        //   isValid=false;
        // }

        //Tag test, method grabs elements.
        if (settings.getTags() != null && !settings.getTags().isEmpty()) {

            for (HtmlTag tag : settings.getTags()) {
                if (!checkTag(driver, tag)) {
                    logger.warn("Web App Validation Error: Tag selected by {} does not match expected value {} on {}", tag.getCssSelector(), tag.getValue(), tag.getEntrypoint());
                    tagsPassed = false;
                }
            }
            if(tagsPassed)
                logger.info("Validated specified HTML tags.");
        }

        //Login Test, method grabs elements.
        if (settings.getLogins() != null && !settings.getLogins().isEmpty()) {

            for (Login login : settings.getLogins()) {

                if (!checkLogin(driver, login)) {
                    logger.warn("Web App Validation Error: Cannot login {} on {} with username field selected by {}", login.getUsername(), login.getEntrypoint(), login.getUsernameCssSelector());
                    loginPassed = false;
                }
            }
            if(loginPassed)
                logger.info("Validated specified logins.");
        }

        //Search test, method grabs elements
        if (settings.getSearchTerms() != null && !settings.getSearchTerms().isEmpty()) {

            for (SearchTerm term : settings.getSearchTerms()) {
                if (!checkSearch(driver, term)) {
                    logger.warn("Web App Validation Error: Cannot validate search for: " + term.getTerm());
                    searchPassed = false;
                }
            }
            if(searchPassed)
                logger.info("Validated specified search terms.");
        }

        // quit driver, close windows
        driver.quit();
        return titlesPassed && tagsPassed && loginPassed && searchPassed;
    }

    /**
     * Check main page title against user defined value.
     *
     * @param driver WebDriver to use
     * @param entryPoint String of entrypoint to use to build url of page to check title
     * @param expectedValue String of expected title
     * @return
     */
    private Boolean checkTitle(WebDriver driver, String entryPoint, String expectedValue) {
        entryPoint = checkEntrypoint(entryPoint);
        driver.navigate().to("http://" + instance.publicDnsName() + entryPoint);

        String title = driver.getTitle();

        if (!Objects.equals(expectedValue, title)) {
            return false;
        }
        return true;
    }

    /**
     * Checks tag with user-specified css selector in page against expected value.
     *
     * @param driver
     * @param tag HtmlTag to test
     * @return Boolean if test passed
     */
    private Boolean checkTag(WebDriver driver, HtmlTag tag) {

        if (tag.getCssSelector().isEmpty()) {
            logger.warn("Validation Error: CSS Selector cannot be null or empty");
            return false;
        }

        String entrypoint = checkEntrypoint(tag.getEntrypoint());

        String tagUrl = "http://" + instance.publicDnsName() + entrypoint;

        driver.navigate().to(tagUrl);

        WebElement element = driver.findElement(By.cssSelector(tag.getCssSelector()));

        return element.getAttribute("textContent").equals(tag.getValue());
    }

    /**
     * Requires the database to be connected in order to verify the login. May need
     * to coordinate with RDS connection.
     *
     * @param driver WebDriver for Selenium to use
     * @param login Login to test
     * @return Boolean if test passed
     */
    private Boolean checkLogin(WebDriver driver, Login login){

        if (login.getUsername().isEmpty() || login.getUsernameCssSelector().isEmpty() ||
                login.getPasswordCssSelector().isEmpty() || login.getPassword().isEmpty()) {
            logger.warn("Validation Error: Login field ids and username cannot be blank");
            return false;
        }

        String entrypoint = checkEntrypoint(login.getEntrypoint());

        driver.navigate().to("http://" + instance.publicDnsName() + entrypoint);

        WebElement userInput = driver.findElement(By.cssSelector(login.getUsernameCssSelector()));
        WebElement passInput = driver.findElement(By.cssSelector(login.getPasswordCssSelector()));

        //should this be specified by user? (this cannot be an empty string)

        userInput.sendKeys(login.getUsername());
        passInput.sendKeys(login.getPassword());
        passInput.submit();

        // check if the main page contains the username.
        WebElement element = driver.findElement(By.xpath("//*[contains(text(), " + login.getUsername() + ")]"));

        if (element == null || !element.getAttribute("textContent").contains(login.getUsername())) {
            return false;
        }

        return true;
    }

    /**
     * Checks search functionality given a user defined term.
     * @param driver WebDriver to use
     * @param term SearchTerm to test
     * @return Boolean if test passed
     */
    private Boolean checkSearch(WebDriver driver, SearchTerm term) {

        if (term.getSearchCssSelector().isEmpty()) {
            return false;
        }

        String entrypoint = checkEntrypoint(term.getEntrypoint());

        String searchUrl = "http://" + instance.publicDnsName() + entrypoint;
        //make sure back main page.
        driver.navigate().to(searchUrl);

        //unhappy with blank field
        WebElement search = driver.findElement(By.cssSelector(term.getSearchCssSelector()));
        search.sendKeys(term.getTerm(), Keys.RETURN);

        //should be on search page at this point (this can be empty)
        WebElement success = driver.findElement(By.xpath("//*[contains(text(), '" + term.getTerm() + "')]"));

        //check if we are on "Search Results" page or correct page. 
        if (driver.getCurrentUrl().toLowerCase().contains("search") || !success.getAttribute("textContent").isEmpty()) {
            return true;
        }

        return false;
    }

    /**
     * Ensures entrypoint has '/' at the start of the string to ensure a proper url can be formed
     * @param entrypoint String of entrypoint to check
     * @return String of entrypoint with guaranteed '/' as first character.
     */
    private String checkEntrypoint(String entrypoint) {
        if (!entrypoint.startsWith("/")) {
            return "/" + entrypoint;
        }

        return entrypoint;
    }

    /**
     * Check that random link selected from main page is accessible.
     *
     * @param driver WebDriver to use
     * @return Boolean if test passed
     * @throws IOException if there is an IO error
     * @throws InterruptedException if the thread is interrupted
     */
    private Boolean checkLink(WebDriver driver) throws IOException, InterruptedException {

        List<WebElement> links = driver.findElements(By.tagName("a"));

        if (links.isEmpty()) {
            return true;
        }

        //generate thread safe random number [lower, upper)
        int linkSelector = ThreadLocalRandom.current().nextInt(0, links.size());

        String link = links.get(linkSelector).getAttribute("href");

        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(link)).build();

        if (!(validateWithPing(httpRequest))) {
            return false;
        }

        return true;
    }

    /**
     * Validate an httpRequest by ping server, 200 and 301 responses valid.
     *
     * @param httpRequest HttpRequest to check for valid status
     * @return Boolean if test passed
     * @throws IOException if there is an IO error
     * @throws InterruptedException if the thread is interrupted
     */
    private Boolean validateWithPing(HttpRequest httpRequest) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        logger.info("EC2 Instance responded with {}", httpResponse.statusCode());

        return httpResponse.statusCode() == 200 || httpResponse.statusCode() == 301;

    }
}
