package sparc.team3.validator.validate;

import software.amazon.awssdk.services.ec2.model.*;
import sparc.team3.validator.util.selenium.HtmlTag;
import sparc.team3.validator.util.selenium.Login;
import sparc.team3.validator.util.selenium.SearchTerm;
import sparc.team3.validator.util.selenium.SeleniumSettings;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

public class WebAppValidate implements Callable<Boolean>{

    private final Instance instance;
    private final Logger logger;
    private final SeleniumSettings settings; 


    public WebAppValidate(Instance instance, SeleniumSettings settings){

        logger = LoggerFactory.getLogger(WebAppValidate.class);
        this.instance = instance;
        this.settings = settings; 
    
    }

    @Override
    public Boolean call() throws Exception {
      return validateWebFunctionality("");
    }

    /**
     * Validates web app functionality based on user input.
     * 
     * Need to watch out for An invalid or illegal selector was specified
     * 
     * Requires entrypoint of the main page. 
     * @param entryPoint
     * @return
     * @throws Exception
     */
    public Boolean validateWebFunctionality(String entryPoint) throws Exception {

        //Check if user provided valid settings
        if(settings == null || entryPoint.isEmpty()){

          logger.warn("Incorrect settings provided to web app validator.");
          return false; 
        }
    
        // initialize webdriver
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--headless");
        options.addArguments("--disable-dev-shm-usage");
    
        //CHRIS: UPDATE FILE PATH HERE
        ChromeDriverService service = new ChromeDriverService.Builder()
            .usingDriverExecutable(new File("/home/sparcDev/chromedriver"))
            .build();
        
        WebDriver driver = new ChromeDriver(service, options);
    
        //get main page url 
        String url = instance.publicDnsName();
        url = "http://" + url + entryPoint;
        driver.get(url);

        Boolean isValid = true;
        
        //Validate title pages specified by user. 
        if(settings.getTitles() != null && !settings.getTitles().isEmpty()){

          for(Entry<String, String> entry: settings.getTitles().entrySet()){
            
            //method does not grab any web elements. 
            if (!(checkTitle(driver, entry.getKey(), entry.getValue()))) {
              logger.warn("Web Server Validation Error: Key " + entry.getKey() 
              + " did not match expected value " + entry.getValue());
              isValid=false;
            }
          }
        }

        //Random link test, thread safe RNG.
        // if (!checkLink(driver)) {
        //   logger.warn("Validation Error: Corrupted hyperlink");
        //   isValid=false;
        // }

        //Tag test, method grabs elements.
        if(settings.getTags() != null && !settings.getTags().isEmpty()){
          
          for(HtmlTag tag: settings.getTags()){
            if(!checkTag(driver, tag)){
            logger.warn("Validation Error: Tag " + tag.toString() 
            + " does not match expected value "); 
            isValid=false;
            }
          }
        }

        //Login Test, method grabs elements.
        if(settings.getLogins() != null && !settings.getLogins().isEmpty()){

          for(Login login: settings.getLogins()){

            if (!checkLogin(driver, login)) {
              logger.warn("Validation Error: Cannot login: " + login.getUsername());
              isValid=false;
            }
          }
        }

        //Search test, method grabs elements
        if(settings.getSearchTerms() != null && !settings.getSearchTerms().isEmpty()){

          for(SearchTerm term: settings.getSearchTerms()){
            if (!checkSearch(driver, term)) {
              logger.warn("Validation Error: Cannot validate search for: " + term.getTerm());
              isValid=false;
            }
          }
        }
        
        // quit driver, close windows
        driver.quit();
        return isValid;
      }

      /**
       * Check main page title against user defined value.
       * 
       * @param expectedValue
       * @return
       * @throws InterruptedException
       */
      private Boolean checkTitle(WebDriver driver, String entryPoint, String expectedValue) throws InterruptedException {
    
        driver.navigate().to("http://" + instance.publicDnsName() + entryPoint);
        
        String title = driver.getTitle();
    
        if (!Objects.equals(expectedValue, title)) {
    
          logger.warn("Validation failed: main page title does not match expected value");
          return false;
        }
    
        logger.info("Title page validated");
        return true;
      }
    
