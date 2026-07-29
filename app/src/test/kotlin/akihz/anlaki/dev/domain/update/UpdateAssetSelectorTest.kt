package akihz.anlaki.dev.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateAssetSelectorTest {
    private val tag = "v1.2.3"

    @Test
    fun selectsFirstSupportedAbi() {
        val result = selectUpdateApkName(
            assetNames = listOf(
                "akihz-$tag-armeabi-v7a.apk",
                "akihz-$tag-arm64-v8a.apk",
                "akihz-$tag-universal.apk"
            ),
            tag = tag,
            supportedAbis = listOf("arm64-v8a", "armeabi-v7a")
        )

        assertEquals("akihz-$tag-arm64-v8a.apk", result)
    }

    @Test
    fun fallsBackToUniversalApk() {
        val result = selectUpdateApkName(
            assetNames = listOf("akihz-$tag-universal.apk"),
            tag = tag,
            supportedAbis = listOf("x86_64")
        )

        assertEquals("akihz-$tag-universal.apk", result)
    }

    @Test
    fun rejectsUnrelatedApks() {
        val result = selectUpdateApkName(
            assetNames = listOf("other-$tag-arm64-v8a.apk"),
            tag = tag,
            supportedAbis = listOf("arm64-v8a")
        )

        assertNull(result)
    }
}
