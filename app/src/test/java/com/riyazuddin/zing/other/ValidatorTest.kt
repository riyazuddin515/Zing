package com.riyazuddin.zing.other

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.riyazuddin.zing.R
import com.riyazuddin.zing.other.Constants.INVALID
import com.riyazuddin.zing.other.Constants.MAX_NAME
import com.riyazuddin.zing.other.Constants.MAX_PASSWORD
import com.riyazuddin.zing.other.Constants.MAX_USERNAME
import com.riyazuddin.zing.other.Constants.MIN_PASSWORD
import com.riyazuddin.zing.other.Constants.MIN_USERNAME
import com.riyazuddin.zing.other.Constants.VALID
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class ValidatorTest {

    private var context: Context = ApplicationProvider.getApplicationContext()
    private var validate: Validator = Validator(context)

    //---------------> Tests for Name <---------------------
    @Test
    fun name_empty() {
        val result = validate.validateName("")
        assertThat(result).isEqualTo(context.getString(R.string.error_name_empty))
    }

    @Test
    fun name_empty_with_one_space() {
        val result = validate.validateName(" ")
        assertThat(result).isEqualTo(INVALID)
    }


    @Test
    fun name_less_than_max() {
        val result = validate.validateName("Mohammed Fayazuddin")
        assertThat(result == VALID)
    }

    @Test
    fun name_max_length() {
        val result = validate.validateName("Mohammed Fayazuddin ")
        assertThat(result == VALID)
    }

    @Test
    fun name_more_than_max_length() {
        val result = validate.validateName("Mohammed Fayazuddin Ghaddha")
        assertThat(result).isEqualTo(context.getString(R.string.error_name_too_long, MAX_NAME))
    }

    fun name_number() {
        val result = validate.validateName("Fayaz786")
        assertThat(result).isEqualTo(context.getString(R.string.error_name_valid_message))
    }

    @Test
    fun name_symbol() {
        val result = validate.validateName("Fayaz$")
        assertThat(result).isEqualTo(INVALID)
    }

    @Test
    fun name_valid() {
        val result = validate.validateName("Fayazuddin")
        assertThat(result == VALID)
    }

    @Test
    fun name_valid_all_capital() {
        val result = validate.validateName("FAYAZUDDIN")
        assertThat(result == VALID)
    }

    @Test
    fun name_valid_all_small() {
        val result = validate.validateName("fayazuddin")
        assertThat(result == VALID)
    }

    //------------------------> Tests for Username <------------------

    @Test
    fun username_empty() {
        val result = validate.validateUsername("")
        assertThat(result).isEqualTo(context.getString(R.string.error_username_empty))
    }

    @Test
    fun username_less_than_min_length() {
        val result = validate.validateUsername("ri")
        assertThat(result).isEqualTo(context.getString(R.string.error_username_too_short, MIN_USERNAME))
    }

    @Test
    fun username_greater_than_max_length() {
        val result = validate.validateUsername("AppleAppleAppleAppleAppleApple")
        assertThat(result).isEqualTo(context.getString(R.string.error_username_too_long, MAX_USERNAME))
    }

    @Test
    fun username_starts_with_number() {
        val result = validate.validateUsername("1riyaz")
        assertThat(result).isEqualTo(context.getString(R.string.error_username_valid_message))
    }

    @Test
    fun username_starts_with_symbol() {
        val result = validate.validateUsername("@riy")
        assertThat(result).isEqualTo(context.getString(R.string.error_username_valid_message))
    }

    @Test
    fun username_valid() {
        val result = validate.validateUsername("riy")
        assertThat(result).isEqualTo(VALID)
    }

    @Test
    fun username_valid_1() {
        val result = validate.validateUsername("riyaz_515")
        assertThat(result).isEqualTo(VALID)
    }

    //----------------------> Tests for email <------------------->
    @Test
    fun email_empty() {
        val result = validate.validateEmail("")
        assertThat(result).isEqualTo(context.getString(R.string.error_email_empty))
    }

    @Test
    fun email_invalid() {
        val result = validate.validateEmail("123gmail.com")
        assertThat(result).isEqualTo(context.getString(R.string.error_not_a_valid_email))
    }

    @Test
    fun email_valid() {
        val result = validate.validateEmail("123@gmai.com")
        assertThat(result).isEqualTo(VALID)
    }


    //------------------> Tests for password <----------------->
    @Test
    fun password_empty() {
        val result = validate.validatePassword("")
        assertThat(result).isEqualTo(context.getString(R.string.error_password_empty))
    }

    @Test
    fun password_less_than_min_length() {
        val result = validate.validatePassword("1234")
        assertThat(result).isEqualTo(context.getString(R.string.error_password_too_short, MIN_PASSWORD))
    }

    @Test
    fun password_greater_than_max_length() {
        val result = validate.validatePassword("1234567890123456789012345")
        assertThat(result).isEqualTo(context.getString(R.string.error_password_too_long, MAX_PASSWORD))
    }

    @Test
    fun password_valid() {
        val result = validate.validatePassword("123456789")
        assertThat(result).isEqualTo(VALID)
    }

}