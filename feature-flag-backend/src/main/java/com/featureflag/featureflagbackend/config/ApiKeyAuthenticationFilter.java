
package com.featureflag.featureflagbackend.config;

import com.featureflag.featureflagbackend.service.ProjectApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ProjectApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(
            ProjectApiKeyService apiKeyService
    ) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getMethod().equalsIgnoreCase("OPTIONS")
                || !request.getRequestURI().startsWith("/api/evaluate/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");

        String[] pathParts = request.getRequestURI().split("/");

        // Expected: /api/evaluate/{projectId}/{flagKey}
        if (pathParts.length != 5) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid evaluation endpoint"
            );
            return;
        }

        UUID projectId;

        try {
            projectId = UUID.fromString(pathParts[3]);
        } catch (IllegalArgumentException e) {
            response.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid project ID"
            );
            return;
        }

        if (!apiKeyService.isValidApiKey(apiKey, projectId)) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired API key"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
