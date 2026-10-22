package com.fredrickemmanuel.studyzen.feature.auth

/** Client-side form checks. Each returns an error message, or null when the value is fine. */
object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 8
    const val CODE_LENGTH = 6

    private val emailRegex = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

    fun nameError(name: String): String? =
        if (name.trim().length < 2) "Enter your full name" else null

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Enter your email address"
        !emailRegex.matches(email.trim()) -> "Enter a valid email address"
        else -> null
    }

    /** For creating or resetting a password. */
    fun newPasswordError(password: String): String? = when {
        password.isEmpty() -> "Enter a password"
        password.length < MIN_PASSWORD_LENGTH -> "Use at least $MIN_PASSWORD_LENGTH characters"
        !hasLetterAndDigit(password) -> "Include at least one letter and one number"
        else -> null
    }

    /** For logging in: only checks that something was typed. The server decides if it is right. */
    fun loginPasswordError(password: String): String? =
        if (password.isEmpty()) "Enter your password" else null

    fun confirmPasswordError(password: String, confirmation: String): String? = when {
        confirmation.isEmpty() -> "Confirm your password"
        confirmation != password -> "Passwords don't match"
        else -> null
    }

    fun codeError(code: String): String? =
        if (code.length != CODE_LENGTH) "Enter the $CODE_LENGTH-digit code" else null

    fun hasMinLength(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH

    fun hasLetterAndDigit(password: String): Boolean =
        password.any { it.isLetter() } && password.any { it.isDigit() }
}
