package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.feature.auth.AuthValidation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthValidationTest {

    @Test
    fun emailRules() {
        assertNotNull(AuthValidation.emailError(""))
        assertNotNull(AuthValidation.emailError("not-an-email"))
        assertNotNull(AuthValidation.emailError("a@b"))
        assertNull(AuthValidation.emailError("student@school.edu"))
        assertNull(AuthValidation.emailError("  student@school.edu  "))
    }

    @Test
    fun newPasswordRules() {
        assertNotNull(AuthValidation.newPasswordError(""))
        assertNotNull(AuthValidation.newPasswordError("short1"))
        assertNotNull(AuthValidation.newPasswordError("onlyletters"))
        assertNotNull(AuthValidation.newPasswordError("12345678"))
        assertNull(AuthValidation.newPasswordError("study2025"))
    }

    @Test
    fun confirmPasswordMustMatch() {
        assertNotNull(AuthValidation.confirmPasswordError("study2025", ""))
        assertNotNull(AuthValidation.confirmPasswordError("study2025", "study2026"))
        assertNull(AuthValidation.confirmPasswordError("study2025", "study2025"))
    }

    @Test
    fun codeMustBeSixDigits() {
        assertNotNull(AuthValidation.codeError("12345"))
        assertNull(AuthValidation.codeError("123456"))
    }

    @Test
    fun nameNeedsTwoCharacters() {
        assertNotNull(AuthValidation.nameError(" a "))
        assertNull(AuthValidation.nameError("Fredrick"))
    }

    @Test
    fun loginOnlyRequiresSomething() {
        assertNotNull(AuthValidation.loginPasswordError(""))
        assertEquals(null, AuthValidation.loginPasswordError("x"))
    }
}
