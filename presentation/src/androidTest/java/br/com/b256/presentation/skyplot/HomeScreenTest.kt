package br.com.b256.presentation.skyplot

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * Teste de referência de UI em Compose: exercita a versão "stateless" de [SkyPlotScreen]
 * (ver `SkyPlotScreen.kt`), sem precisar de Hilt/ViewModel, e verifica o comportamento via
 * semantics (`onNodeWithText`/`onNodeWithContentDescription`) em vez de referências diretas a
 * componentes internos.
 */
class HomeScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun exibeOCabecalhoDaTela() {
        composeTestRule.setContent {
            SkyPlotScreen(
                gnssState = null,
                locationState = null,
                orientationState = null,
            )
        }

        composeTestRule.onNodeWithText("GNSS SKYPLOT").assertIsDisplayed()
    }

    @Test
    fun oPainelDeFotosEOBotaoDeCameraFicamSempreVisiveis() {
        composeTestRule.setContent {
            SkyPlotScreen(
                gnssState = null,
                locationState = null,
                orientationState = null,
                photos = emptyList(),
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Photograph position")
            .assertIsDisplayed()
    }

    @Test
    fun tocarNaCameraSemPosicaoAvisaOUsuario() {
        var unavailable = false

        composeTestRule.setContent {
            SkyPlotScreen(
                gnssState = null,
                locationState = null,
                orientationState = null,
                onCaptureUnavailable = { unavailable = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Photograph position").performClick()

        assert(unavailable)
    }
}
