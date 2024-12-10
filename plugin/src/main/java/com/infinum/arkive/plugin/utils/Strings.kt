package com.infinum.arkive.plugin.utils

val String.capFirst
    get() = this.replaceFirstChar(Char::titlecase)