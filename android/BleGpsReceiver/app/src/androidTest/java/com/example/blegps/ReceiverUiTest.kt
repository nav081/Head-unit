package com.example.blegps

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.blegps.ui.MainActivity
import org.junit.Rule
import org.junit.Test

class ReceiverUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun showsReceiverHomeTab() {
        composeTestRule.onNodeWithText("Receiver Home").assertIsDisplayed()
    }
}
