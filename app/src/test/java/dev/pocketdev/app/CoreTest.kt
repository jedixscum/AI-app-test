package dev.pocketdev.app

import dev.pocketdev.app.core.CommandResult
import dev.pocketdev.app.core.Endpoints
import dev.pocketdev.app.core.InferenceLocation
import dev.pocketdev.app.core.ProjectNames
import dev.pocketdev.app.core.Redactor
import dev.pocketdev.app.core.Shell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreTest {
    @Test fun succeededRequiresExitZeroAndNoTermuxError() {
        assertTrue(CommandResult(0, "ok", "").succeeded)
        assertFalse(CommandResult(1, "", "boom").succeeded)
        assertFalse(CommandResult(null, "", "").succeeded)
        assertFalse(CommandResult(0, "", "", errorCode = 5).succeeded)
        assertFalse(CommandResult.timeout().succeeded)
        assertFalse(CommandResult.failure("x").succeeded)
    }

    @Test fun shellQuoteEscapesSingleQuotes() {
        assertEquals("'abc'", Shell.quote("abc"))
        assertEquals("'a'\\''b'", Shell.quote("a'b"))
        assertEquals("'; rm -rf ~'", Shell.quote("; rm -rf ~"))
    }

    @Test fun projectNameValidation() {
        assertTrue(ProjectNames.isValid("my-app_1.0"))
        assertFalse(ProjectNames.isValid(""))
        assertFalse(ProjectNames.isValid("../etc"))
        assertFalse(ProjectNames.isValid("a b"))
        assertFalse(ProjectNames.isValid("-rf"))
        assertFalse(ProjectNames.isValid("x".repeat(65)))
    }

    @Test fun redactorHidesSecrets() {
        assertEquals("password: [redacted]", Redactor.redact("password: hunter2hunter2"))
        assertFalse(Redactor.redact("token ghp_abcdefghijklmnopqrstuvwxyz0123").contains("ghp_"))
        assertFalse(Redactor.redact("key sk-abcdefghijklmnop1234").contains("sk-abc"))
        assertEquals("Authorization: Bearer [redacted]", Redactor.redact("Authorization: Bearer abc.def"))
        assertEquals("nothing secret", Redactor.redact("nothing secret"))
    }

    @Test fun inferenceLocationOnlyLoopbackIsLocal() {
        assertEquals(InferenceLocation.LOCAL, Endpoints.inferenceLocation("http://127.0.0.1:11434"))
        assertEquals(InferenceLocation.LOCAL, Endpoints.inferenceLocation("http://localhost:11434"))
        assertEquals(InferenceLocation.REMOTE, Endpoints.inferenceLocation("https://api.example.com/v1"))
        assertEquals(InferenceLocation.REMOTE, Endpoints.inferenceLocation("http://192.168.1.20:11434"))
        assertEquals(InferenceLocation.REMOTE, Endpoints.inferenceLocation("not a url"))
    }
}