      /**
       * Check that random link selected from main page is accessible.
       * 
       * @param driver
       * @return
       * @throws IOException
       * @throws InterruptedException
       */
      private Boolean checkLink(WebDriver driver) throws IOException, InterruptedException {
    
        List<WebElement> links = driver.findElements(By.tagName("a"));

        if(links.isEmpty()){
          return true; 
        }
        
        //generate thread safe randome number [lower, upper)
        int linkSelector = ThreadLocalRandom.current().nextInt(0, links.size());
    
        String link = links.get(linkSelector).getAttribute("href");
    
        logger.info("Checking random link: " + link);
    
        HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(link)).build();
    
        if (!(validateWithPing(httpRequest))) {
    
          return false;
        }
    
        logger.info("Random link validated: " + link);
        return true;
      }
    
      /**
       * Requires the database to be connected in order to verify the login. May need
       * to coordinate with RDS connection.
       * 
       * @param url
       * @param driver
       * @return
       * @throws InterruptedException
       */
      private Boolean checkLogin(WebDriver driver, Login login) throws InterruptedException {

        if(login.getUsername().isEmpty() || login.getUsernameFieldID().isEmpty() || 
        login.getPasswordFieldID().isEmpty() || login.getPassword().isEmpty()){
          logger.warn("Validation Error: Login field ids and username cannot be blank"); 
          return false; 
        }
        
        driver.navigate().to("http://" + instance.publicDnsName() + login.getEntrypoint());

        WebElement userInput = driver.findElement(By.id(login.getUsernameFieldID()));
        WebElement passInput = driver.findElement(By.id(login.getPasswordFieldID()));

        //should this be specified by user? (this cannot be an empty string)
        WebElement testLogin = driver.findElement(By.name("wploginattempt"));
    
        userInput.sendKeys(login.getUsername());
        passInput.sendKeys(login.getPassword());
        testLogin.click();

        logger.info("Current url" + driver.getCurrentUrl());

        // check if the main page contains the user name.
        WebElement element = driver.findElement(By.xpath("//span[contains(text(), "+login.getUsername()+")]"));

        logger.info("Current text is" + element.getText());
    
        if (element == null || !element.getText().contains(login.getUsername())) {
          return false;
        }
        
        logger.info("Validated login for" + login.getUsername());
        return true;
      }
    
      /**
       * Checks search functionality given a user defined term.
       * 
       * @return
       */
      private Boolean checkSearch(WebDriver driver, SearchTerm term) {

        if(term.getSearchFieldID().isEmpty()){
          return false;
        }
        
        String searchUrl = "http://" + instance.publicDnsName() + term.getEntrypoint();
        //make sure back main page.
        driver.navigate().to(searchUrl);
        
        //unhappy with blank field
        WebElement search = driver.findElement(By.xpath("//*[@id='"+term.getSearchFieldID()+"']"));
        search.sendKeys(term.getTerm(), Keys.RETURN);
    
        //should be on search page at this point (this can be empty)
        WebElement success = driver.findElement(By.xpath("//*[contains(text(), '" + term.getTerm() + "')]"));
    
        //check if we are on "Search Results" page or correct page. 
        if (driver.getCurrentUrl().toLowerCase().contains("search") || !success.getText().isEmpty()) {
          logger.info("Valdiated search for: " + term.getTerm()+ " at " + driver.getCurrentUrl());
          return true;
        }
    
        return false;
      }

      /**
       * Checks user-specified tag in page against expected value. 
       * 
       * @param driver
       * @param tag
       * @return
       * @throws InterruptedException
       */
      private Boolean checkTag(WebDriver driver, HtmlTag tag) throws InterruptedException{

        if(tag.getTagID().isEmpty()){
          logger.warn("Validation Error: Tag Id cannout be null or empty");
          return false;
        }
        
        String tagUrl = "http://" + instance.publicDnsName() + tag.getEntrypoint();

        driver.navigate().to(tagUrl);

        Thread.sleep(1000); 

        List<WebElement> elements = driver.findElements(By.tagName(tag.getTagID()));

        for(WebElement element: elements){
          if(element.getText().equals(tag.getValue())){
            return true;
          }
        }
        return false;
      }

      /**
       * Validate an httpRequest by ping server, 200 and 301 responses valid. 
       * @param httpRequest
       * @return
       * @throws IOException
       * @throws InterruptedException
       */
      private Boolean validateWithPing(HttpRequest httpRequest) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    
        logger.info("EC2 Instance responded with {}", httpResponse.statusCode());
    
        return httpResponse.statusCode() == 200 || httpResponse.statusCode() == 301;
    
      }
}
