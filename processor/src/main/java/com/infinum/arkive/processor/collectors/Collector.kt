package com.infinum.arkive.processor.collectors

internal interface Collector<Holder> {

    fun collect(): Set<Holder>
}
