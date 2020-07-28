package com.drunkhex.roothide

import android.util.Log
import java.io.*
import java.util.function.LongBinaryOperator

class ShellController(
    shellPath: String,
    redirectStderr: Boolean = true,
    var ioTimeoutMs: Long = 100
) {
    private var m_shell =
        ProcessBuilder(shellPath).also { it.redirectErrorStream(redirectStderr) }.start()

    private val shellStdin = BufferedWriter(OutputStreamWriter(m_shell.outputStream))
    private val shellStdout = BufferedReader(InputStreamReader(m_shell.inputStream))
    private val shellStderr = BufferedReader(InputStreamReader(m_shell.errorStream))

    ////////////////////////////////////////////////////////////////////////////////////////////////
    data class ShellReturnValue(val exitCode: Int, val stdout: String, val stderr: String)

    ////////////////////////////////////////////////////////////////////////////////////////////////

    // blocks thread until read complete
    private fun f_readFullText(reader: BufferedReader, ioTimeoutMs: Long): String {
        if (!reader.ready()) {
            Thread.sleep(ioTimeoutMs)
        }
        val result = StringBuilder(64)
        while (reader.ready()) {
            result.append(reader.readLine() + "\n")
            Thread.sleep(ioTimeoutMs)
        }
        return result.toString()
    }

    // blocks thread until reset complete
    private fun f_clearReader(reader: BufferedReader, readTimeoutMillis: Long) {
        if (!reader.ready()) {
            Thread.sleep(readTimeoutMillis)
        }
        while (reader.ready()) {
            while (reader.skip(1024L) != 0L);
            Thread.sleep(readTimeoutMillis)
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    fun execCommand(commandLine: String,expectOutput: Boolean = true, ioTimeoutMs: Long = 0): ShellReturnValue {
        // reset streams so they don't contain output from other commands
        if (expectOutput) {
            f_clearReader(shellStdout, ioTimeoutMs)
            f_clearReader(shellStderr, ioTimeoutMs)
        }

        // execute command
        shellStdin.apply {
            append(commandLine)
            if (!expectOutput)
                append(" >/dev/null 2>&1")
            newLine()
            flush()
        }

        val stdout = if (expectOutput) f_readFullText(shellStdout, ioTimeoutMs) else ""
        val stderr = if (expectOutput) f_readFullText(shellStderr, ioTimeoutMs) else ""

        // get return code
        shellStdin.apply {
            append("echo $?")
            newLine()
            flush()
        }

        val exitCode = shellStdout.readLine().toInt()

        return ShellReturnValue(exitCode, stdout, stderr)
    }

    companion object {
        const val SU_SHELL_PATH = "/system/xbin/su"
        const val HIDDEN_SU_SHELL_PATH = "/system/xbin/su_"
        const val SH_SHELL_PATH = "/system/bin/sh"
    }
}