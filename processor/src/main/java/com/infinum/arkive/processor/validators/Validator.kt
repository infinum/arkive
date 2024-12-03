package com.infinum.arkive.processor.validators

internal interface Validator<Holder> {

    fun validate(elements: Set<Holder>): Set<Holder>
}
