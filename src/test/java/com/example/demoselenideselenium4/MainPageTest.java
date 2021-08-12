package com.example.demoselenideselenium4;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.*;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.devtools.CdpInfo;
import org.openqa.selenium.devtools.CdpVersionFinder;
import org.openqa.selenium.devtools.Connection;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.noop.NoOpCdpInfo;
import org.openqa.selenium.devtools.v91.fetch.Fetch;
import org.openqa.selenium.devtools.v91.fetch.model.HeaderEntry;
import org.openqa.selenium.devtools.v91.network.Network;
import org.openqa.selenium.devtools.v91.network.model.AuthChallengeResponse;
import org.openqa.selenium.devtools.v91.network.model.Request;
import org.openqa.selenium.devtools.v91.network.model.RequestId;
import org.openqa.selenium.devtools.v91.network.model.RequestWillBeSent;
import org.openqa.selenium.devtools.v91.performance.Performance;
import org.openqa.selenium.devtools.v91.performance.model.Metric;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.HttpClient;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.empty;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Selenide.*;

public class MainPageTest {
    @BeforeAll
    public static void setUpAll() {
        Configuration.browserSize = "1280x800";
    }

    @Test
    public void metricsTest() {
        DevTools devTools = getLocalDevTools();
        devTools.send(Performance.enable(empty()));
        open("https://www.jetbrains.com/");
        List<Metric> send = devTools.send(Performance.getMetrics());
        assertTrue(send.size() > 0);
        send.forEach(it -> System.out.printf("%s: %s%n", it.getName(), it.getValue()));
    }

    @Test
    public void getResponseTest() {
        DevTools devTools = getLocalDevTools();
        devTools.send(Network.enable(empty(), empty(), empty()));

        final List<RequestWillBeSent> requests = new ArrayList<>();
        final List<String> responses = new ArrayList<>();
        devTools.addListener(Network.requestWillBeSent(), req -> {
            if (req.getRequest().getUrl().contains("proxy/services/credit-application/async/api/external/v1/request/parameters")) {
                requests.add(req);
            }
        });

        devTools.addListener(Network.responseReceived(),
                entry -> {
                    if (requests.size() == 0) {
                        return;
                    }
                    RequestWillBeSent request = requests.get(0);
                    if (request != null && entry.getRequestId().toString().equals(request.getRequestId().toString())) {
                        Network.GetResponseBodyResponse send = devTools.send(Network.getResponseBody(request.getRequestId()));
                        responses.add(send.getBody());
                    }
                });
        open("https://www.sberbank.ru/ru/person/credits/money/consumer_unsecured/zayavka");
        await().pollThread(Thread::new)
                .atMost(10, TimeUnit.SECONDS)
                .until(() -> responses.size() == 1);

        System.out.println(responses.get(0));
    }


    @Test
    public void interceptRequestTest() {
        DevTools devTools = getLocalDevTools();
        devTools.send(Fetch.enable(empty(), empty()));
        String content = "[{\"title\":\"Todo 1\",\"order\":null,\"completed\":false},{\"title\":\"Todo 2\",\"order\":null,\"completed\":true}]";
        devTools.addListener(Fetch.requestPaused(), request -> {
            String url = request.getRequest().getUrl();
            String query = getUrl(url);
            if (url.contains("/todos/") && query == null) {
                List<HeaderEntry> corsHeaders = new ArrayList<>();
                corsHeaders.add(new HeaderEntry("Access-Control-Allow-Origin", "*"));
                corsHeaders.add(new HeaderEntry("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE"));
                devTools.send(Fetch.fulfillRequest(
                        request.getRequestId(),
                        200,
                        Optional.of(corsHeaders),
                        Optional.empty(),
                        Optional.of(Base64.getEncoder().encodeToString(content.getBytes())),
                        Optional.of("OK"))
                );
            } else {
                devTools.send(Fetch.continueRequest(
                        request.getRequestId(),
                        Optional.of(url),
                        Optional.of(request.getRequest().getMethod()),
                        request.getRequest().getPostData(),
                        request.getResponseHeaders()));
            }
        });
        open("https://todobackend.com/client/index.html?https://todo-backend-spring4-java8.herokuapp.com/todos/");
        $$("#todo-list li").shouldHave(CollectionCondition.size(2));
        $$("#todo-list label").shouldHave(CollectionCondition.texts("Todo 1", "Todo 2"));
    }

    private String getUrl(String url) {
        try {
            return new URL(url).getQuery();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private DevTools getLocalDevTools() {
        open();
        ChromeDriver driver = (ChromeDriver) WebDriverRunner.getWebDriver();
        DevTools devTools = driver.getDevTools();
        devTools.createSession();
        return devTools;
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
