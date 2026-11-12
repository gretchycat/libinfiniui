package com.infinifolder.viewer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedDefaultLaunchPolicyTest {
    @Test fun installedSavedDefaultCanLaunchWhenMimeQueryOmitsIt() {
        assertTrue(SavedDefaultLaunchPolicy.includeExplicit(
            "com.infinimedia/com.infinimedia.InfiniMediaActivity", emptySet(), installedAndEnabled = true))
    }

    @Test fun alreadyCompatibleChoiceDoesNotNeedAnExplicitEntry() {
        assertFalse(SavedDefaultLaunchPolicy.includeExplicit(
            "com.infinimedia/com.infinimedia.InfiniMediaActivity",
            setOf("com.infinimedia/com.infinimedia.InfiniMediaActivity"), installedAndEnabled = true))
    }

    @Test fun absentOrUnavailableSavedDefaultIsNotInjected() {
        assertFalse(SavedDefaultLaunchPolicy.includeExplicit(null, emptySet(), installedAndEnabled = true))
        assertFalse(SavedDefaultLaunchPolicy.includeExplicit(
            "com.infinimedia/com.infinimedia.InfiniMediaActivity", emptySet(), installedAndEnabled = false))
    }
}
