package com.civisanalytics.audit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin("*")
@RestController
public class HealthController {

	@GetMapping("/ping")
	public ResponseEntity<String> ping() {
		return ResponseEntity.ok("pong");
	}
}