package com.smeloan.platform.auth.entity;

/**
 * Roles available in the SME Loan Origination Platform.
 * <ul>
 *   <li>RM - Relationship Manager: creates and manages loan applications</li>
 *   <li>MANAGER - Approves or rejects applications after review</li>
 *   <li>ADMIN - System administrator with full access</li>
 * </ul>
 */
public enum UserRole {
    RM,
    MANAGER,
    ADMIN
}
