package com.infinum.arkive.annotations

@MustBeDocumented
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
public annotation class ArkiveComposable(
    val name: String = "",
    val group: String = "",
    val skip: Boolean = false,
    val tags: Array<String> = [],
    val extraMetadata: Array<String> = [],
    val designNodeId: String = "",
)
