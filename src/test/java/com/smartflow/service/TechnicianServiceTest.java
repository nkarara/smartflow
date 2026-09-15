package com.smartflow.service;

import com.smartflow.dto.InterventionDtos;
import com.smartflow.entity.Intervention;
import com.smartflow.entity.Priority;
import com.smartflow.entity.Role;
import com.smartflow.entity.Skill;
import com.smartflow.entity.Technician;
import com.smartflow.entity.User;
import com.smartflow.repository.InterventionRepository;
import com.smartflow.repository.TechnicianRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Proposition automatique du meilleur technicien selon disponibilité, compétences,
 * localisation et charge de travail.
 */
@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;
    @Mock
    private InterventionRepository interventionRepository;

    @InjectMocks
    private TechnicianService technicianService;

    @Test
    void suggestForRanksAvailableSkilledTechnicianFirst() {
        Technician alice = technician(1L, "Alice", true, "Paris", "Réseau");
        Technician bob = technician(2L, "Bob", false, "Lyon", "Réseau", "Matériel");

        when(technicianRepository.findAll()).thenReturn(List.of(alice, bob));
        when(interventionRepository.countByTechnicianIdAndStatusIn(anyLong(), any())).thenReturn(0L);

        Intervention intervention = new Intervention();
        intervention.setTitle("Panne réseau");
        intervention.setDescription("La connexion est coupée au bureau.");
        intervention.setPriority(Priority.HIGH);
        intervention.setLocation("Paris");

        List<InterventionDtos.SuggestionItem> suggestions = technicianService.suggestFor(intervention);

        assertEquals(2, suggestions.size());
        // Alice est proposée en premier : disponible, localisation identique + compétence réseau
        assertEquals(1L, suggestions.get(0).technicianId());
        assertEquals("Alice", suggestions.get(0).technicianName().split(" ")[0]);
        assertEquals(0L, suggestions.get(0).activeInterventions());
    }

    private Technician technician(Long id, String firstName, boolean available,
                                  String location, String... skillNames) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName("Test");
        user.setRole(Role.TECHNICIAN);

        Set<Skill> skills = new java.util.HashSet<>();
        for (String name : skillNames) {
            skills.add(Skill.builder().name(name).build());
        }

        Technician technician = new Technician();
        technician.setId(id);
        technician.setUser(user);
        technician.setAvailable(available);
        technician.setLocation(location);
        technician.setSkills(skills);
        return technician;
    }
}