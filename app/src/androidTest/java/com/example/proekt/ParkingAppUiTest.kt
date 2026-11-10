package com.example.proekt

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParkingAppUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNavigation_bottomBarTabsExist() {
        // Проверяваме, че разделите в долната навигация присъстват
        composeTestRule.onNodeWithText("Начало").assertIsDisplayed()
        composeTestRule.onNodeWithText("Абонаменти").assertIsDisplayed()
        composeTestRule.onNodeWithText("Бариера").assertIsDisplayed()
        composeTestRule.onNodeWithText("Профил").assertIsDisplayed()
    }

    @Test
    fun testNavigation_switchToSubscriptions() {
        // Превключване към раздел Абонаменти
        composeTestRule.onNodeWithText("Абонаменти").performClick()

        // Проверяваме, че екранът показва бутона за нов абонамент
        composeTestRule.onNodeWithText("Нов абонамент").assertIsDisplayed()
    }

    @Test
    fun testNavigation_switchToScanner() {
        // Превключване към терминала на бариерата
        composeTestRule.onNodeWithText("Бариера").performClick()

        // Проверяваме заглавието на терминала
        composeTestRule.onNodeWithText("Контролен терминал на бариерата").assertIsDisplayed()
        composeTestRule.onNodeWithText("Провери и задействай бариерата").assertIsDisplayed()
    }
}
