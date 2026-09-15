package com.smartflow.controller;

import com.smartflow.dto.InterventionDtos;
import com.smartflow.entity.Priority;
import com.smartflow.entity.Status;
import com.smartflow.security.JwtService;
import com.smartflow.security.UserDetailsServiceImpl;
import com.smartflow.service.InterventionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Couche Web : réponse JSON et validation des entrées.
 */
@WebMvcTest(InterventionController.class)
@AutoConfigureMockMvc(addFilters = false)
class InterventionControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InterventionService interventionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void getReturnsInterventionAsJson() throws Exception {
        InterventionDtos.InterventionResponse response = new InterventionDtos.InterventionResponse(
                1L, "Panne réseau", "La connexion est coupée", Status.IN_PROGRESS, Priority.HIGH,
                2L, "Réseau", 3L, "Jean Dupont", 4L, "Lucas Moreau",
                "Paris", LocalDateTime.now(), null, 60, 30, "CR", null);

        when(interventionService.get(1L)).thenReturn(response);

        mockMvc.perform(get("/api/interventions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Panne réseau"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.clientName").value("Jean Dupont"));
    }

    @Test
    void createWithInvalidBodyRejected() throws Exception {
        mockMvc.perform(post("/api/interventions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"\",\"categoryId\":1,\"priority\":\"HIGH\"}"))
                .andExpect(status().isBadRequest());
    }
}