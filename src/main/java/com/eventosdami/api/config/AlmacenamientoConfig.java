package com.eventosdami.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.eventosdami.api.almacenamiento.AlmacenResumenes;
import com.eventosdami.api.almacenamiento.S3AlmacenResumenes;

import io.awspring.cloud.s3.S3Operations;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

/**
 * Configura el almacenamiento de los resúmenes de compra en AWS S3.
 * <p>
 * El cliente {@code S3Client} y el {@link S3Operations} (S3Template) los crea
 * Spring Cloud AWS a partir de {@code spring.cloud.aws.*}. Esta clase aporta
 * el nombre del bucket y, si están en el {@code .env}, las credenciales.
 */
@Configuration(proxyBeanMethods = false)
public class AlmacenamientoConfig {

	/**
	 * Lee las credenciales del archivo {@code .env} (o de variables de entorno reales).
	 * El lab de AWS entrega una access key temporal ({@code ASIA...}) que solo funciona
	 * junto con {@code AWS_SESSION_TOKEN}. Si el {@code .env} no trae claves, se usa la
	 * cadena por defecto del SDK: {@code ~/.aws/credentials} o el rol IAM de EC2.
	 */
	@Bean
	@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "true", matchIfMissing = true)
	AwsCredentialsProvider awsCredentialsProvider(
			@Value("${AWS_ACCESS_KEY_ID:}") String accessKey,
			@Value("${AWS_SECRET_ACCESS_KEY:}") String secretKey,
			@Value("${AWS_SESSION_TOKEN:}") String sessionToken) {
		return proveedorDeCredenciales(accessKey, secretKey, sessionToken);
	}

	static AwsCredentialsProvider proveedorDeCredenciales(String accessKey, String secretKey, String sessionToken) {
		String access = accessKey == null ? "" : accessKey.trim();
		String secret = secretKey == null ? "" : secretKey.trim();
		String token = sessionToken == null ? "" : sessionToken.trim();

		if (!StringUtils.hasText(access) && !StringUtils.hasText(secret) && !StringUtils.hasText(token)) {
			return DefaultCredentialsProvider.create();
		}
		if (!StringUtils.hasText(access) || !StringUtils.hasText(secret)) {
			throw new IllegalStateException(
					"En .env faltan AWS_ACCESS_KEY_ID o AWS_SECRET_ACCESS_KEY.");
		}
		if (StringUtils.hasText(token)) {
			return StaticCredentialsProvider.create(AwsSessionCredentials.create(access, secret, token));
		}
		if (access.startsWith("ASIA")) {
			throw new IllegalStateException(
					"La access key del lab es temporal. Pega también AWS_SESSION_TOKEN en el archivo .env y reinicia.");
		}
		return StaticCredentialsProvider.create(AwsBasicCredentials.create(access, secret));
	}

	@Bean
	@ConditionalOnProperty(name = "spring.cloud.aws.s3.enabled", havingValue = "true", matchIfMissing = true)
	AlmacenResumenes s3AlmacenResumenes(S3Operations s3Operations, @Value("${app.s3.bucket}") String bucket) {
		return new S3AlmacenResumenes(s3Operations, bucket);
	}

}
