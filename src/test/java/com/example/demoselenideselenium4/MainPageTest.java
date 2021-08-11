package com.example.demoselenideselenium4;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.*;
import org.openqa.selenium.BuildInfo;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.CdpInfo;
import org.openqa.selenium.devtools.CdpVersionFinder;
import org.openqa.selenium.devtools.Connection;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.noop.NoOpCdpInfo;
import org.openqa.selenium.devtools.v91.performance.Performance;
import org.openqa.selenium.devtools.v91.performance.model.Metric;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.HttpClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.*;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Selenide.*;

public class MainPageTest {
    @BeforeAll
    public static void setUpAll() {
        Configuration.browserSize = "1280x800";
    }

    @Test
    public void search() {
        open();
        ChromeDriver driver = (ChromeDriver) WebDriverRunner.getWebDriver();
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        devTools.send(Performance.enable(empty()));

        open("https://www.jetbrains.com/");
        List<Metric> send = devTools.send(Performance.getMetrics());
        assertTrue(send.size() > 0);
        send.forEach( it -> System.out.printf("%s: %s%n", it.getName(), it.getValue()));
    }

    @Test
    public void devtoolsWithSelenoid() throws URISyntaxException {
        Configuration.remote = "http://localhost:4444/wd/hub";
        open();
        RemoteWebDriver webDriver = (RemoteWebDriver) WebDriverRunner.getWebDriver();
        Capabilities capabilities = webDriver.getCapabilities();
        CdpInfo cdpInfo = new CdpVersionFinder()
                .match(capabilities.getBrowserVersion())
                .orElseGet(NoOpCdpInfo::new);
        HttpClient.Factory factory = HttpClient.Factory.createDefault();
        URI uri = new URI(String.format("ws://localhost:4444/devtools/%s", webDriver.getSessionId()));
        Connection connection = new Connection(
                factory.createClient(ClientConfig.defaultConfig().baseUri(uri)),
                uri.toString());
        DevTools devTools = new DevTools(cdpInfo::getDomains, connection);
        devTools.createSession();
        devTools.send(Performance.enable(empty()));

        open("https://www.jetbrains.com/");
        List<Metric> send = devTools.send(Performance.getMetrics());
        assertTrue(send.size() > 0);
        send.forEach(it -> System.out.printf("%s: %s%n", it.getName(), it.getValue()));
    }
}
