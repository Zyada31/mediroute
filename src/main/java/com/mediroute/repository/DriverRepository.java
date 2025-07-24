package com.mediroute.repository;

import com.mediroute.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    boolean existsByEmail(String email);
    List<Driver> findByActiveTrue();
}