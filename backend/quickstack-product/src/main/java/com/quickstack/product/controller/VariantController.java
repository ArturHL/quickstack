package com.quickstack.product.controller;

import com.quickstack.common.security.JwtAuthenticationPrincipal;
import com.quickstack.product.dto.request.VariantCreateRequest;
import com.quickstack.product.dto.request.VariantUpdateRequest;
import com.quickstack.product.dto.response.VariantResponse;
import com.quickstack.product.service.VariantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/variants")
public class VariantController {

    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @GetMapping
    public ResponseEntity<List<VariantResponse>> listVariants(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable UUID productId) {

        return ResponseEntity.ok(variantService.listVariants(principal.tenantId(), productId));
    }

    @PostMapping
    @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
    public ResponseEntity<VariantResponse> addVariant(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody VariantCreateRequest request) {

        VariantResponse response = variantService.addVariant(principal.tenantId(), principal.userId(), productId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
    public ResponseEntity<VariantResponse> updateVariant(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody VariantUpdateRequest request) {

        return ResponseEntity.ok(variantService.updateVariant(principal.tenantId(), principal.userId(), productId, variantId, request));
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("@catalogPermissionEvaluator.canManageCatalog(authentication)")
    public ResponseEntity<Void> deleteVariant(
            @AuthenticationPrincipal JwtAuthenticationPrincipal principal,
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {

        variantService.deleteVariant(principal.tenantId(), principal.userId(), productId, variantId);
        return ResponseEntity.noContent().build();
    }
}
