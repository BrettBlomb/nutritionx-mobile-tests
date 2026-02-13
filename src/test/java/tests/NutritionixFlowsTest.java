package tests;

import io.appium.java_client.MobileBy;
import io.appium.java_client.TouchAction;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.PointOption;

import java.time.Duration;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NutritionixFlowsTest extends BaseMobileTest {

  private WebDriverWait waitFor(int seconds) {
    // Selenium 3 uses seconds, not Duration
    return new WebDriverWait(driver, seconds);
  }
/*=====================================
               START HELPERS 
  =====================================*/
  private void dismissPopups() {
    try {
      // Wait for popup to potentially appear
      Thread.sleep(2000);
      By finishBtn = MobileBy.AndroidUIAutomator("new UiSelector().text(\"Finish\")");
      List<WebElement> elements = driver.findElements(finishBtn);
      if (!elements.isEmpty()) {
        elements.get(0).click();
        System.out.println("Popup dismissed.");
        Thread.sleep(500);
      }
    } catch (Exception ignored) {}
  }

  /** Dismiss popups, then wait and click */
  private void waitAndClick(By locator, int seconds) {
    dismissPopups();
    waitFor(seconds).until(ExpectedConditions.elementToBeClickable(locator)).click();
  }

  /** Dismiss popups, then wait and return element */
  private WebElement waitAndFind(By locator, int seconds) {
    dismissPopups();
    return waitFor(seconds).until(ExpectedConditions.elementToBeClickable(locator));
  }

  /** Dismiss popups, then type into element found by locator */
  private void typeInto(By locator, String text, int seconds) {
    dismissPopups();
    WebElement element = waitFor(seconds).until(ExpectedConditions.elementToBeClickable(locator));
    element.click();
    element.sendKeys(text);
  }

  /** Find the food entry EditText (has hint text starting with " e.g.") */
  private By getFoodEntryFieldLocator() {
    return MobileBy.AndroidUIAutomator("new UiSelector().className(\"android.widget.EditText\").textStartsWith(\" e.g.\")");
  }

  /** Scroll to and return the View Daily Summary element */
  private WebElement scrollToViewDailySummary() {
    By viewDailySummary = MobileBy.AndroidUIAutomator(
        "new UiScrollable(new UiSelector().scrollable(true)).setAsVerticalList()" +
            ".scrollToEnd(10).scrollIntoView(new UiSelector().text(\"View Daily Summary\"));"
    );
    return waitAndFind(viewDailySummary, 25);
  }

  /** Verify View Daily Summary is visible, throw if not */
  private void verifyViewDailySummaryVisible(String errorMessage) {
    WebElement summary = scrollToViewDailySummary();
    if (summary == null || !summary.isDisplayed()) {
      throw new RuntimeException(errorMessage);
    }
  }

  private void loginViaEmailIfNeeded() {
    // Wait for screen to load, then check if login button exists
    try {
      Thread.sleep(2000);
    } catch (InterruptedException ignored) {}

    By loginViaEmail = MobileBy.AndroidUIAutomator("new UiSelector().text(\"Login via Email\")");
    List<WebElement> loginElements = driver.findElements(loginViaEmail);

    if (loginElements.isEmpty()) {
      System.out.println("Already logged in, skipping login.");
      return;
    }

    // Not logged in, proceed with login
    loginElements.get(0).click();

    By emailField = By.xpath("//android.widget.TextView[@text='Email']/following::android.widget.EditText[1]");
    By passwordField = By.xpath("//android.widget.TextView[@text='Password']/following::android.widget.EditText[1]");

    String emailVal = env("NUTRITIONIX_EMAIL", "");
    String passVal = env("NUTRITIONIX_PASSWORD", "");

    WebElement email = waitFor(20).until(ExpectedConditions.elementToBeClickable(emailField));
    email.click();
    email.clear();
    email.sendKeys(emailVal);

    WebElement password = waitFor(20).until(ExpectedConditions.elementToBeClickable(passwordField));
    password.click();
    password.clear();
    password.sendKeys(passVal);

    By loginBtn = MobileBy.AndroidUIAutomator("new UiSelector().text(\"Login\")");
    waitFor(20).until(ExpectedConditions.elementToBeClickable(loginBtn)).click();
  }

  /** Swipe left on an element to reveal delete button */
  private void swipeLeftOnElement(WebElement element) {
    int centerX = element.getLocation().getX() + (element.getSize().getWidth() / 2);
    int centerY = element.getLocation().getY() + (element.getSize().getHeight() / 2);
    int endX = element.getLocation().getX() + 10;

    // Use mobile: swipeGesture for Appium 2.x
    java.util.Map<String, Object> params = new java.util.HashMap<>();
    params.put("left", element.getLocation().getX());
    params.put("top", element.getLocation().getY());
    params.put("width", element.getSize().getWidth());
    params.put("height", element.getSize().getHeight());
    params.put("direction", "left");
    params.put("percent", 0.75);

    driver.executeScript("mobile: swipeGesture", params);
  }

  /** Delete a food item by swiping left and tapping Delete. Returns true if successful. */
  private boolean deleteFoodItem(String foodName) {
    try {
      // Try to scroll to find the food item
      By foodLocator = MobileBy.AndroidUIAutomator(
          "new UiScrollable(new UiSelector().scrollable(true)).setAsVerticalList()" +
              ".scrollIntoView(new UiSelector().text(\"" + foodName + "\"));"
      );
      WebElement foodElement = waitAndFind(foodLocator, 10);

      if (foodElement != null) {
        swipeLeftOnElement(foodElement);
        Thread.sleep(500);

        By deleteBtn = MobileBy.AndroidUIAutomator("new UiSelector().text(\"Delete\")");
        waitAndClick(deleteBtn, 10);

        System.out.println("Deleted: " + foodName);
        return true;
      } else {
        System.out.println("Food not found: " + foodName);
        return false;
      }
    } catch (Exception e) {
      System.out.println("Could not delete " + foodName + ": " + e.getMessage());
      return false;
    }
  }
  /*=====================================
               END HELPERS 
  =======================================*/




  /*=====================================
               START TESTS 
  =======================================*/

  @Test
  @Order(1)
  public void logFoodByBrowsingAllFoods() {
    loginViaEmailIfNeeded();

    By searchBox = MobileBy.AndroidUIAutomator(
        "new UiSelector().className(\"android.widget.EditText\").text(\"Search foods to log\")"
    );
    waitAndClick(searchBox, 25);

    By browseAllFoods = MobileBy.AndroidUIAutomator(
        "new UiSelector().className(\"android.widget.TextView\").text(\"Browse All Foods\")"
    );
    waitAndClick(browseAllFoods, 25);

    // Type foods into the food entry field (second EditText, not the search box)
    String foods = "Soda, Turkey Sandwich, Yogurt";
    typeInto(getFoodEntryFieldLocator(), foods, 25);

    By addToBasketBtn = MobileBy.AndroidUIAutomator(
        "new UiSelector().className(\"android.widget.TextView\").text(\"Add to Basket\")"
    );
    waitAndClick(addToBasketBtn, 25);

    By logFoodBtn = MobileBy.AndroidUIAutomator("new UiSelector().textStartsWith(\"Log\")");
    waitAndClick(logFoodBtn, 25);

    // Verify we can see View Daily Summary (confirms food was logged and we're on main screen)
    verifyViewDailySummaryVisible("View Daily Summary not visible - food may not have been logged");
  }

  @Test
  @Order(2)
  public void createCustomFoodAndSave() {
    loginViaEmailIfNeeded();

    // Tap search bar first (common entry point)
    By searchBox = MobileBy.AndroidUIAutomator(
        "new UiSelector().className(\"android.widget.EditText\").text(\"Search foods to log\")"
    );
    waitAndClick(searchBox, 25);

    // Create Custom food
    By createCustomFoodBtn = MobileBy.AndroidUIAutomator(
        "new UiSelector().className(\"android.widget.TextView\").text(\"Create custom food\")"
    );
    waitAndClick(createCustomFoodBtn, 25);

    // Food name
    By foodNameField = By.xpath("//android.widget.TextView[@text='Food Name*']/following-sibling::android.widget.EditText");
    typeInto(foodNameField, "Big Mac", 25);

    // Calories
    By caloriesField = By.xpath("//android.widget.TextView[@text='Calories*']/following::android.widget.EditText[1]");
    typeInto(caloriesField, "563", 25);

    // Scroll to bottom and save/add
    By saveAndAddBtn = MobileBy.AndroidUIAutomator(
        "new UiScrollable(new UiSelector().scrollable(true)).setAsVerticalList()" +
            ".scrollToEnd(10).scrollIntoView(new UiSelector().textContains(\"Save and add\"));"
    );
    waitAndClick(saveAndAddBtn, 25);

    By logFoodBtn = MobileBy.AndroidUIAutomator("new UiSelector().textStartsWith(\"Log\")");
    waitAndClick(logFoodBtn, 25);

    // Verify we can see View Daily Summary (confirms food was logged and we're on main screen)
    verifyViewDailySummaryVisible("View Daily Summary not visible - food may not have been logged");
  }

  @Test
  @Order(3)
  public void viewDailySummary() {
    loginViaEmailIfNeeded();

    // Click View Daily Summary
    scrollToViewDailySummary().click();

    // Verify we're on the Daily Summary screen
    By sourceOfCalories = MobileBy.AndroidUIAutomator("new UiSelector().text(\"Source of Calories\")");
    WebElement element = waitAndFind(sourceOfCalories, 25);
    if (element == null || !element.isDisplayed()) {
      throw new RuntimeException("Source of Calories not visible - Daily Summary screen did not load");
    }

    // Navigate back
    driver.pressKey(new KeyEvent(AndroidKey.BACK));

    // Verify we can see View Daily Summary again (back on main screen)
    verifyViewDailySummaryVisible("View Daily Summary not visible - failed to navigate back to main screen");
  }

  

  @Test
  @Order(4)
  public void deleteAllAddedFoods() {
    loginViaEmailIfNeeded();

    // Delete each food item from main screen
    String[] foodsToDelete = {"Soda", "Turkey Sandwich", "Yogurt", "Big Mac"};
    List<String> failedDeletes = new java.util.ArrayList<>();

    for (String food : foodsToDelete) {
      if (!deleteFoodItem(food)) {
        failedDeletes.add(food);
      }
    }

    // Navigate to Custom Foods via hamburger menu
    By hamburger = By.xpath(
        "//*[@resource-id='com.nutritionix.nixtrack:id/action_bar_root']" +
        "//*[self::android.widget.ImageButton or self::android.view.ViewGroup][@clickable='true'][1]"
    );
    waitAndClick(hamburger, 25);

    By customFoods = MobileBy.AndroidUIAutomator("new UiSelector().text(\"Custom Foods\")");
    waitAndClick(customFoods, 25);

    // Delete Big Mac from Custom Foods
    if (!deleteFoodItem("Big Mac")) {
      failedDeletes.add("Big Mac (Custom Foods)");
    }

    if (!failedDeletes.isEmpty()) {
      throw new RuntimeException("Failed to delete the following items: " + String.join(", ", failedDeletes));
    }

    // Navigate back and verify View Daily Summary is visible
    driver.pressKey(new KeyEvent(AndroidKey.BACK));
    verifyViewDailySummaryVisible("View Daily Summary not visible after deleting foods");
  }

  @Test
  @Order(5)
  public void signOut() {
    loginViaEmailIfNeeded();

    // Open hamburger menu
    By hamburger = By.xpath(
        "//*[@resource-id='com.nutritionix.nixtrack:id/action_bar_root']" +
        "//*[self::android.widget.ImageButton or self::android.view.ViewGroup][@clickable='true'][1]"
    );
    waitAndClick(hamburger, 25);

    // Tap Signout
    By signoutBtn = MobileBy.AndroidUIAutomator("new UiSelector().text(\"Signout\")");
    waitAndClick(signoutBtn, 25);

    // Verify we can see Sign in with Facebook (confirms we're logged out)
    By signInWithFacebook = MobileBy.AndroidUIAutomator("new UiSelector().text(\"Sign in with Facebook\")");
    WebElement element = waitAndFind(signInWithFacebook, 25);
    if (element == null || !element.isDisplayed()) {
      throw new RuntimeException("Sign in with Facebook not visible - signout may have failed");
    }
  }
}
/*=======================================
               END TESTS
  =======================================*/