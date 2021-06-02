package com.riyazuddin.zing.other

import android.content.Context
import android.content.res.Resources
import android.util.Patterns
import com.riyazuddin.zing.R
import com.riyazuddin.zing.other.Constants.INVALID
import com.riyazuddin.zing.other.Constants.MAX_NAME
import com.riyazuddin.zing.other.Constants.MAX_PASSWORD
import com.riyazuddin.zing.other.Constants.MAX_USERNAME
import com.riyazuddin.zing.other.Constants.MIN_PASSWORD
import com.riyazuddin.zing.other.Constants.MIN_USERNAME
import com.riyazuddin.zing.other.Constants.VALID
import dagger.hilt.android.qualifiers.ApplicationContext
import org.jetbrains.annotations.TestOnly
import java.util.regex.Pattern
import javax.inject.Inject

class Validator constructor(private val context: Context) {

    companion object {
        const val USERNAME_EXPRESSION = "^[a-zA-Z][a-zA-Z0-9_]{2,14}$"
        const val NAME_EXPRESSION = "^[a-zA-Z][a-zA-Z ]{0,19}$"
    }

    fun validateName(name: String): String {
        if (name.isEmpty())
            return context.getString(R.string.error_name_empty)
        if (name.length > MAX_NAME)
            return context.getString(R.string.error_name_too_long, MAX_NAME)
        if (!Pattern.matches(NAME_EXPRESSION, name))
            return INVALID

        return VALID
    }

    fun validateUsername(username: String): String {
        if (username.isEmpty())
            return context.getString(R.string.error_username_empty)
        if (username.length < MIN_USERNAME)
            return context.getString(R.string.error_username_too_short, MIN_USERNAME)
        if (username.length > MAX_USERNAME)
            return context.getString(R.string.error_username_too_long, MAX_USERNAME)
        if (!(Pattern.matches(USERNAME_EXPRESSION, username)))
            return context.getString(R.string.error_username_valid_message)
        return VALID
    }

    fun validateEmail(email: String): String {
        if (email.isEmpty())
            return context.getString(R.string.error_email_empty)
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return context.getString(R.string.error_not_a_valid_email)

        return VALID
    }

    fun validatePassword(password: String): String {
        if (password.isEmpty())
            return context.getString(R.string.error_password_empty)
        if (password.length < MIN_PASSWORD)
            return context.getString(R.string.error_password_too_short, MIN_PASSWORD)
        if (password.length > MAX_PASSWORD)
            return context.getString(R.string.error_password_too_long, MAX_PASSWORD)

        return VALID
    }

}