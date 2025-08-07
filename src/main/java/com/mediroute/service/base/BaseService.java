package com.mediroute.service.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

public interface BaseService<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    Page<T> findAll(Pageable pageable);
    Page<T> findAll(Specification<T> specification, Pageable pageable);
    void deleteById(ID id);
    boolean existsById(ID id);
    long count();
}