package com.mediroute.repository;

import com.mediroute.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    boolean existsByEmail(String email);
    List<Driver> findByActiveTrue();
    Optional<Driver> findByNameAndPhone(String name, String phone);
    boolean existsByNameAndPhone(String name, String phone);
}