// 10. DriverSkill Repository
package com.mediroute.repository;

import com.mediroute.entity.DriverSkill;
import com.mediroute.repository.base.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@Repository
@Tag(name = "Driver Skill Repository", description = "Driver skill data access operations")
public interface DriverSkillRepository extends BaseRepository<DriverSkill, Long> {

    // Basic Queries
    List<DriverSkill> findByDriverIdOrderBySkillName(Long driverId);

    List<DriverSkill> findBySkillNameOrderByDriverId(String skillName);

    List<DriverSkill> findByDriverIdAndSkillName(Long driverId, String skillName);

    // Certification Queries
    List<DriverSkill> findByIsCertifiedTrueOrderBySkillName();

    List<DriverSkill> findByDriverIdAndIsCertifiedTrue(Long driverId);

    @Query("SELECT ds FROM DriverSkill ds WHERE ds.driver.id = :driverId AND ds.isCertified = true " +
            "AND (ds.expiryDate IS NULL OR ds.expiryDate > :currentDate)")
    List<DriverSkill> findValidCertificationsForDriver(@Param("driverId") Long driverId,
                                                       @Param("currentDate") LocalDate currentDate);

    // Expiry Tracking
    @Query("SELECT ds FROM DriverSkill ds WHERE ds.expiryDate IS NOT NULL AND ds.expiryDate <= :expiryDate " +
            "ORDER BY ds.expiryDate ASC")
    List<DriverSkill> findExpiringCertifications(@Param("expiryDate") LocalDate expiryDate);

    @Query("SELECT ds FROM DriverSkill ds WHERE ds.expiryDate IS NOT NULL AND ds.expiryDate < :currentDate")
    List<DriverSkill> findExpiredCertifications(@Param("currentDate") LocalDate currentDate);

    // Skill Level Queries
    @Query("SELECT ds FROM DriverSkill ds WHERE ds.skillLevel >= :minLevel ORDER BY ds.skillLevel DESC")
    List<DriverSkill> findByMinSkillLevel(@Param("minLevel") Integer minLevel);

    @Query("SELECT ds FROM DriverSkill ds WHERE ds.skillName = :skillName AND ds.skillLevel >= :minLevel " +
            "ORDER BY ds.skillLevel DESC")
    List<DriverSkill> findBySkillNameAndMinLevel(@Param("skillName") String skillName,
                                                 @Param("minLevel") Integer minLevel);

    // Statistics
    @Query("SELECT DISTINCT ds.skillName FROM DriverSkill ds ORDER BY ds.skillName")
    List<String> findDistinctSkillNames();

    @Query("SELECT ds.skillName, COUNT(ds) FROM DriverSkill ds WHERE ds.isCertified = true " +
            "GROUP BY ds.skillName ORDER BY COUNT(ds) DESC")
    List<Object[]> countCertifiedDriversBySkill();

    @Query("SELECT COUNT(ds) FROM DriverSkill ds WHERE ds.driver.id = :driverId AND ds.isCertified = true")
    long countCertificationsForDriver(@Param("driverId") Long driverId);
}