package io.k48.fortyeightid.audit;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:eventType IS NULL OR a.action = :eventType)
              AND (:userId IS NULL OR a.userId = :userId)
              AND (CAST(:from AS timestamp) IS NULL OR a.createdAt >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findWithFilters(
            @Param("eventType") String eventType,
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.userId = :userId
              AND a.action LIKE 'LOGIN%'
              AND (CAST(:from AS timestamp) IS NULL OR a.createdAt >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findLoginHistory(
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
            SELECT COUNT(DISTINCT a.userId) FROM AuditLog a
            WHERE a.action LIKE 'LOGIN%'
              AND a.createdAt >= :since
            """)
    long countActiveSessionsSince(@Param("since") Instant since);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.action LIKE 'LOGIN%'
              AND a.createdAt >= :since
            ORDER BY a.createdAt DESC
            """)
    java.util.List<AuditLog> findLoginActivitySince(@Param("since") Instant since);

    @Query("""
            SELECT a FROM AuditLog a
            ORDER BY a.createdAt DESC
            """)
    java.util.List<AuditLog> findRecentActivity(Pageable pageable);

    // ── Traffic queries ───────────────────────────────────────────────────────

    @Query(value = """
            SELECT * FROM audit_log
            WHERE action = 'API_KEY_USED'
              AND details->>'keyId' = :keyId
            ORDER BY created_at DESC
            """, nativeQuery = true)
    java.util.List<AuditLog> findApiKeyUsageByKeyId(@Param("keyId") String keyId);

    @Query(value = """
            SELECT * FROM audit_log
            WHERE action = 'API_KEY_USED'
              AND details->>'keyId' = :keyId
            ORDER BY created_at DESC
            """, nativeQuery = true)
    Page<AuditLog> findApiKeyUsageByKeyIdPaged(@Param("keyId") String keyId, Pageable pageable);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.action = 'OPERATOR_ACTION'
              AND a.userId IN :userIds
            ORDER BY a.createdAt DESC
            """)
    java.util.List<AuditLog> findOperatorActionsByUserIds(@Param("userIds") java.util.Collection<UUID> userIds);

    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.action = 'OPERATOR_ACTION'
              AND a.userId IN :userIds
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLog> findOperatorActionsByUserIdsPaged(
            @Param("userIds") java.util.Collection<UUID> userIds, Pageable pageable);
}
