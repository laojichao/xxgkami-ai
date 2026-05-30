package org.xxg.backend.backend.mapper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.xxg.backend.backend.entity.IpBlacklist;
import java.time.LocalDateTime;
import java.util.Optional;

public interface IpBlacklistRepository extends JpaRepository<IpBlacklist, Long> {
    Optional<IpBlacklist> findByIpAddress(String ipAddress);

    @Query("SELECT COUNT(i) > 0 FROM IpBlacklist i WHERE i.ipAddress = :ip " +
           "AND (i.permanent = true OR i.blockedUntil > :now)")
    boolean isBlocked(@Param("ip") String ip, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM IpBlacklist i WHERE i.permanent = false AND i.blockedUntil < :now")
    void deleteExpired(@Param("now") LocalDateTime now);
}
