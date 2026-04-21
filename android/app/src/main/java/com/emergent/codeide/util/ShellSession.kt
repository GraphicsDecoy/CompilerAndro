package com.emergent.codeide.util

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * A persistent interactive shell backed by /system/bin/sh. Each session holds its own process
 * with stdout/stderr merged and streamed back as lines. Commands are written to stdin.
 * Lightweight: one thread per active session for reading, only spawned while attached.
 */
class ShellSession(
    private val workingDir: File,
    private val shellPath: String = "/system/bin/sh",
    extraEnv: Map<String, String> = emptyMap(),
) {
    val output = MutableSharedFlow<String>(extraBufferCapacity = 512)
    private var process: Process? = null
    private var writer: OutputStreamWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val history = ConcurrentLinkedQueue<String>()

    fun start() {
        if (process != null) return
        val pb = ProcessBuilder(shellPath, "-i")
            .redirectErrorStream(true)
            .directory(workingDir)
        val env = pb.environment()
        // Inject Termux PATH if available
        val termuxBin = "/data/data/com.termux/files/usr/bin"
        val termuxPrefix = "/data/data/com.termux/files/usr"
        if (File(termuxBin).exists()) {
            env["PATH"] = "$termuxBin:" + (env["PATH"] ?: System.getenv("PATH") ?: "/system/bin")
            env["PREFIX"] = termuxPrefix
            env["HOME"] = "/data/data/com.termux/files/home"
            env["LD_LIBRARY_PATH"] = "$termuxPrefix/lib"
        }
        env["TERM"] = "xterm-256color"
        env["PS1"] = "$ "
        env.putAll(extraEnv())

        val p = pb.start()
        process = p
        writer = OutputStreamWriter(p.outputStream)

        scope.launch {
            BufferedReader(InputStreamReader(p.inputStream)).use { r ->
                val buf = CharArray(1024)
                while (isActive) {
                    val n = r.read(buf)
                    if (n <= 0) break
                    output.emit(String(buf, 0, n))
                }
            }
        }
    }

    private val extraEnvMap = extraEnv
    private fun extraEnv(): Map<String, String> = extraEnvMap

    fun send(cmd: String) {
        history.add(cmd)
        if (history.size > 200) history.poll()
        writer?.apply {
            write(cmd)
            write("\n")
            flush()
        }
    }

    fun sendSignal(ctrlC: Boolean = true) {
        // Best-effort: write ^C character to stdin
        writer?.apply { write(3); flush() }
    }

    fun stop() {
        try { writer?.close() } catch (_: Exception) {}
        try { process?.destroy() } catch (_: Exception) {}
        process = null
        writer = null
        scope.cancel()
    }

    fun isRunning(): Boolean = process?.isAlive == true
}
