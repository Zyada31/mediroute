package com.mediroute.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OrgFilter extends OncePerRequestFilter {

    private final EntityManagerFactory entityManagerFactory;

    public OrgFilter(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Long orgId = SecurityBeans.currentOrgId();
        if (orgId != null) {
            // Obtain the request-bound EntityManager if OpenEntityManagerInViewFilter is active
            EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
            if (em != null) {
                Session session = em.unwrap(Session.class);
                Filter filter = session.getEnabledFilter("orgFilter");
                if (filter == null) {
                    filter = session.enableFilter("orgFilter");
                }
                filter.setParameter("orgId", orgId);
            }
        }
        filterChain.doFilter(request, response);
    }
}


