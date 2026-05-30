package com.masstack.authn.services

import android.content.Context
import android.util.Log
import com.masstack.authn.TestFixtures
import com.masstack.authn.data.models.ServerError
import com.masstack.authn.data.repositories.AuthRepository
import com.masstack.authn.utils.NetworkUtils
import io.mockk.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CibaServiceTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var context: Context
    private lateinit var service: CibaService

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0

        mockkObject(NetworkUtils)
        every { NetworkUtils.isNetworkAvailable(any()) } returns true
        every { NetworkUtils.getNetworkType(any()) } returns "WiFi"

        authRepository = mockk(relaxed = true)
        context = mockk(relaxed = true)

        service = CibaService(authRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        unmockkObject(NetworkUtils)
        unmockkAll()
    }

    @Test
    fun `should emit Idle then Error when cibaAuthorize fails`() = runTest {
        coEvery { authRepository.cibaAuthorize(any(), any()) } returns Result.failure(Exception("Auth failed"))

        val states = service.startCibaFlow(context, "testuser").toList()

        assertTrue(states[0] is CibaService.CibaState.Idle)
        assertTrue(states[1] is CibaService.CibaState.Error)
        assertEquals("Auth failed", (states[1] as CibaService.CibaState.Error).message)
    }

    @Test
    fun `should emit Error with ServerError code when auth fails with ServerError`() = runTest {
        val serverError = ServerError(code = 503, message = "Service Unavailable")
        coEvery { authRepository.cibaAuthorize(any(), any()) } returns Result.failure(serverError)

        val states = service.startCibaFlow(context, "testuser").toList()

        val error = states[1] as CibaService.CibaState.Error
        assertEquals("SERVER_503", error.errorCode)
        assertTrue(error.exception is ServerError)
    }

    @Test
    fun `should emit Idle, Authorizing, Polling, Success on immediate token`() = runTest {
        val cibaResponse = TestFixtures.cibaResponse(interval = 1, expiresIn = 10)
        coEvery { authRepository.cibaAuthorize(any(), any()) } returns Result.success(cibaResponse)

        val tokenResponse = TestFixtures.tokenResponse()
        coEvery { authRepository.cibaToken(any()) } returns Result.success(tokenResponse)

        val states = service.startCibaFlow(context, "testuser").toList()

        assertTrue(states[0] is CibaService.CibaState.Idle)
        assertTrue(states[1] is CibaService.CibaState.Authorizing)
        assertTrue(states[2] is CibaService.CibaState.Polling)
        assertTrue(states[3] is CibaService.CibaState.Success)
        assertEquals("test-access-token", (states[3] as CibaService.CibaState.Success).tokenResponse.accessToken)
    }

    @Test
    fun `should continue polling on authorization_pending error`() = runTest {
        val cibaResponse = TestFixtures.cibaResponse(interval = 1, expiresIn = 10)
        coEvery { authRepository.cibaAuthorize(any(), any()) } returns Result.success(cibaResponse)

        coEvery { authRepository.cibaToken(any()) } returnsMany listOf(
            Result.failure(Exception("authorization_pending")),
            Result.success(TestFixtures.tokenResponse())
        )

        val states = service.startCibaFlow(context, "testuser").toList()

        val successStates = states.filterIsInstance<CibaService.CibaState.Success>()
        assertEquals(1, successStates.size)

        val pollingStates = states.filterIsInstance<CibaService.CibaState.Polling>()
        assertEquals(2, pollingStates.size)
    }

    @Test
    fun `should emit Error on expired_token`() = runTest {
        val cibaResponse = TestFixtures.cibaResponse(interval = 1, expiresIn = 10)
        coEvery { authRepository.cibaAuthorize(any(), any()) } returns Result.success(cibaResponse)
        coEvery { authRepository.cibaToken(any()) } returns Result.failure(Exception("expired_token"))

        val states = service.startCibaFlow(context, "testuser").toList()

        val error = states.filterIsInstance<CibaService.CibaState.Error>().first()
        assertEquals("expired_token", error.errorCode)
    }

    @Test
    fun `should emit Error on access_denied`() = runTest {
        val cibaResponse = TestFixtures.cibaResponse(interval = 1, expiresIn = 10)
        coEvery { authRepository.cibaAuthorize(any(), any()) } returns Result.success(cibaResponse)
        coEvery { authRepository.cibaToken(any()) } returns Result.failure(Exception("access_denied"))

        val states = service.startCibaFlow(context, "testuser").toList()

        val error = states.filterIsInstance<CibaService.CibaState.Error>().first()
        assertEquals("access_denied", error.errorCode)
    }

    @Test
    fun `should emit Error on unknown error`() = runTest {
        val cibaResponse = TestFixtures.cibaResponse(interval = 1, expiresIn = 10)
        coEvery { authRepository.cibaAuthorize(any(), any()) } returns Result.success(cibaResponse)
        coEvery { authRepository.cibaToken(any()) } returns Result.failure(Exception("some_unknown_error"))

        val states = service.startCibaFlow(context, "testuser").toList()

        val error = states.filterIsInstance<CibaService.CibaState.Error>().first()
        assertTrue(error.message.contains("some_unknown_error"))
    }

    @Test
    fun `should emit Error when max attempts reached`() = runTest {
        val cibaResponse = TestFixtures.cibaResponse(interval = 1, expiresIn = 2)
        coEvery { authRepository.cibaAuthorize(any(), any()) } returns Result.success(cibaResponse)
        coEvery { authRepository.cibaToken(any()) } returns Result.failure(Exception("authorization_pending"))

        val states = service.startCibaFlow(context, "testuser").toList()

        val error = states.filterIsInstance<CibaService.CibaState.Error>().first()
        assertTrue(error.message.contains("Maximum polling attempts"))
    }

    @Test
    fun `should skip poll when network unavailable`() = runTest {
        val cibaResponse = TestFixtures.cibaResponse(interval = 1, expiresIn = 5)
        coEvery { authRepository.cibaAuthorize(any(), any()) } returns Result.success(cibaResponse)

        var networkCallCount = 0
        every { NetworkUtils.isNetworkAvailable(any()) } answers {
            networkCallCount++
            networkCallCount > 1
        }

        coEvery { authRepository.cibaToken(any()) } returns Result.success(TestFixtures.tokenResponse())

        val states = service.startCibaFlow(context, "testuser").toList()

        assertTrue(states.any { it is CibaService.CibaState.Success })
    }

    @Test
    fun `Authorizing state should contain correct data`() = runTest {
        val cibaResponse = TestFixtures.cibaResponse(authReqId = "req-123", expiresIn = 120, interval = 5)
        coEvery { authRepository.cibaAuthorize(any(), any()) } returns Result.success(cibaResponse)
        coEvery { authRepository.cibaToken(any()) } returns Result.success(TestFixtures.tokenResponse())

        val states = service.startCibaFlow(context, "testuser").toList()

        val authorizing = states.filterIsInstance<CibaService.CibaState.Authorizing>().first()
        assertEquals("req-123", authorizing.authReqId)
        assertEquals(120, authorizing.expiresIn)
        assertEquals(5, authorizing.interval)
    }
}
