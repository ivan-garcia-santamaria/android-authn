package com.masstack.authn.models

import com.masstack.authn.data.models.OpenIdConfiguration
import org.junit.Assert.*
import org.junit.Test

class OpenIdConfigurationTest {

    @Test
    fun `all endpoint fields should be nullable with null defaults`() {
        val config = OpenIdConfiguration()

        assertNull(config.issuer)
        assertNull(config.authorizationEndpoint)
        assertNull(config.tokenEndpoint)
        assertNull(config.endSessionEndpoint)
        assertNull(config.backchannelAuthenticationEndpoint)
        assertNull(config.webauthnRegistrationOptionsEndpoint)
        assertNull(config.webauthnRegistrationVerificationEndpoint)
        assertNull(config.webauthnAuthenticationOptionsEndpoint)
        assertNull(config.webauthnAuthenticationVerificationEndpoint)
    }

    @Test
    fun `should store all endpoint values when provided`() {
        val config = OpenIdConfiguration(
            issuer = "https://example.com",
            authorizationEndpoint = "https://example.com/authorize",
            tokenEndpoint = "https://example.com/token",
            endSessionEndpoint = "https://example.com/logout",
            backchannelAuthenticationEndpoint = "https://example.com/bc-authorize",
            webauthnRegistrationOptionsEndpoint = "https://example.com/webauthn/reg/options",
            webauthnRegistrationVerificationEndpoint = "https://example.com/webauthn/reg/verify",
            webauthnAuthenticationOptionsEndpoint = "https://example.com/webauthn/auth/options",
            webauthnAuthenticationVerificationEndpoint = "https://example.com/webauthn/auth/verify"
        )

        assertEquals("https://example.com", config.issuer)
        assertEquals("https://example.com/authorize", config.authorizationEndpoint)
        assertEquals("https://example.com/token", config.tokenEndpoint)
        assertEquals("https://example.com/logout", config.endSessionEndpoint)
        assertEquals("https://example.com/bc-authorize", config.backchannelAuthenticationEndpoint)
        assertEquals("https://example.com/webauthn/reg/options", config.webauthnRegistrationOptionsEndpoint)
        assertEquals("https://example.com/webauthn/reg/verify", config.webauthnRegistrationVerificationEndpoint)
        assertEquals("https://example.com/webauthn/auth/options", config.webauthnAuthenticationOptionsEndpoint)
        assertEquals("https://example.com/webauthn/auth/verify", config.webauthnAuthenticationVerificationEndpoint)
    }
}
