package com.quickstack.branch.service;

/**
 * Audit actions for branch management operations.
 * <p>
 * Used in structured log messages following the pattern:
 * {@code [BRANCH] ACTION=X tenantId=Y userId=Z resourceId=W resourceType=T}
 */
public enum BranchAction {
    BRANCH_CREATED,
    BRANCH_UPDATED,
    BRANCH_DELETED,
    AREA_CREATED,
    AREA_UPDATED,
    AREA_DELETED,
    TABLE_CREATED,
    TABLE_UPDATED,
    TABLE_DELETED,
    TABLE_STATUS_UPDATED
}
