package com.eventosdami.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

class AlmacenamientoConfigTest {

	@Test
	void usaCredencialesTemporalesDelLabCuandoHaySessionToken() {
		var credenciales = AlmacenamientoConfig.proveedorDeCredenciales("ASIAEXAMPLE", "secret", "token-de-sesion")
			.resolveCredentials();

		assertThat(credenciales).isInstanceOf(AwsSessionCredentials.class);
		assertThat(credenciales.accessKeyId()).isEqualTo("ASIAEXAMPLE");
		assertThat(((AwsSessionCredentials) credenciales).sessionToken()).isEqualTo("token-de-sesion");
	}

	@Test
	void usaCredencialesBasicasCuandoNoHaySessionToken() {
		var credenciales = AlmacenamientoConfig.proveedorDeCredenciales("AKIAEXAMPLE", "secret", "  ")
			.resolveCredentials();

		assertThat(credenciales).isInstanceOf(AwsBasicCredentials.class);
		assertThat(credenciales.accessKeyId()).isEqualTo("AKIAEXAMPLE");
	}

	@Test
	void exigeSessionTokenSiLaAccessKeyEsDelLab() {
		assertThatThrownBy(() -> AlmacenamientoConfig.proveedorDeCredenciales("ASIAEXAMPLE", "secret", ""))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("AWS_SESSION_TOKEN");
	}

	@Test
	void sinClavesEnElEnvUsaLaCadenaPorDefectoDelSdk() {
		assertThat(AlmacenamientoConfig.proveedorDeCredenciales("", "", null))
			.isInstanceOf(DefaultCredentialsProvider.class);
	}

}
