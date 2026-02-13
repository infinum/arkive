package com.infinum.arkive.annotations

@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class ArkiveView(
    val name: String = "",
    val group: String = "",
    val skip: Boolean = false,
    val tags: Array<String> = [],
    val extraMetadata: Array<String> = [],
)
