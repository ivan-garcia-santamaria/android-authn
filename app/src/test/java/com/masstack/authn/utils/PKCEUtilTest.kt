package com.masstack.authn.utils

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for PKCEUtil
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PKCEUtilTest {

    @Test
    fun `generateCodeVerifier should return base64url encoded string`() {
        val codeVerifier = PKCEUtil.generateCodeVerifier()

        // Should not be empty
        assertNotNull(codeVerifier)
        assertTrue(codeVerifier.isNotEmpty())

        // Should be URL-safe base64 (no +, /, or =)
        assertFalse(codeVerifier.contains("+"))
        assertFalse(codeVerifier.contains("/"))
        assertFalse(codeVerifier.contains("="))
    }

    @Test
    fun `generateCodeVerifier should generate different values each time`() {
        val verifier1 = PKCEUtil.generateCodeVerifier()
        val verifier2 = PKCEUtil.generateCodeVerifier()

        assertNotEquals(verifier1, verifier2)
    }

    @Test
    fun `generateCodeChallenge should return base64url encoded SHA256 hash`() {
        val codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val codeChallenge = PKCEUtil.generateCodeChallenge(codeVerifier)

        // Should not be empty
        assertNotNull(codeChallenge)
        assertTrue(codeChallenge.isNotEmpty())

        // Should be URL-safe base64
        assertFalse(codeChallenge.contains("+"))
        assertFalse(codeChallenge.contains("/"))
        assertFalse(codeChallenge.contains("="))

        // Should be deterministic (same input = same output)
        val codeChallenge2 = PKCEUtil.generateCodeChallenge(codeVerifier)
        assertEquals(codeChallenge, codeChallenge2)
    }

    @Test
    fun `generateCodeChallenge should match RFC 7636 example`() {
        // Example from RFC 7636 Appendix B
        val codeVerifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val expectedChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"

        val actualChallenge = PKCEUtil.generateCodeChallenge(codeVerifier)

        assertEquals(expectedChallenge, actualChallenge)
    }

    @Test
    fun `generatePKCEPair should return valid pair`() {
        val pair = PKCEUtil.generatePKCEPair()

        assertNotNull(pair)
        assertNotNull(pair.codeVerifier)
        assertNotNull(pair.codeChallenge)
        assertTrue(pair.codeVerifier.isNotEmpty())
        assertTrue(pair.codeChallenge.isNotEmpty())

        // Verify challenge is derived from verifier
        val expectedChallenge = PKCEUtil.generateCodeChallenge(pair.codeVerifier)
        assertEquals(expectedChallenge, pair.codeChallenge)
    }

    @Test
    fun `generatePKCEPair should generate unique pairs`() {
        val pair1 = PKCEUtil.generatePKCEPair()
        val pair2 = PKCEUtil.generatePKCEPair()

        assertNotEquals(pair1.codeVerifier, pair2.codeVerifier)
        assertNotEquals(pair1.codeChallenge, pair2.codeChallenge)
    }

    @Test
    fun `code verifier should be at least 43 characters long`() {
        // Per RFC 7636, code verifier should be 43-128 characters
        val codeVerifier = PKCEUtil.generateCodeVerifier()
        assertTrue(codeVerifier.length >= 43)
    }

    @Test
    fun `code verifier should be at most 128 characters long`() {
        val codeVerifier = PKCEUtil.generateCodeVerifier()
        assertTrue(codeVerifier.length <= 128)
    }
}
