//package com.mediroute;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//// Add this test to verify endpoints
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
//public class EndpointTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Test
//    public void testSwaggerUiIsAccessible() throws Exception {
//        mockMvc.perform((get("/swagger-ui.html")
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    public void testApiDocsIsAccessible() throws Exception {
//        mockMvc.perform(get("/api-docs"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.info.title").value("MediRoute API"));
//    }
//
//    @Test
//    public void testHealthEndpoint() throws Exception {
//        mockMvc.perform((get("/actuator/health"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value("UP"));
//    }
//}
