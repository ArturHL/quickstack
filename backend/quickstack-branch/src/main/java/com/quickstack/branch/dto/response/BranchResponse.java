package com.quickstack.branch.dto.response;

import com.quickstack.branch.entity.Branch;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a branch, including core info and audit metadata.
 */
public record BranchResponse(
    UUID id,
    UUID tenantId,
    String name,
    String code,
    String address,
    String city,
    String phone,
    String email,
    boolean isActive,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy
) {
    /**
     * Maps a Branch entity to a BranchResponse.
     *
     * @param branch the entity to map
     * @return the response DTO
     */
    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
            branch.getId(),
            branch.getTenantId(),
            branch.getName(),
            branch.getCode(),
            branch.getAddress(),
            branch.getCity(),
            branch.getPhone(),
            branch.getEmail(),
            branch.isActive(),
            branch.getCreatedAt(),
            branch.getUpdatedAt(),
            branch.getCreatedBy(),
            branch.getUpdatedBy()
        );
    }
}
