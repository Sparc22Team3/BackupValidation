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
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;

public class EC2ValidateInstance {
  private final Instance instance;
  private final Ec2Client ec2Client;
  private WebDriver driver;
  private final Logger logger;

  public EC2ValidateInstance(Ec2Client ec2Client, Instance instance) {
    logger = LoggerFactory.getLogger(EC2ValidateInstance.class);
    this.ec2Client = ec2Client;
    this.instance = instance;
  }

  /**
   * Ping url and check status code.
   * 
   * @return boolean whether the instance is pingable
   * @throws Exception when there is a problem pinging the instance
   */
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

    driver = new ChromeDriver(service, options);

    // recompute url
    String url = instance.publicDnsName();
    url = "http://" + url + entryPoint;

    // Iterate through a map of values expected by the user.
    for (Entry<String, String> entry : functionality.entrySet()) {

      if (entry.getKey().equalsIgnoreCase("Title")) {

        if (!(checkTitle(url, entry.getValue()))) {
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
   * YOU HAVE THE PROD AVAILABLE
   * 
   * Type (mediaWiki, title: value, map tags by id/expected values, login username/pw (input boxes/id button), 
   * search terms:search id, entrypoint for every test)
   * 
   * Will get values from config - web app validator class
   * Method call, logic to decide which tests to run. Thread exector.
   * Check for image/images. (id/make sure image loaded - event triggered if doesn't load)
   * 
   * S3 url (base of url is correct s3 url) -> pulling from correct bucket/object
   * 
   * Restore/Same settings (original bucket was public its public?)
   * @param url
   * @param expectedValue
   * @return
   * @throws InterruptedException
   */
  private Boolean checkTitle(String url, String expectedValue) throws InterruptedException {

    // blocking call
    driver.get(url);

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

    Random rand = new Random();

    //rand exclusive
    int linkSelector = rand.nextInt(links.size());

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
   * //Have instance be production/check validation script if works. 
   * 
   * 
   * @param url
   * @param driver
   * @return
   * @throws InterruptedException
   */
  private Boolean checkLogin(String url, WebDriver driver) throws InterruptedException {

    // NEED SOME CODE TO WAIT FOR RDS connection.

    // navigate to the login page
    // entry point should be in config.
    driver.navigate()
        .to("http://" + instance.publicDnsName() + "/wiki/index.php?title=Special:UserLogin&returnto=Main+Page");
    // These params should be in json config
    String username = "Sparc";
    String password = "DinnerCrime";

    // "Should these be in json config?" Media wiki will have same id/not all webpages
    // Get us into relm of plan/generic/anything that can be generic
    // Title always will be title tag from the head. 
    //Make abstract class that has stuff that can be completely generic
    //Head section Title/
    //Provide map of id elements and expected value of main page.
    //Abstract validatior/media wiki validator class
    //Pass method as a peremator (class from strings/use media wiki end)
    //Check login.
    WebElement userInput = driver.findElement(By.id("wpName1"));
    WebElement passInput = driver.findElement(By.id("wpPassword1"));
    WebElement login = driver.findElement(By.name("wploginattempt"));

    userInput.sendKeys(username);
    passInput.sendKeys(password);
    login.click();

    // check if the main page contains the user name.
    WebElement element = driver.findElement(By.xpath("//*[contains(text(), '" + username + "')]"));

    //Did page change/user name should somewhere
    logger.info(element.getText());

    // Will fail without connection to database.
    if (element == null || !element.getText().contains(username)) {
      return false;
    }

    return true;
  }

  /**
   * Checks search functionality given a user defined term.
   * 
   * S
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

  public Boolean validateWithPing(String entryPoint) throws IOException, InterruptedException {
    String url = instance.publicDnsName();

    try {
      waitForEC2Checks();
    } catch (InterruptedException | TimeoutException e) {
      logger.error("Waiting for the EC2 instance to be ready has timed out or otherwise been interrupted", e);
      return false;
    }

    url = "http://" + url + entryPoint;
    HttpClient httpClient = HttpClient.newHttpClient();
    HttpRequest httpRequest = HttpRequest.newBuilder().uri(URI.create(url)).build();
    HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

    logger.info("EC2 Instance responded with {}", httpResponse.statusCode());

    return httpResponse.statusCode() == 200;

  }

  public Boolean validateWithPing(HttpRequest httpRequest) throws IOException, InterruptedException {

    HttpClient httpClient = HttpClient.newHttpClient();
    HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

    logger.info("EC2 Instance responded with {}", httpResponse.statusCode());

    return httpResponse.statusCode() == 200;

  }

  /**
   * Busy wait for EC2 to complete setup.
   * 
   * @throws Exception when the instance times out.
   */
  private void waitForEC2Checks() throws InterruptedException, TimeoutException {

    // Wait for ec2 instance to complete setup
    // @TODO should we change to waiter?
    int attempts = 0;
    while (attempts < 11) {

      try {
        DescribeInstanceStatusRequest statusReq = DescribeInstanceStatusRequest
            .builder().instanceIds(instance.instanceId()).build();

        DescribeInstanceStatusResponse statusRes = ec2Client.describeInstanceStatus(statusReq);

        String running = statusRes.instanceStatuses().get(0).instanceState().name().toString();
        String sysPass = statusRes.instanceStatuses().get(0).systemStatus().status().toString();
        String reachPass = statusRes.instanceStatuses().get(0).instanceStatus().status().toString();

        logger.info("EC2 instance {}:\t\tRunning:{}\t\tSys Pass:{}\t\tReach Pass:{}", instance.instanceId(), running,
            sysPass, reachPass);

        if ((running.equals("running")) && (sysPass.equals("passed") || sysPass.equals("ok"))
            && (reachPass.equals("passed") || reachPass.equals("ok"))) {
          break;
        }

      } catch (AwsServiceException e) {
        logger.error("Problem getting status of EC2 instance {}", instance.instanceId(), e);
        return;
      }

      Thread.sleep(60000);
      attempts++;
    }

    if (attempts >= 11) {
      throw new TimeoutException("EC2 Instance Timeout");
    }
  }

  /**
   * Terminate ec2Instance attached to client.
   */
  public void terminateEC2Instance() {

    System.out.println("Terminating Instance");

    TerminateInstancesRequest terminateRequest = TerminateInstancesRequest.builder().instanceIds(instance.instanceId())
        .build();
    ec2Client.terminateInstances(terminateRequest);

  }
}
