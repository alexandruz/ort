/*
 * Copyright (C) 2021 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.plugins.packagecurationproviders.clearlydefined

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.anyUrl
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.core.WireMockConfiguration

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.should

import java.time.Duration

import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.Package
import org.ossreviewtoolkit.utils.ort.OkHttpClientHelper

/**
 * A test for [ClearlyDefinedPackageCurationProvider], which uses a mock server. This allows testing some specific
 * error conditions.
 */
class ClearlyDefinedPackageCurationProviderTest : WordSpec({
    val server = WireMockServer(
        WireMockConfiguration.options()
            .dynamicPort()
            .usingFilesUnderDirectory("src/test/assets/")
    )

    beforeSpec {
        server.start()
    }

    afterSpec {
        server.stop()
    }

    beforeEach {
        server.resetAll()
    }

    "ClearlyDefinedPackageCurationProvider" should {
        "handle a SocketTimeoutException" {
            server.stubFor(
                get(anyUrl())
                    .willReturn(aResponse().withFixedDelay(100))
            )
            val client = OkHttpClientHelper.buildClient {
                readTimeout(Duration.ofMillis(1))
            }

            val provider = ClearlyDefinedPackageCurationProvider(
                config = ClearlyDefinedPackageCurationProviderConfig(
                    serverUrl = "http://localhost:${server.port()}",
                    0
                ),
                client = client
            )

            val id = Identifier("Maven:some-ns:some-component:1.2.3")
            val packages = listOf(Package.EMPTY.copy(id = id))

            provider.getCurationsFor(packages) should beEmpty()
        }
    }
})
