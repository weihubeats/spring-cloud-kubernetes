/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.kubernetes.fabric8.config.labeled_secret_with_prefix;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author wind57
 */
abstract class LabeledSecretWithPrefixTests {

	private static KubernetesClient mockClient;

	@Autowired
	private WebTestClient webClient;

	static void setUpBeforeClass(KubernetesClient mockClient) {
		LabeledSecretWithPrefixTests.mockClient = mockClient;
		// Configure the kubernetes master url to point to the mock server
		System.setProperty(Config.KUBERNETES_MASTER_SYSTEM_PROPERTY, mockClient.getConfiguration().getMasterUrl());
		System.setProperty(Config.KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
		System.setProperty(Config.KUBERNETES_AUTH_TRYKUBECONFIG_SYSTEM_PROPERTY, "false");
		System.setProperty(Config.KUBERNETES_AUTH_TRYSERVICEACCOUNT_SYSTEM_PROPERTY, "false");
		System.setProperty(Config.KUBERNETES_NAMESPACE_SYSTEM_PROPERTY, "test");
		System.setProperty(Config.KUBERNETES_HTTP2_DISABLE, "true");

		Map<String, String> one = Collections.singletonMap("one.property",
				Base64.getEncoder().encodeToString("one".getBytes(StandardCharsets.UTF_8)));

		createSecret("secret-one", one, Collections.singletonMap("letter", "a"));

		Map<String, String> two = Collections.singletonMap("property",
				Base64.getEncoder().encodeToString("two".getBytes(StandardCharsets.UTF_8)));
		createSecret("secret-two", two, Collections.singletonMap("letter", "b"));

		Map<String, String> three = Collections.singletonMap("property",
				Base64.getEncoder().encodeToString("three".getBytes(StandardCharsets.UTF_8)));
		createSecret("secret-three", three, Collections.singletonMap("letter", "c"));

		Map<String, String> four = Collections.singletonMap("property",
				Base64.getEncoder().encodeToString("four".getBytes(StandardCharsets.UTF_8)));
		createSecret("secret-four", four, Collections.singletonMap("letter", "d"));

	}

	private static void createSecret(String name, Map<String, String> data, Map<String, String> labels) {
		mockClient.secrets().inNamespace("spring-k8s").resource(new SecretBuilder().withNewMetadata().withName(name)
				.withLabels(labels).endMetadata().addToData(data).build()).create();
	}

	/**
	 * <pre>
	 *   'spring.cloud.kubernetes.secrets.useNameAsPrefix=true'
	 *   'spring.cloud.kubernetes.secrets.sources[0].useNameAsPrefix=false'
	 * 	 ("one.property", "one")
	 *
	 * 	 As such: @ConfigurationProperties("one")
	 * </pre>
	 */
	@Test
	void testOne() {
		this.webClient.get().uri("/labeled-secret/prefix/one").exchange().expectStatus().isOk().expectBody(String.class)
				.value(Matchers.equalTo("one"));
	}

	/**
	 * <pre>
	 *   'spring.cloud.kubernetes.secrets.useNameAsPrefix=true'
	 *   'spring.cloud.kubernetes.secrets.sources[1].explicitPrefix=two'
	 * 	 ("property", "two")
	 *
	 * 	 As such: @ConfigurationProperties("two")
	 * </pre>
	 */
	@Test
	void testTwo() {
		this.webClient.get().uri("/labeled-secret/prefix/two").exchange().expectStatus().isOk().expectBody(String.class)
				.value(Matchers.equalTo("two"));
	}

	/**
	 * <pre>
	 *   'spring.cloud.kubernetes.secrets.useNameAsPrefix=true'
	 *   'spring.cloud.kubernetes.secrets.sources[2].labels=letter:c'
	 * 	 ("property", "three")
	 *
	 *   We find the secret by labels, and use it's name as the prefix.
	 *
	 * 	 As such: @ConfigurationProperties(prefix = "secret-three")
	 * </pre>
	 */
	@Test
	void testThree() {
		this.webClient.get().uri("/labeled-secret/prefix/three").exchange().expectStatus().isOk()
				.expectBody(String.class).value(Matchers.equalTo("three"));
	}

	/**
	 * <pre>
	 *   'spring.cloud.kubernetes.secrets.useNameAsPrefix=true'
	 *   'spring.cloud.kubernetes.secrets.sources[3].labels=letter:d'
	 * 	 ("property", "four")
	 *
	 *   We find the secret by labels, and use it's name as the prefix.
	 *
	 * 	 As such: @ConfigurationProperties(prefix = "secret-four")
	 * </pre>
	 */
	@Test
	void testFour() {
		this.webClient.get().uri("/labeled-secret/prefix/four").exchange().expectStatus().isOk()
				.expectBody(String.class).value(Matchers.equalTo("four"));
	}

}
