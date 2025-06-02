package hellocucumber;

import io.cucumber.java.en.*;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

public class StepDefinitions {
    private WebDriver driver;
    private final int DELAY_BETWEEN_STEPS = 1000;
    // loginPage
    private final String Username_Text_Box = "/html/body/div/form/input[1]";
    private final String Password_Text_Box = "//*[@id=\"password\"]";
    private final String LoginButton_In_Login_Page = "/html/body/div/form/button";
    //navigate to participants
    private final String participants_Button= "//*[@id=\"participants-btn\"]";
    private final String Participants_Table = "table.online-participants-table tbody tr td:last-child";
    private final String QA_Course_In_My_Courses= "//div[1]/div[1]/div[1]/div[1]/a[1]/div[1]";
    // add participant
    private final String Add_Participant_Button = "//*[@id=\"add-participant-btn\"]";
    private final String New_Participant_Name_Textbox = "//*[@id=\"first-name\"]";
    private final String New_Participant_Name_Value = "Tamir";
    private final String New_Participant_Last_Name_Textbox = "//*[@id=\"last-name\"]";
    private final String New_Participant_Last_Name_Value = "Avisar";
    private final String New_Participant_Age_Textbox = "//*[@id=\"age\"]";
    private final String New_Participant_Age_Value = "22";
    private final String New_Participant_Phone_Number_Textbox = "//*[@id=\"phone\"]";
    private final String New_Participant_Phone_Number_Value = "0523850279";
    private final String New_Participant_Email_Textbox = "//*[@id=\"email\"]";
    private final String Add_Participant_Save_Button= "//*[@id=\"participant-modal\"]/div/div/button[2]";
    // remove participant
    private final String Remove_Participant_Button = "//*[@id=\"remove-participant-btn\"]";

    private final String username = "admin";
    private final String password = "pass";

    private String URL;

    public StepDefinitions() throws InterruptedException {
        String chromeDriverPath;
        try {
            // connect to the configuration file
            String configFilePath = "./config.properties";
            FileInputStream propsInput = new FileInputStream(configFilePath);
            Properties prop = new Properties();
            prop.load(propsInput);

            // use the properties of the configuration file
            chromeDriverPath = prop.getProperty("CHROME_DRIVER_PATH");
            if (chromeDriverPath.equals("\\Selenium\\chromedriver1.exe")) { // default state
                // set the chrome driver path executable
                String filePath = System.getProperty("user.dir");
                File file = new File(filePath);
                file = file.getParentFile();
                chromeDriverPath = file + chromeDriverPath; // add the local path at the start
            }
            URL = prop.getProperty("LOCAL_MOODLE_LINK");
        }
        catch (Exception e) {
            String filePath = System.getProperty("user.dir");
            File file = new File(filePath);
            file = file.getParentFile();
            chromeDriverPath = file + "\\Selenium\\chromedriver.exe";
            URL = "https://ims-project.cs.bgu.ac.il/";
        }

        //link the webdriver
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
    }

    @BeforeAll
    public static void beforeAll() throws InterruptedException {


    }

    @BeforeEach
    public void beforeEach() throws InterruptedException {

    }

    @AfterEach
    public void afterEach() throws InterruptedException {

    }

    // -----------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------
    // First scenario: Manager adds new participant
    // -----------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------

