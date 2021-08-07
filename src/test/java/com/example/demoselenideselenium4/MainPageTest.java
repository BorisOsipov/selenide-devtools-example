package com.example.demoselenideselenium4;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;

public class MainPageTest {
    @BeforeAll
    public static void setUpAll() {
        Configuration.proxyEnabled = true;
        Configuration.browserSize = "1280x800";
    }

    @BeforeEach
    public void setUp() {
        open("https://www.jetbrains.com/");
    }

    @Test
    public void search() {
        $("[data-test='search-input']").sendKeys("Selenium");
        $("button[data-test='full-search-button']").click();
        $("input[data-test='search-input']").shouldHave(attribute("value", "Selenium"));
    }
}
