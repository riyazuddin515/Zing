package com.riyazuddin.zing.ui.auth.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.riyazuddin.zing.MainCoroutineRule
import com.riyazuddin.zing.getOrAwaitValueTest
import com.riyazuddin.zing.repositories.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AuthViewModelTest {

    @get:Rule()
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        viewModel = AuthViewModel(FakeAuthRepository())
    }

    @Test
    fun register_with_correct_details() {
        viewModel.register("test", "test123", "test@gmail.com", "12345678")
        val result = viewModel.registerStatus.getOrAwaitValueTest()
        assertThat(result.getContentHasBeenHandled()?.data).isTrue()
    }

    @Test
    fun register_with_email_already_exits() {
        viewModel.register("test", "test123", "test@gail.com", "12345678")
        viewModel.register("test1", "test1234", "test@gail.com", "12345678")
        val result = viewModel.registerStatus.getOrAwaitValueTest()
        assertThat(result.getContentHasBeenHandled()?.data).isFalse()
    }

    @Test
    fun login_with_no_email_exits() {
        viewModel.login("test@gmail.com","12345678")
        val result = viewModel.loginStatus.getOrAwaitValueTest()
        assertThat(result.getContentHasBeenHandled()?.data).isFalse()
    }

    @Test
    fun login_with_wrong_password(){
        viewModel.register("Riyaz","riyazuddin515","riyazuddin515@gmail.com","12345678")
        viewModel.login("riyazuddin515@gmail.com","123456")
        val result = viewModel.loginStatus.getOrAwaitValueTest()
        assertThat(result.getContentHasBeenHandled()?.data).isFalse()
    }

    @Test
    fun login_with_correct_details(){
        viewModel.register("Riyaz","riyazuddin515","riyazuddin515@gmail.com","12345678")
        viewModel.login("riyazuddin515@gmail.com","12345678")
        val result = viewModel.loginStatus.getOrAwaitValueTest()
        assertThat(result.getContentHasBeenHandled()?.data).isTrue()
    }

    @Test
    fun send_password_reset_no_email_exits(){
        viewModel.sendPasswordResetLink("riyazuddin@gmail.com")
        val result = viewModel.passwordResetStatus.getOrAwaitValueTest()
        assertThat(result.getContentHasBeenHandled()?.message).isEqualTo("No user record found")
    }
    @Test
    fun send_password_reset_email_exits(){
        viewModel.sendPasswordResetLink("riyazuddin515@gmail.com")
        val result = viewModel.passwordResetStatus.getOrAwaitValueTest()
        assertThat(result.getContentHasBeenHandled()?.data).isEqualTo("Mail Sent")
    }

    @Test
    fun search_username_that_not_exits() {
        viewModel.searchUsername("riyazuddin922")
        val result = viewModel.isUsernameAvailable.getOrAwaitValueTest()
        assertThat(result.getContentHasBeenHandled()?.data).isTrue()
    }
    @Test
    fun search_username_that_exits(){
        viewModel.searchUsername("riyazuddin515")
        val result = viewModel.isUsernameAvailable.getOrAwaitValueTest()
        assertThat(result.getContentHasBeenHandled()?.message).isEqualTo("Already taken")
    }

}