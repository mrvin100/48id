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
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
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
              AND (:from IS NULL OR a.createdAt >= :from)
              AND (:to IS NULL OR a.createdAt <= :to)
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
}
