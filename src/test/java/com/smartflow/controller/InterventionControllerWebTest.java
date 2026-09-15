package com.smartflow.controller;

import com.smartflow.dto.InterventionDtos;
import com.smartflow.entity.Priority;
import com.smartflow.entity.Status;
import com.smartflow.exception.BusinessException;
import com.smartflow.exception.ResourceNotFoundException;
import com.smartflow.security.JwtService;
import com.smartflow.security.UserDetailsServiceImpl;
import com.smartflow.service.InterventionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Test
    void createWithValidBodyReturnsCreated() throws Exception {
        InterventionDtos.InterventionResponse response = new InterventionDtos.InterventionResponse(
                9L, "Panne réseau", "La connexion est coupée", Status.NOUVELLE, Priority.HIGH,
                2L, "Réseau", 3L, "Jean Dupont", null, null,
                "Paris", LocalDateTime.now(), null, 60, null, null, null);
        when(interventionService.create(any(InterventionDtos.CreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/interventions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId":3,"title":"Panne réseau","description":"La connexion est coupée",
                                 "categoryId":2,"priority":"HIGH","location":"Paris","estimatedTimeMinutes":60}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.status").value("NOUVELLE"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    void notFoundErrorsAreMappedToJson() throws Exception {
        when(interventionService.get(404L))
                .thenThrow(new ResourceNotFoundException("Intervention", 404L));

        mockMvc.perform(get("/api/interventions/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("introuvable")));
    }

    @Test
    void businessConflictsAreMappedToJson() throws Exception {
        when(interventionService.assign(anyLong(), any(InterventionDtos.AssignRequest.class)))
                .thenThrow(new BusinessException(HttpStatus.CONFLICT, "Statut incompatible"));

        mockMvc.perform(put("/api/interventions/5/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"technicianId\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Statut incompatible"));
    }
}