import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import com.infinum.arkive.ArkiveShoot
import org.junit.Rule
import org.junit.Test

// TODO: This class is going to be generated from another processor
class ArkiveTest {
    @get:Rule
    val paparazzi = Paparazzi(
        renderingMode = SessionParams.RenderingMode.SHRINK
    )

    @Test
    fun testAllComposableFunctions() {
        val shooter = ArkiveShoot()

        shooter.runTests { name, function ->
            paparazzi.snapshot(name = name) {
                function()
            }
        }
    }
}
