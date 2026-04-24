package com.example.blegps

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.rule.GrantPermissionRule
import com.example.blegps.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class ReceiverUiTest {
    private val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(
        GrantPermissionRule.grant(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.POST_NOTIFICATIONS
        )
    ).around(composeTestRule)

    @Test
    fun showsReceiverHomeTab() {
        composeTestRule.onNodeWithText("Receiver Home").assertIsDisplayed()
    }
}
