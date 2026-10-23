package com.eventosdami.api.web;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check típico para probes de Kubernetes / balanceadores.
 */
@RestController
public class HealthController {

	private final String serviceName;

	public HealthController(@Value("${spring.application.name:api}") String serviceName) {
		this.serviceName = serviceName;
	}

	@GetMapping("/health")
	public HealthResponse health() {
		return new HealthResponse("UP", serviceName, Instant.now());
	}

	public record HealthResponse(String status, String service, Instant timestamp) {
	}

}
