package com.mediroute.repository.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.jpa.domain.Specification;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    static <T> Specification<T> orgScoped(Long orgId) {
        return (root, query, cb) -> orgId == null ? cb.conjunction() : cb.equal(root.get("orgId"), orgId);
    }
}