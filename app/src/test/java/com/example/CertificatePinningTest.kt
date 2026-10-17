package com.example

import com.example.data.api.BabyBuddyApiClient
import com.example.data.api.UntrustedCertificateException
import com.example.data.api.certificateFingerprint
import com.example.data.api.findInChain
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.net.ssl.SSLHandshakeException

/** "Allow self-signed certificates" trusts only the certificate the user confirmed, never any certificate. */
class CertificatePinningTest {
    private val selfSigned = HeldCertificate.Builder().commonName("babybuddy.lan").build() // names neither localhost nor the IP
    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.useHttps(HandshakeCertificates.Builder().heldCertificate(selfSigned).build().sslSocketFactory())
        server.start()
    }

    @After
    fun tearDown() = server.close()

    private suspend fun connect(allowSelfSigned: Boolean, pin: String = ""): Throwable? {
        server.enqueue(MockResponse.Builder().body("""{"count":0,"next":null,"previous":null,"results":[]}""").build())
        val api = BabyBuddyApiClient.createService(server.url("/").toString(), allowSelfSigned, pinnedCertSha256 = pin)
        return runCatching { api.getChildren("Token test") }.exceptionOrNull()
    }

    private fun Throwable?.untrusted() = this?.findInChain { it is UntrustedCertificateException } as UntrustedCertificateException?

    @Test
    fun unknownCertificateIsRejectedWithItsFingerprint() = runTest {
        val untrusted = connect(allowSelfSigned = true).untrusted()
        assertEquals(certificateFingerprint(selfSigned.certificate), untrusted?.fingerprint)
    }

    @Test
    fun pinnedCertificateIsTrustedWhateverTheHostName() = runTest {
        assertNull(connect(allowSelfSigned = true, pin = certificateFingerprint(selfSigned.certificate).lowercase()))
    }

    @Test
    fun otherCertificateIsRejectedWhenOneIsPinned() = runTest {
        val other = certificateFingerprint(HeldCertificate.Builder().build().certificate)
        val untrusted = connect(allowSelfSigned = true, pin = other).untrusted()
        assertEquals(certificateFingerprint(selfSigned.certificate), untrusted?.fingerprint)
    }

    @Test
    fun selfSignedIsRejectedWhenNotAllowed() = runTest {
        val error = connect(allowSelfSigned = false)
        assertTrue(error?.findInChain { it is SSLHandshakeException } != null)
        assertNull(error.untrusted())
    }
}
