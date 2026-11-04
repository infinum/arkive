# UI-tree capture (com.infinum.arkive.composeutils.uitree) only runs in Arkive's generated
# unit tests, where the Gradle plugin injects ui-tooling-data. composeUtils compiles against it
# compileOnly, so a consumer's minified release build must not fail on the absent classes.
-dontwarn androidx.compose.ui.tooling.data.**
