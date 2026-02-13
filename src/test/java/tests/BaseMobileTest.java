package tests;

import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseMobileTest {

  protected AndroidDriver driver;

  @BeforeAll
  public void setUp() {
    try {
      String appiumUrl = env("APPIUM_SERVER_URL", "http://127.0.0.1:4723/wd/hub");
      String appPath = env("APP_PATH", "C:/Users/DELL/nutritionx-mobile-tests/app/com-nutritionix-nixtrack-108070-68972947-7a2e0bbaa9efd46f8309d9b00273d551.apk");

      // Convert relative path to absolute path for Appium
      File appFile = new File(appPath);
      if (!appFile.isAbsolute()) {
        String projectRoot = System.getProperty("user.dir");
        appFile = new File(projectRoot, appPath);
        appPath = appFile.getAbsolutePath();
      }

      if (!appFile.exists()) {
        throw new RuntimeException("APK file not found at: " + appPath);
      }

      DesiredCapabilities caps = new DesiredCapabilities();
      caps.setCapability("platformName", "Android");
      caps.setCapability("appium:automationName", "UiAutomator2");
      caps.setCapability("appium:deviceName", env("DEVICE_NAME", "Android Device"));
      caps.setCapability("appium:app", appPath);

      caps.setCapability("appium:autoGrantPermissions", true);
      caps.setCapability("appium:noReset", true);
      caps.setCapability("appium:newCommandTimeout", 120);
      caps.setCapability("appium:uiautomator2ServerInstallTimeout", 60000);
      caps.setCapability("appium:adbExecTimeout", 60000);
      caps.setCapability("appium:enforceXPath1", true);

      String udid = System.getenv("UDID");
      if (udid != null && !udid.isBlank()) {
        caps.setCapability("appium:udid", udid);
      }

      driver = new AndroidDriver(new URL(appiumUrl), caps);

      // Selenium 3 signature
      driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);

    } catch (Exception e) {
      throw new RuntimeException("Failed to create AndroidDriver", e);
    }
  }

  @AfterAll
  public void tearDown() {
    if (driver != null) driver.quit();
  }

  protected String env(String key, String defaultValue) {
    String v = System.getenv(key);
    return (v == null || v.isBlank()) ? defaultValue : v;
  }
}
