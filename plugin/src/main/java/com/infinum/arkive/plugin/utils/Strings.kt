package com.infinum.arkive.plugin.utils

internal val String.capFirst
    get() = this.replaceFirstChar(Char::titlecase)
