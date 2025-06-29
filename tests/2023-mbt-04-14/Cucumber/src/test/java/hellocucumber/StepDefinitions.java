package hellocucumber;

import io.cucumber.java.en.*;
import io.cucumber.java.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
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
    //navigate to operators
    private final String Remove_Operator_Button = "//*[@id=\"remove-operator-btn\"]";
    private final String operators_Button = "//*[@id=\"operators-btn\"]";
    //add operator
    private final String add_Operator = "//*[@id=\"add-operator-btn\"]";
    private final String add_Operator_Username_Textbox = "//*[@id=\"username\"]";
    private final String add_Operator_Password_Textbox = "//*[@id=\"password\"]";
    private final String add_Operator_Save_Button = "//*[@id=\"save-operator-btn\"]";


    private final String username = "admin";
    private final String password = "pass";
    private final String main_Menu_URL = "https://ims-project.cs.bgu.ac.il/main_menu";
    private final String disconnect_Button = "//*[@id=\"disconnect-btn\"]";


    private final String lobbies_Button = "/html/body/div/button[1]";
    private final String create_Lobby_Button = "//*[@id=\"create-lobby-btn\"]";
    private final String lobby_Participants_Table = "//*[@id='create-lobby-modal']/div/form/div/div[1]/table/tbody/tr";
    private final String save_Lobby_Button = "//*[@id=\"create-lobby-modal\"]/div/form/div/div[2]/button[2]";
    private final String remove_Lobby_Buton = "//*[@id=\"delete-lobby-btn\"]";

    private final String session_Data_Button = "/html/body/div/button[4]";

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
    // First scenario: Admin adds new participant.
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
    // Second scenario: Admin remove participant.
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


    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------
    // 3rd scenario: Admin adds new operator.
    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------

    @And("There is no operator with the username {string}")
    public void thereIsNoOperatorWithTheUsername(String username) throws InterruptedException {
        WebElement Operators_Button = driver.findElement(By.xpath(operators_Button));
        Operators_Button.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);
        List<WebElement> adminCells = driver.findElements(By.cssSelector(Participants_Table));
        int result = check_if_exists_record_with_email(adminCells, username);
        if(result != -1){
            adminCells.get(result).click();

            //click on the delete button to remove the user
            WebElement deleteButton = driver.findElement(By.xpath(Remove_Operator_Button));
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

    @When("Add operator with the username {string} and password {string}")
    public void addOperator(String username, String password) throws InterruptedException {
        WebElement addOperatorButton = driver.findElement(By.xpath(add_Operator));
        addOperatorButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        //add the details of the operator
        WebElement UserNameButton = driver.findElement(By.xpath(add_Operator_Username_Textbox));
        UserNameButton.sendKeys(username);
        WebElement PasswordButton = driver.findElement(By.xpath(add_Operator_Password_Textbox));
        PasswordButton.sendKeys(password);
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement saveButton = driver.findElement(By.xpath(add_Operator_Save_Button));
        saveButton.click();

        //handles the opened window
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.alertIsPresent());
        Alert alert = driver.switchTo().alert();
        alert.accept();

        Thread.sleep(DELAY_BETWEEN_STEPS);
    }

    @Then("The operator with username {string} added successfully")
    public void theOperatorAddedSuccessfully(String username) throws InterruptedException {
        List<WebElement> operatorsCells = driver.findElements(By.cssSelector(Participants_Table));
        assertNotEquals(check_if_exists_record_with_email(operatorsCells, username), -1);
        Thread.sleep(2500);
    }

    @And("The operator with username {string} and password {string} can log in")
    public void operatorCanLogin(String username, String password) throws InterruptedException {
        driver.get(main_Menu_URL);
        try {
            WebElement disconnectButton = driver.findElement(By.xpath(disconnect_Button));
            disconnectButton.click();
        }
        catch (Exception ignored) {

        }
        WebElement UserNameButton = driver.findElement(By.xpath(Username_Text_Box));
        UserNameButton.sendKeys(username);
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement PasswordButton = driver.findElement(By.xpath(Password_Text_Box));
        PasswordButton.sendKeys(password);
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement loginButton = driver.findElement(By.xpath(LoginButton_In_Login_Page));
        loginButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        try {
            WebElement disconnectButton = driver.findElement(By.xpath(disconnect_Button));
            disconnectButton.click();
            assertTrue(true);
        }
        catch (Exception ignored) {
            fail();
        }
        driver.quit();
    }

    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------
    // 4th scenario: Admin removes operator.
    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------

    @And("There is an operator with the username {string} and password {string}")
    public void thereIsAnOperatorWithTheUsername(String username, String password) throws InterruptedException {
        WebElement Operators_Button = driver.findElement(By.xpath(operators_Button));
        Operators_Button.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        List<WebElement> adminCells = driver.findElements(By.cssSelector(Participants_Table));
        int result = check_if_exists_record_with_email(adminCells, username);
        if(result == -1){
            WebElement addOperatorButton = driver.findElement(By.xpath(add_Operator));
            addOperatorButton.click();
            Thread.sleep(DELAY_BETWEEN_STEPS);

            //add the details of the operator
            WebElement UserNameButton = driver.findElement(By.xpath(add_Operator_Username_Textbox));
            UserNameButton.sendKeys(username);
            WebElement PasswordButton = driver.findElement(By.xpath(add_Operator_Password_Textbox));
            PasswordButton.sendKeys(password);
            Thread.sleep(DELAY_BETWEEN_STEPS);

            WebElement saveButton = driver.findElement(By.xpath(add_Operator_Save_Button));
            saveButton.click();

            //handles the opened window
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            alert.accept();

            Thread.sleep(DELAY_BETWEEN_STEPS);
        }
    }

    @When("Remove the operator with the username {string}")
    public void reomoveOperator(String username) throws InterruptedException {
        List<WebElement> adminCells = driver.findElements(By.cssSelector(Participants_Table));
        int result = check_if_exists_record_with_email(adminCells, username);
        if(result != -1){
            adminCells.get(result).click();

            //click on the delete button to remove the user
            WebElement deleteButton = driver.findElement(By.xpath(Remove_Operator_Button));
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
        else {
            fail("the operator " + username + " was not found");
        }
    }

    @Then("The operator with username {string} removed successfully")
    public void theOperatorRemovedSuccessfully(String username) throws InterruptedException {
        List<WebElement> operatorsCells = driver.findElements(By.cssSelector(Participants_Table));
        assertEquals(check_if_exists_record_with_email(operatorsCells, username), -1);
        Thread.sleep(2500);
    }

    @And("The operator with username {string} and password {string} can't log in")
    public void operatorCantLogin(String username, String password) throws InterruptedException {
        driver.get(main_Menu_URL);
        try {
            WebElement disconnectButton = driver.findElement(By.xpath(disconnect_Button));
            disconnectButton.click();
        }
        catch (Exception ignored) {

        }
        WebElement UserNameButton = driver.findElement(By.xpath(Username_Text_Box));
        UserNameButton.sendKeys(username);
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement PasswordButton = driver.findElement(By.xpath(Password_Text_Box));
        PasswordButton.sendKeys(password);
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement loginButton = driver.findElement(By.xpath(LoginButton_In_Login_Page));
        loginButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        try {
            WebElement disconnectButton = driver.findElement(By.xpath(disconnect_Button));
            disconnectButton.click();
            fail();
        }
        catch (Exception ignored) {
            assertTrue(true);
        }
        driver.quit();
    }

    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------
    // 5th scenario: Operator can see Lobbies and Session Data functionality.
    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------

    @Given("Exists an operator with the username {string} and password {string}")
    public void existsOperator(String username, String password) throws InterruptedException {
        driver = new ChromeDriver();
        driver.get(URL);
        Thread.sleep(2500);

        WebElement UserNameButton = driver.findElement(By.xpath(Username_Text_Box));
        UserNameButton.sendKeys(this.username);
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement PasswordButton = driver.findElement(By.xpath(Password_Text_Box));
        PasswordButton.sendKeys(this.password);
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement loginButton = driver.findElement(By.xpath(LoginButton_In_Login_Page));
        loginButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement Operators_Button = driver.findElement(By.xpath(operators_Button));
        Operators_Button.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        List<WebElement> adminCells = driver.findElements(By.cssSelector(Participants_Table));
        int result = check_if_exists_record_with_email(adminCells, username);
        if(result == -1){
            WebElement addOperatorButton = driver.findElement(By.xpath(add_Operator));
            addOperatorButton.click();
            Thread.sleep(DELAY_BETWEEN_STEPS);

            //add the details of the operator
            WebElement userNameButton = driver.findElement(By.xpath(add_Operator_Username_Textbox));
            userNameButton.sendKeys(username);
            WebElement passwordButton = driver.findElement(By.xpath(add_Operator_Password_Textbox));
            passwordButton.sendKeys(password);
            Thread.sleep(DELAY_BETWEEN_STEPS);

            WebElement saveButton = driver.findElement(By.xpath(add_Operator_Save_Button));
            saveButton.click();

            //handles the opened window
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            alert.accept();

            Thread.sleep(DELAY_BETWEEN_STEPS);
        }
    }

    @And("The operator with the username {string} and password {string} is logged in")
    public void operatorLogin(String username, String password) throws InterruptedException {
        driver.get(main_Menu_URL);
        try {
            WebElement disconnectButton = driver.findElement(By.xpath(disconnect_Button));
            disconnectButton.click();
        }
        catch (Exception ignored) {

        }
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

    @When("The operator is in the main menu page")
    public void operatorIsInMainMenu() throws InterruptedException {
        driver.get(main_Menu_URL);
    }

    @Then("The operator can see the Lobbies and Session Data buttons")
    public void operatorCanSeeTheLobbiesAndSessionData() throws InterruptedException {
        try {
            WebElement lobbiesButton = driver.findElement(By.xpath(lobbies_Button));
            lobbiesButton.click();
            WebElement sessionDataButton = driver.findElement(By.xpath(session_Data_Button));
            sessionDataButton.click();
        }
        catch (Exception e) {
            fail("can't find the button");
        }
        driver.quit();
    }

    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------
    // 6th scenario: Operator can't see Participants and Operators functionality.
    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------

    @Then("The operator can't see the Participants and Operators buttons")
    public void operatorCantSeeTheParticipantsAndOperators() throws InterruptedException {
        try {
            WebElement participantsButton = driver.findElement(By.xpath(participants_Button));
            participantsButton.click();
            fail("Can find the participant button");
        }
        catch (WebDriverException e) {
            assertTrue(true);
        }
        catch (Exception e) {
            assertTrue(true);
        }
        try {
            WebElement sessionDataButton = driver.findElement(By.xpath(session_Data_Button));
            sessionDataButton.click();
            fail("Can find the session data button");
        }
        catch (WebDriverException e) {
            assertTrue(true);
        }
        catch (Exception e) {
            assertTrue(true);
        }
        driver.quit();
    }

    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------
    // 7th scenario: Admin creates a lobby.
    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------

    @And("There is a participant with the email {string} and name {string} {string}")
    public void existsParticipantWithTheEmail(String email, String firstname, String lastname) throws InterruptedException {
        driver.get(main_Menu_URL);
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
            Thread.sleep(DELAY_BETWEEN_STEPS);
        }
        WebElement addParticipantButton = driver.findElement(By.xpath(Add_Participant_Button));
        addParticipantButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        //add the details of the participant
        WebElement UserNameButton = driver.findElement(By.xpath(New_Participant_Name_Textbox));
        UserNameButton.sendKeys(firstname);
        WebElement LastNameButton = driver.findElement(By.xpath(New_Participant_Last_Name_Textbox));
        LastNameButton.sendKeys(lastname);
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

    @When("Creates a lobby with the participants {string} {string} and {string} {string}")
    public void createsLobby(String firstname1, String lastname1, String firstname2, String lastname2) throws InterruptedException {
        driver.get(main_Menu_URL);
        WebElement lobbiesButton = driver.findElement(By.xpath(lobbies_Button));
        lobbiesButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement createLobbyButton = driver.findElement(By.xpath(create_Lobby_Button));
        createLobbyButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        List<WebElement> rows = driver.findElements(By.xpath(lobby_Participants_Table));
        for (WebElement row : rows) {
            try {
                WebElement nameCell = row.findElement(By.xpath("./td[2]"));
                String name = nameCell.getText().trim();

                if (name.compareTo(firstname1 + " " + lastname1) == 0) {
                    WebElement checkbox = row.findElement(By.xpath("./td[3]/input[@type='checkbox']"));
                    if (!checkbox.isSelected()) {
                        checkbox.click();
                    }
                }
                else if (name.compareTo(firstname2 + " " + lastname2) == 0) {
                    WebElement checkbox = row.findElement(By.xpath("./td[3]/input[@type='checkbox']"));
                    if (!checkbox.isSelected()) {
                        checkbox.click();
                    }
                }
            } catch (Exception e) {
                fail("can't find the lobby participants table");
            }
        }

        WebElement saveLobbyButton = driver.findElement(By.xpath(save_Lobby_Button));
        saveLobbyButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);
    }

    @Then("The lobby with the participants {string} {string} and {string} {string} created successfully")
    public void lobbyExists(String firstname1, String lastname1, String firstname2, String lastname2) throws InterruptedException {
        List<WebElement> lobbyCells = driver.findElements(By.cssSelector(Participants_Table));
        assertNotEquals(lobbyCells.size(), 0);
        Thread.sleep(2500);

        //remove it for initialize the state of the system
        lobbyCells.get(lobbyCells.size()-1).click();

        //click on the delete button to remove the user
        WebElement deleteButton = driver.findElement(By.xpath(remove_Lobby_Buton));
        deleteButton.click();

        //accept the alert
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.alertIsPresent());
        Alert alert = driver.switchTo().alert();
        alert.accept();
        driver.quit();
    }

    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------
    // 8th scenario: Admin removes a lobby.
    // -----------------------------------------------------------------------------
    // -----------------------------------------------------------------------------

    @When("Exists a lobby with the participants {string} {string} and {string} {string}")
    public void lobbyExist(String firstname1, String lastname1, String firstname2, String lastname2) throws InterruptedException {
        driver.get(main_Menu_URL);
        WebElement lobbiesButton = driver.findElement(By.xpath(lobbies_Button));
        lobbiesButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        WebElement createLobbyButton = driver.findElement(By.xpath(create_Lobby_Button));
        createLobbyButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);

        List<WebElement> rows = driver.findElements(By.xpath(lobby_Participants_Table));
        for (WebElement row : rows) {
            try {
                WebElement nameCell = row.findElement(By.xpath("./td[2]"));
                String name = nameCell.getText().trim();

                if (name.compareTo(firstname1 + " " + lastname1) == 0) {
                    WebElement checkbox = row.findElement(By.xpath("./td[3]/input[@type='checkbox']"));
                    if (!checkbox.isSelected()) {
                        checkbox.click();
                    }
                }
                else if (name.compareTo(firstname2 + " " + lastname2) == 0) {
                    WebElement checkbox = row.findElement(By.xpath("./td[3]/input[@type='checkbox']"));
                    if (!checkbox.isSelected()) {
                        checkbox.click();
                    }
                }
            } catch (Exception e) {
                fail("can't find the lobby participants table");
            }
        }

        WebElement saveLobbyButton = driver.findElement(By.xpath(save_Lobby_Button));
        saveLobbyButton.click();
        Thread.sleep(DELAY_BETWEEN_STEPS);
    }

    @Then("The lobby with the participants {string} {string} and {string} {string} can removed successfully")
    public void removeLobby(String firstname1, String lastname1, String firstname2, String lastname2) throws InterruptedException {
        List<WebElement> lobbyCells = driver.findElements(By.cssSelector(Participants_Table));
        assertNotEquals(lobbyCells.size(), 0);
        Thread.sleep(2500);

        try {
            //remove it for initialize the state of the system
            lobbyCells.get(lobbyCells.size() - 1).click();

            //click on the delete button to remove the user
            WebElement deleteButton = driver.findElement(By.xpath(remove_Lobby_Buton));
            deleteButton.click();

            //accept the alert
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.alertIsPresent());
            Alert alert = driver.switchTo().alert();
            alert.accept();
        }
        catch (WebDriverException e) {
            fail("cant find the lobby participants table");
        }
        catch (Exception e) {
            fail("can't find the lobby participants table");
        }
        driver.quit();
    }
}
