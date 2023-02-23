@file:Suppress("unused")

package com.doktech.doktechcodeformatplugin.utils

/**
 * Check if we are in kotlin file content
 */
fun String.isKotlinFileContent(): Boolean =
    this.startsWith(prefix = "data class") ||
            this.startsWith(prefix = "interface") ||
            this.startsWith(prefix = "class") ||
            this.startsWith(prefix = "sealed class") ||
            this.startsWith(prefix = "annotation class") ||
            this.startsWith(prefix = "enum class") ||
            this.startsWith(prefix = "sealed interface")

/**
 * check if variable start with val or var
 */
fun String.isVarOrVal(): Boolean = this == "val" || this == "var"


fun String.isSpaceBeforeDoubleQuote(): Boolean =
    this == " \""
fun String.isOnlyDoubleQuote(): Boolean =
    this == "\""

fun String.isContainsDoubleQuote(): Boolean =
    this.contains("\"")