package dev.yarallex.shipyard.log

import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutput.Style
import org.gradle.internal.logging.text.StyledTextOutputFactory

class ShipyardLog(factory: StyledTextOutputFactory) {

    private val out: StyledTextOutput = factory.create("shipyard")

    fun ok(message: String) {
        out.style(Style.Success).text("✓ ").style(Style.Normal).println(message)
    }

    fun arrow(message: String) {
        out.style(Style.Info).text("→ ").style(Style.Normal).println(message)
    }

    fun warn(message: String) {
        out.style(Style.Description).println(message)
    }

    fun line(message: String = "") {
        out.style(Style.Normal).println(message)
    }

    fun header(message: String) {
        out.style(Style.Header).println(message)
    }

    fun divider(width: Int = DIVIDER_WIDTH) {
        out.style(Style.Description).println("─".repeat(width))
    }

    companion object {
        private const val DIVIDER_WIDTH = 60
    }
}
