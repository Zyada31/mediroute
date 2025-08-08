//package com.mediroute;
//
//import org.apache.catalina.core.ApplicationContext;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.jdbc.Sql;
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//
//@SpringBootTest
//@ActiveProfiles("test")
//public class SystemHealthCheckTest {
//
//    @Autowired
//    private ApplicationContext context;
//
//    @Test
//    public void testAllBeansCanBeCreated() {
//        // Verify all beans can be instantiated
//        String[] beanNames = context.getBeanDefinitionNames();
//        for (String beanName : beanNames) {
//            if (beanName.startsWith("com.mediroute")) {
//                assertDoesNotThrow(() -> context.getBean(beanName));
//            }
//        }
//    }
//
//    @Test
//    @Sql("/test-data/sample-data.sql")
//    public void testCriticalUserFlows() {
//        // Test 1: Upload Excel file
//        testExcelUpload();
//
//        // Test 2: Run optimization
//        testOptimization();
//
//        // Test 3: Assign drivers
//        testDriverAssignment();
//
//        // Test 4: Generate reports
//        testReportGeneration();
//    }
//
//    @Test
//    public void testDatabaseConnections() {
//        // Verify connection pool health
//        HikariDataSource ds = (HikariDataSource) dataSource;
//        assertTrue(ds.getHikariPoolMXBean().getActiveConnections() <
//                ds.getMaximumPoolSize());
//    }
//}
