package com.drunkhex.roothide

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Метод выполняет скрипты shell в отдельном потоке.
 *
 * @param command shell скрипт.
 */

class MainActivity : AppCompatActivity() {
    private lateinit var m_textView: TextView
    private lateinit var m_button: Button
    private lateinit var m_textField: TextView

    private var m_suHidden: Boolean? = null

    private lateinit var m_suShell: ShellController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        m_suHidden = f_getSuHiddenStatus()
        f_initShell()

        setContentView(R.layout.activity_main)
        m_textView = findViewById(R.id.textView)
        m_button = findViewById(R.id.button)
        m_button.setOnClickListener(::changeSuHiddenState)
        m_textField = findViewById(R.id.textfield)

        f_setSuHiddenText(m_suHidden!!) // either throws before initialization or has boolean value
        m_textField.append("INF: SU status: " + if (m_suHidden!!) "hidden\n" else "not hidden\n")
    }

    private fun f_initShell() {
        if (m_suHidden!!) {
            m_suShell = ShellController(ShellController.HIDDEN_SU_SHELL_PATH, true)
        } else {
            m_suShell = ShellController(ShellController.SU_SHELL_PATH, true)
        }
    }

//    data class ShellReturnValue(val exitCode: Int, val stdout: String)

//    private fun showToast(text: String) =
//        Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    fun f_getSuHiddenStatus(): Boolean {
        val result =
            ShellController(ShellController.SH_SHELL_PATH, true).execCommand(
                "ls /system/xbin/su_",
                false
            )

        if (result.exitCode != 0 && result.exitCode != 1) { // 0 - exists; 1 - doesn't exist
            m_textField.append("ERR: Checking if SU is hidden failed with exit code " + result.exitCode + "\n")

            if (m_suHidden == null) {
                throw RuntimeException("Could not check initial SU state. Exit code " + result.exitCode + "\n")
            }
        }

        return result.exitCode == 0
    }

    private fun appendLogMessage(
        successMessage: String,
        failMessage: String,
        shellReturnValue: ShellController.ShellReturnValue
    ) {
//        m_textField.append("DBG: Command output:\n" + shellReturnValue.stdout + "\n")

        if (shellReturnValue.exitCode != 0) {
            m_textField.append(failMessage + " Exit code: " + shellReturnValue.exitCode.toString() + "\n" + shellReturnValue.stdout + "\n")
        } else {
            m_textField.append(successMessage + "\n")
        }
    }

    fun changeSuHiddenState(@Suppress("UNUSED_PARAMETER") view: View?) {
        var shellResult = m_suShell.execCommand("/system/bin/mount -o rw,remount /system", false)
        appendLogMessage(
            "INF: Successfully remounted '/system' to read-write mode.",
            "ERR: Failed to remount '/system' to read-write mode.",
            shellResult
        )

        if (m_suHidden!!) {
            shellResult = m_suShell.execCommand("mv /system/xbin/su_ /system/xbin/su", false)
            appendLogMessage(
                "INF: Successfully renamed 'su_' to 'su'.",
                "ERR: Failed to rename 'su_'",
                shellResult
            )
        } else {
            shellResult = m_suShell.execCommand("mv /system/xbin/su /system/xbin/su_", false)
            appendLogMessage(
                "INF: Successfully renamed 'su_' to 'su'.",
                "ERR: Failed to rename 'su_'",
                shellResult
            )
        }

        shellResult = m_suShell.execCommand("/system/bin/mount -o ro,remount /system", false)
        appendLogMessage(
            "INF: Successfully remounted '/system' to read-only mode.",
            "ERR: Failed to remount '/system' to read-only mode.",
            shellResult
        )

        m_suHidden = f_getSuHiddenStatus()
        f_setSuHiddenText(m_suHidden!!)
        //f_initShell()
    }

    fun f_setSuHiddenText(isHidden: Boolean) {
        m_textView.text = if (isHidden) "SU is hidden" else "SU is not hidden"
    }
}