package com.infinum.arkive.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSDeclaration
import java.io.File

//val packages: Sequence<String> by lazy {
//    val path = packagesPath?:return@lazy emptySequence()
//    val packagesFile = File(path)
//    packagesFile.readLines().asSequence()
//}
//
//@OptIn(KspExperimental::class)
//fun Resolver.getMainSrcDeclaration(): Sequence<KSDeclaration> {
//    return packages.flatMap {
//        getDeclarationsFromPackage(it)
//    }
//}