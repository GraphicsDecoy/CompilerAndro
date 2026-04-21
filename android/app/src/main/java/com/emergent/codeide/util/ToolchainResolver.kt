package com.emergent.codeide.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File

/**
 * Resolves available toolchain binaries on-device (gcc/g++/clang/clang++/python/node/cmake).
 * We prefer Termux-provided tools at /data/data/com.termux/files/usr/bin when accessible,
 * then fall back to anything on PATH (system images rarely ship compilers but we still try).
 */
object ToolchainResolver {

    private val candidateDirs = listOf(
        "/data/data/com.termux/files/usr/bin",
        "/system/bin",
        "/system/xbin",
        "/vendor/bin",
    )

    private val toolAliases = mapOf(
        "g++" to listOf("g++","clang++"),
        "gcc" to listOf("gcc","clang"),
        "clang++" to listOf("clang++","g++"),
        "clang" to listOf("clang","gcc"),
        "python" to listOf("python","python3"),
        "python3" to listOf("python3","python"),
        "node" to listOf("node","nodejs"),
        "cmake" to listOf("cmake"),
        "make" to listOf("make"),
        "sh" to listOf("sh","bash"),
    )

    /** Returns the first executable path resolving to the given logical tool, or null. */
    fun resolve(tool: String): String? {
        val names = toolAliases[tool] ?: listOf(tool)
        for (dir in candidateDirs) {
            for (n in names) {
                val f = File(dir, n)
                if (f.exists() && f.canExecute()) return f.absolutePath
            }
        }
        // PATH fallback
        System.getenv("PATH")?.split(":")?.forEach { p ->
            for (n in names) {
                val f = File(p, n)
                if (f.exists() && f.canExecute()) return f.absolutePath
            }
        }
        return null
    }

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.termux", 0); true
        } catch (e: Exception) { false }
    }

    fun openTermuxPlayStore(context: Context) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/"))
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    }
}
