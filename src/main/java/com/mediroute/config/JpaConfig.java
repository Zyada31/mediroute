package com.mediroute.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Configuration
@EnableJpaRepositories(basePackages = "com.mediroute.repository")
@EnableJpaAuditing
@EnableTransactionManagement
@FilterDef(name = "orgFilter", parameters = @ParamDef(name = "orgId", type = Long.class))
public class JpaConfig {
}