    @Given("A logged in admin user with username and password")
    public void adminLogin() throws InterruptedException {
        driver = new ChromeDriver();
        driver.get(URL);
        Thread.sleep(2500);

        WebElement UserNameButton = driver.findElement(By.xpath(Username_Text_Box));
        UserNameButton.sendKeys(username);
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement PasswordButton = driver.findElement(By.xpath(Password_Text_Box));
        PasswordButton.sendKeys(password);
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement loginButton = driver.findElement(By.xpath(LoginButton_In_Login_Page));
        loginButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);
    }


    @And("There is no participant with the email {string}")
    public void removeParticipantWithTheSameEmailIfExists(String email) throws InterruptedException {
        WebElement Participants_Button = driver.findElement(By.xpath(participants_Button));
        Participants_Button.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        List<WebElement> emailCells = driver.findElements(By.cssSelector(Participants_Table));
        int result = check_if_exists_record_with_email(emailCells, email);
        if(result != -1){
            //choose the specific row at the table
            emailCells.get(result).click();

            //click on the delete button to remove the user
            WebElement deleteButton = driver.findElement(By.xpath(Remove_Participant_Button));
            deleteButton.click();

            //accept the alert
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            alert.accept();

            //accept the second alert
            WebDriverWait wait2 = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait2.until(ExpectedConditions.alertIsPresent());
            Alert alert2 = driver.switchTo().alert();
            alert2.accept();
        }
    }

    //return the index of the row with the given email, if it doesn't exist return -1.
    private int check_if_exists_record_with_email(List<WebElement> records, String email) throws InterruptedException {
        int ret = 0;
        for (WebElement cell : records) {
            String current_email = cell.getText();
            if (current_email.equalsIgnoreCase(email)) {
                return ret;
            }
            ret++;
        }

        return -1;
    }


    @When("Add participant with the email {string}")
    public void addParticipant(String email) throws InterruptedException {
        WebElement addParticipantButton = driver.findElement(By.xpath(Add_Participant_Button));
        addParticipantButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        //add the details of the participant
        WebElement UserNameButton = driver.findElement(By.xpath(New_Participant_Name_Textbox));
        UserNameButton.sendKeys(New_Participant_Name_Value);
        WebElement LastNameButton = driver.findElement(By.xpath(New_Participant_Last_Name_Textbox));
        LastNameButton.sendKeys(New_Participant_Last_Name_Value);
        WebElement ageButton = driver.findElement(By.xpath(New_Participant_Age_Textbox));
        ageButton.sendKeys(New_Participant_Age_Value);
        WebElement phoneButton = driver.findElement(By.xpath(New_Participant_Phone_Number_Textbox));
        phoneButton.sendKeys(New_Participant_Phone_Number_Value);
        WebElement emailButton = driver.findElement(By.xpath(New_Participant_Email_Textbox));
        emailButton.sendKeys(email);
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement saveButton = driver.findElement(By.xpath(Add_Participant_Save_Button));
        saveButton.click();

        //handles the opened window
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.alertIsPresent());
        Alert alert = driver.switchTo().alert();
        alert.accept();

        Thread.sleep(DELAY_BETWEEN_STEPS);
    }

    @Then("The participant with email {string} added successfully")
    public void theParticipantAddedSuccessfully(String email) throws InterruptedException {
        List<WebElement> emailCells = driver.findElements(By.cssSelector(Participants_Table));
        assertNotEquals(check_if_exists_record_with_email(emailCells, email), -1);
        Thread.sleep(2500);
        driver.quit();
    }

    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------
    // Second scenario: Teacher reduces the maximum size of the file to submit to 1.
    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------

    @And("There is a participant with the email {string}")
    public void removeParticipantWithTheSameEmailIfDoesntExist(String email) throws InterruptedException {
        WebElement Participants_Button = driver.findElement(By.xpath(participants_Button));
        Participants_Button.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        List<WebElement> emailCells = driver.findElements(By.cssSelector(Participants_Table));
        int result = check_if_exists_record_with_email(emailCells, email);
        if(result == -1){
            WebElement addParticipantButton = driver.findElement(By.xpath(Add_Participant_Button));
            addParticipantButton.click();
            Thread.sleep(DELAY_BETWEEN_STEPS);

            //add the details of the participant
            WebElement UserNameButton = driver.findElement(By.xpath(New_Participant_Name_Textbox));
            UserNameButton.sendKeys(New_Participant_Name_Value);
            WebElement LastNameButton = driver.findElement(By.xpath(New_Participant_Last_Name_Textbox));
            LastNameButton.sendKeys(New_Participant_Last_Name_Value);
            WebElement ageButton = driver.findElement(By.xpath(New_Participant_Age_Textbox));
            ageButton.sendKeys(New_Participant_Age_Value);
            WebElement phoneButton = driver.findElement(By.xpath(New_Participant_Phone_Number_Textbox));
            phoneButton.sendKeys(New_Participant_Phone_Number_Value);
            WebElement emailButton = driver.findElement(By.xpath(New_Participant_Email_Textbox));
            emailButton.sendKeys(email);
            Thread.sleep(DELAY_BETWEEN_STEPS);

            WebElement saveButton = driver.findElement(By.xpath(Add_Participant_Save_Button));
            saveButton.click();

            //handles the opened window
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            alert.accept();
        }
    }

    @When("remove the participant with the email {string}")
    public void removeParticipant(String email) throws InterruptedException {

        List<WebElement> emailCells = driver.findElements(By.cssSelector(Participants_Table));
        int result = check_if_exists_record_with_email(emailCells, email);
        if(result != -1){
            //choose the specific row at the table
            emailCells.get(result).click();

            //click on the delete button to remove the user
            WebElement deleteButton = driver.findElement(By.xpath(Remove_Participant_Button));
            deleteButton.click();

            //accept the alert
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            alert.accept();

            //accept the second alert
            WebDriverWait wait2 = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait2.until(ExpectedConditions.alertIsPresent());
            Alert alert2 = driver.switchTo().alert();
            alert2.accept();
            Thread.sleep(DELAY_BETWEEN_STEPS);
        }
        else {
            fail("the participant with email " + email + " was not found");
        }
    }

    @Then("The participant with email {string} removed successfully")
    public void theParticipantRemovedSuccessfully(String email) throws InterruptedException {
        List<WebElement> emailCells = driver.findElements(By.cssSelector(Participants_Table));
        assertEquals(check_if_exists_record_with_email(emailCells, email), -1);
        Thread.sleep(2500);
        driver.quit();
    }

}
