package com.untis.homework

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppearancePersistenceTest {
  @Test
  fun childAppearanceRoundTripsNormalizedProfileAndBackgroundColor() {
    val raw = ChildAppearance(theme = "forest", bgColor = "#FFCC00").toJsonString()
    val parsed = childAppearanceFromJson(raw)

    assertEquals("forest", parsed.theme)
    assertEquals("#ffcc00", parsed.bgColor)
    assertEquals("#ffcc00", parsed.backgroundHex())
  }

  @Test
  fun customBackgroundDoesNotOverrideSystemDarkTheme() {
    val parsed = childAppearanceFromJson("""{"theme":"forest","bgColor":"#FFCC00"}""")

    assertEquals("#ffcc00", parsed.backgroundHex(darkTheme = false))
    assertEquals("#14241a", parsed.backgroundHex(darkTheme = true))
  }

  @Test
  fun childAppearanceFallsBackWhenStoredValuesAreInvalid() {
    val parsed = childAppearanceFromJson("""{"theme":"unknown","bgColor":"red"}""")

    assertEquals("ocean", parsed.theme)
    assertEquals("", parsed.bgColor)
    assertEquals("#edf2f5", parsed.backgroundHex())
  }

  @Test
  fun childAppearanceUsesDarkProfileBackgroundWhenSystemIsDark() {
    val parsed = childAppearanceFromJson("""{"theme":"night"}""")

    assertEquals("#e9edf3", parsed.backgroundHex(darkTheme = false))
    assertEquals("#111827", parsed.backgroundHex(darkTheme = true))
  }

  @Test
  fun colorSchemeSwitchesBetweenLightAndDarkSurfaces() {
    val appearance = ChildAppearance(theme = "ocean")

    assertEquals("#edf2f5", appearance.backgroundHex(darkTheme = false))
    assertEquals(Color(0xFFFFFBF4), untisColorScheme(appearance, darkTheme = false).surface)
    assertEquals("#0f2024", appearance.backgroundHex(darkTheme = true))
    assertEquals(Color(0xFF171D22), untisColorScheme(appearance, darkTheme = true).surface)
  }

  @Test
  fun accentSwitchesBetweenLightAndDarkProfiles() {
    val appearance = ChildAppearance(theme = "ocean")

    assertEquals("#0d747a", appearance.accentHex(darkTheme = false))
    assertEquals("#6bd6d0", appearance.accentHex(darkTheme = true))
  }

  @Test
  fun subjectStylesRoundTripValidChildScopedColorsOnly() {
    val raw = subjectStylesToJson(
      mapOf(
        "student-one" to mapOf("M" to "#FFCC00", "D" to "red"),
        "student-two" to mapOf("E" to "#66a06b"),
      )
    )

    assertEquals(
      mapOf(
        "student-one" to mapOf("M" to "#ffcc00"),
        "student-two" to mapOf("E" to "#66a06b"),
      ),
      subjectStylesFromJson(raw),
    )
  }

  @Test
  fun hexColorNormalizerRequiresFullCssHexValues() {
    assertEquals("#0d747a", normalizeHexColor(" #0D747A "))
    assertNull(normalizeHexColor("#123"))
    assertNull(normalizeHexColor("blue"))
  }


}
