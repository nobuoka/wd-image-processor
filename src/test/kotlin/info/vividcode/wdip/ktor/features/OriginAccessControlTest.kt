package info.vividcode.wdip.ktor.features

import io.ktor.application.install
import io.ktor.http.Headers
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class OriginAccessControlTest {

    @Test
    internal fun standard() = withTestApplication {
        application.install(OriginAccessControl) {
            accessControlAllowOrigins.add("test.com")
            accessControlAllowOrigins.add("b.test.org")
        }

        // No origin header
        handleRequest {
        }.let { call ->
            Assertions.assertEquals(
                    Headers.build {
                        set("Vary", "Origin")
                    },
                    call.response.headers.allValues()
            )
        }

        // Not allowed origins
        listOf("test.org", "other.info").forEach { testOrigin ->
            handleRequest {
                addHeader("Origin", testOrigin)
            }.let { call ->
                Assertions.assertEquals(
                        Headers.build {
                            set("Vary", "Origin")
                        },
                        call.response.headers.allValues()
                )
            }
        }

        // Allowed origins
        listOf("test.com", "b.test.org").forEach { testOrigin ->
            handleRequest {
                addHeader("Origin", testOrigin)
            }.let { call ->
                Assertions.assertEquals(
                        Headers.build {
                            set("Vary", "Origin")
                            set("Access-Control-Allow-Origin", testOrigin)
                        },
                        call.response.headers.allValues()
                )
            }
        }
    }

    @Test
    internal fun withoutConfiguration() = withTestApplication {
        application.install(OriginAccessControl)

        // No origin header
        handleRequest {
        }.let { call ->
            Assertions.assertEquals(
                    Headers.build {
                        set("Vary", "Origin")
                    },
                    call.response.headers.allValues()
            )
        }

        // Not allowed origins
        listOf("test.org", "other.info").forEach { testOrigin ->
            handleRequest {
                addHeader("Origin", testOrigin)
            }.let { call ->
                Assertions.assertEquals(
                        Headers.build {
                            set("Vary", "Origin")
                        },
                        call.response.headers.allValues()
                )
            }
        }
    }

}
