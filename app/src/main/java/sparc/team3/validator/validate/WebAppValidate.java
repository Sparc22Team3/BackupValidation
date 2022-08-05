package sparc.team3.validator.validate;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Random;
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

public class WebAppValidate {

    private final Instance instance;
    private final Logger logger;


    public WebAppValidate(Instance instance){

        logger = LoggerFactory.getLogger(WebAppValidate.class);
        this.instance = instance;
    
    }

    public Boolean validateWebFunctionality(Map<String, String> functionality, String entryPoint) throws Exception {

        // Check if we can access the EC2 webserver given an entry point.
        // if (!(validateWithPing(entryPoint))) {
    
        //   logger.warn("Validation Error: Could Note Connect to Website, Status Code != 200");
    
        //   return false;
        // }
    
        // initialize webdriver
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        options.addArguments("--headless");
        options.addArguments("--disable-dev-shm-usage");
    
        // update filepath to chromedriver executable
        ChromeDriverService service = new ChromeDriverService.Builder()
            .usingDriverExecutable(new File("/home/sparcDev/chromedriver"))
            .build();
    
        WebDriver driver = new ChromeDriver(service, options);
    
        // recompute url
        String url = instance.publicDnsName();
        url = "http://" + url + entryPoint;

        driver.get(url);
    
        // Iterate through a map of values expected by the user.
        for (Entry<String, String> entry : functionality.entrySet()) {
    
          if (entry.getKey().equalsIgnoreCase("Title")) {
    
            if (!(checkTitle(driver, entry.getValue()))) {
              logger.warn("Web Server Validation Error: Main page title does not match expected value");
              return false;
            }
            ;
          }
          //links may pull from original EC2 instance, load balancers?
          else if (entry.getKey().equalsIgnoreCase("Link")) {
            if (!checkLink(driver)) {
              logger.warn("Validation Error: Corrupted HyperLink");
              return false;
            }
          }
          //need to wait or check for RDS instance. 
          //May also define a success as returning to the home page. 
          else if (entry.getKey().equalsIgnoreCase("Login")) {
    
            if (!checkLogin(url, driver)) {
              logger.warn("Validation Error: Cannot Login");
              return false;
            }
    
          }
    
          else if (entry.getKey().equalsIgnoreCase("Search")) {
            if (!checkSearch(driver, url, entry.getValue())) {
              logger.warn("Validation Error: Cannot Search");
              return false;
            }
          }
    
          else {
            logger.warn("Functionality not supported: " + entry.getKey());
          }
    
        }
    
        // quit driver, close windows
        driver.quit();
        return true;
      }

      /**
       * Check main page title against user defined value.
       * 
       * @param expectedValue
       * @return
       * @throws InterruptedException
       */
      private Boolean checkTitle(WebDriver driver, String expectedValue) throws InterruptedException {
    
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
      private Boolean checkLogin(String url, WebDriver driver) throws InterruptedException {
        
        driver.navigate().to("http://" + instance.publicDnsName() + "/wiki/index.php?title=Special:UserLogin&returnto=Main+Page");

        String username = "Sparc";
        String password = "DinnerCrime";
    
        WebElement userInput = driver.findElement(By.id("wpName1"));
        WebElement passInput = driver.findElement(By.id("wpPassword1"));
        WebElement login = driver.findElement(By.name("wploginattempt"));
    
        userInput.sendKeys(username);
        passInput.sendKeys(password);
        login.click();
        logger.info("current url 1" + driver.getCurrentUrl());
        // check if the main page contains the user name.
        WebElement element = driver.findElement(By.xpath("//span[contains(text(), '"+username+"')]"));
        //Did page change/user name should somewhere
    
        if (element == null || !element.getText().contains(username)) {
          return false;
        }
    
        return true;
      }
    
      /**
       * Checks search functionality given a user defined term.
       * 
       * @return
       */
      private Boolean checkSearch(WebDriver driver, String url, String value) {
    
        //make sure back main page.
        driver.navigate().to(url);
    
        WebElement search = driver.findElement(By.xpath("//*[@id='searchInput']"));
        search.sendKeys(value, Keys.RETURN);
    
        //should be on search page at this point
        WebElement success = driver.findElement(By.xpath("//*[contains(text(), '" + value + "')]"));
    
        //check if we are on "Search Results" page or correct page. 
        if (driver.getCurrentUrl().toLowerCase().contains("search") || !success.getText().isEmpty()) {
          logger.info("Valdiated search for: " + value + " at " + driver.getCurrentUrl());
          return true;
        }
    
        return false;
      }

      public Boolean validateWithPing(HttpRequest httpRequest) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    
        logger.info("EC2 Instance responded with {}", httpResponse.statusCode());
    
        return httpResponse.statusCode() == 200 || httpResponse.statusCode() == 301;
    
      }
}
