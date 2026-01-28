package lv.janis.iom.controller;

/**
 *
 * @author user
 */
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Health", description = "Service health endpoints")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "Health check")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    @GetMapping("/health")
    public String healthCheck() {
        return "Application is running!";
    }
}
