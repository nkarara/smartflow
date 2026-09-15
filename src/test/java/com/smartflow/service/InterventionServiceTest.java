package com.smartflow.service;

import com.smartflow.dto.InterventionDtos;
import com.smartflow.entity.Category;
import com.smartflow.entity.Client;
import com.smartflow.entity.Intervention;
import com.smartflow.entity.Priority;
import com.smartflow.entity.Role;
import com.smartflow.entity.Status;
import com.smartflow.entity.Technician;
import com.smartflow.entity.User;
import com.smartflow.exception.BusinessException;
import com.smartflow.repository.InterventionHistoryRepository;
import com.smartflow.repository.InterventionRepository;
import com.smartflow.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Création, affectation et transitions de statut des interventions.
 */
@ExtendWith(MockitoExtension.class)
class InterventionServiceTest {

    @Mock
    private InterventionRepository interventionRepository;
    @Mock
    private InterventionHistoryRepository historyRepository;
    @Mock
    private ClientService clientService;
    @Mock
    private TechnicianService technicianService;
    @Mock
    private CategoryService categoryService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InterventionService interventionService;

    private User clientUser;
    private User managerUser;
    private Client client;
    private Category category;

    @BeforeEach
    void setUp() {
        clientUser = user(1L, "client@x.fr", Role.CLIENT, "Jean", "Dupont");
        managerUser = user(2L, "manager@x.fr", Role.MANAGER, "Karim", "Manager");

        client = new Client();
        client.setId(10L);
        client.setUser(clientUser);
        client.setCompanyName("Acme");

        category = new Category();
        category.setId(20L);
        category.setName("Réseau");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(User user) {
        CustomUserDetails details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));
    }

    @Test
    void createSetsStatusNouvelleAndLogsHistory() {
        loginAs(clientUser);
        when(clientService.findClientByUserId(1L)).thenReturn(client);
        when(categoryService.find(20L)).thenReturn(category);
        when(interventionRepository.save(any(Intervention.class))).thenAnswer(invocation -> {
            Intervention saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        InterventionDtos.CreateRequest request = new InterventionDtos.CreateRequest(
                null, "Panne réseau", "Description", 20L, Priority.HIGH, "Paris", null, 60);

        InterventionDtos.InterventionResponse response = interventionService.create(request);

        assertEquals(Status.NOUVELLE, response.status());
        assertEquals("Panne réseau", response.title());
        assertEquals(Priority.HIGH, response.priority());
        verify(historyRepository).save(any(com.smartflow.entity.InterventionHistory.class));
    }

    @Test
    void assignSetsTechnicianAndStatusAssigned() {
        loginAs(managerUser);
        Intervention intervention = buildIntervention(Status.NOUVELLE, null);
        when(interventionRepository.findById(100L)).thenReturn(Optional.of(intervention));

        Technician technician = new Technician();
        technician.setId(50L);
        technician.setUser(user(3L, "tech@x.fr", Role.TECHNICIAN, "Lucas", "Moreau"));
        when(technicianService.find(50L)).thenReturn(technician);

        InterventionDtos.InterventionResponse response =
                interventionService.assign(100L, new InterventionDtos.AssignRequest(50L));

        assertEquals(Status.ASSIGNED, response.status());
        assertEquals(50L, response.technicianId());
    }

    @Test
    void cannotAssignAnInterventionAlreadyInProgress() {
        loginAs(managerUser);
        Intervention intervention = buildIntervention(Status.IN_PROGRESS, null);
        intervention.setTechnician(new Technician());
        intervention.getTechnician().setUser(user(3L, "tech@x.fr", Role.TECHNICIAN, "Lucas", "Moreau"));
        when(interventionRepository.findById(100L)).thenReturn(Optional.of(intervention));

        assertThrows(BusinessException.class,
                () -> interventionService.assign(100L, new InterventionDtos.AssignRequest(50L)));
    }

    @Test
    void invalidTransitionThrowsBusinessException() {
        loginAs(managerUser);
        Intervention intervention = buildIntervention(Status.RESOLVED, null);
        intervention.setTechnician(new Technician());
        intervention.getTechnician().setUser(user(3L, "tech@x.fr", Role.TECHNICIAN, "Lucas", "Moreau"));
        when(interventionRepository.findById(100L)).thenReturn(Optional.of(intervention));

        InterventionDtos.StatusChangeRequest request =
                new InterventionDtos.StatusChangeRequest(Status.NOUVELLE, "annulation");

        assertThrows(BusinessException.class, () -> interventionService.changeStatus(100L, request));
        assertEquals(Status.RESOLVED, intervention.getStatus());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void technicianRefusalMovesBackToNouvelle() {
        User technicianUser = user(3L, "tech@x.fr", Role.TECHNICIAN, "Lucas", "Moreau");
        loginAs(technicianUser);

        Technician technician = new Technician();
        technician.setId(50L);
        technician.setUser(technicianUser);

        Intervention intervention = buildIntervention(Status.ASSIGNED, technician);
        when(interventionRepository.findById(100L)).thenReturn(Optional.of(intervention));

        InterventionDtos.InterventionResponse response = interventionService.changeStatus(100L,
                new InterventionDtos.StatusChangeRequest(Status.NOUVELLE, "Refus faute de pièce"));

        assertEquals(Status.NOUVELLE, response.status());
        assertNull(intervention.getTechnician());
    }

    @Test
    void updateModifiesInterventionFields() {
        loginAs(managerUser);
        Intervention intervention = buildIntervention(Status.NOUVELLE, null);
        when(interventionRepository.findById(100L)).thenReturn(Optional.of(intervention));
        Category newCategory = new Category();
        newCategory.setId(21L);
        newCategory.setName("Imprimante");
        when(categoryService.find(21L)).thenReturn(newCategory);

        InterventionDtos.UpdateRequest request = new InterventionDtos.UpdateRequest(
                "Nouveau titre", "Nouvelle description", 21L, Priority.URGENT, "Lyon", null, 90);

        InterventionDtos.InterventionResponse response = interventionService.update(100L, request);

        assertEquals("Nouveau titre", intervention.getTitle());
        assertEquals(Priority.URGENT, intervention.getPriority());
        assertEquals("Imprimante", response.categoryName());
    }

    @Test
    void assignByClientIsForbidden() {
        loginAs(clientUser);

        assertThrows(BusinessException.class,
                () -> interventionService.assign(100L, new InterventionDtos.AssignRequest(50L)));
    }

    @Test
    void changeStatusForbiddenForUnassignedTechnician() {
        User technicianUser = user(3L, "tech@x.fr", Role.TECHNICIAN, "Lucas", "Moreau");
        loginAs(technicianUser);
        Intervention intervention = buildIntervention(Status.ASSIGNED, null);
        when(interventionRepository.findById(100L)).thenReturn(Optional.of(intervention));

        assertThrows(BusinessException.class, () -> interventionService.changeStatus(100L,
                new InterventionDtos.StatusChangeRequest(Status.ACCEPTED, null)));
        assertEquals(Status.ASSIGNED, intervention.getStatus());
    }

    @Test
    void viewAccessDeniedForAnotherClient() {
        loginAs(clientUser);
        User other = user(99L, "autre@x.fr", Role.CLIENT, "Marie", "Bernard");
        Client otherClient = new Client();
        otherClient.setId(99L);
        otherClient.setUser(other);
        Intervention intervention = buildIntervention(Status.NOUVELLE, null);
        intervention.setClient(otherClient);
        when(interventionRepository.findById(100L)).thenReturn(Optional.of(intervention));

        assertThrows(BusinessException.class, () -> interventionService.get(100L));
    }

    @Test
    void getThrowsNotFoundForMissingIntervention() {
        loginAs(managerUser);
        when(interventionRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(com.smartflow.exception.ResourceNotFoundException.class,
                () -> interventionService.get(100L));
    }

    @Test
    void fullHappyPathStatusFlow() {
        loginAs(managerUser);
        Intervention intervention = buildIntervention(Status.NOUVELLE, null);
        when(interventionRepository.findById(100L)).thenReturn(Optional.of(intervention));
        Technician technician = new Technician();
        technician.setId(50L);
        technician.setUser(user(3L, "tech@x.fr", Role.TECHNICIAN, "Lucas", "Moreau"));
        when(technicianService.find(50L)).thenReturn(technician);

        interventionService.assign(100L, new InterventionDtos.AssignRequest(50L));
        assertEquals(Status.ASSIGNED, intervention.getStatus());

        interventionService.changeStatus(100L, new InterventionDtos.StatusChangeRequest(Status.ACCEPTED, null));
        assertEquals(Status.ACCEPTED, intervention.getStatus());
        interventionService.changeStatus(100L, new InterventionDtos.StatusChangeRequest(Status.IN_PROGRESS, null));
        assertEquals(Status.IN_PROGRESS, intervention.getStatus());
        interventionService.changeStatus(100L, new InterventionDtos.StatusChangeRequest(Status.RESOLVED, null));
        assertEquals(Status.RESOLVED, intervention.getStatus());
        interventionService.changeStatus(100L, new InterventionDtos.StatusChangeRequest(Status.CLOSED, null));
        assertEquals(Status.CLOSED, intervention.getStatus());

        // assign (1) + 4 transitions = 5 entrées d'historique
        verify(historyRepository, org.mockito.Mockito.times(5)).save(any(com.smartflow.entity.InterventionHistory.class));
    }

    private Intervention buildIntervention(Status status, Technician technician) {
        Intervention intervention = new Intervention();
        intervention.setId(100L);
        intervention.setTitle("Panne réseau");
        intervention.setDescription("Description");
        intervention.setClient(client);
        intervention.setCategory(category);
        intervention.setPriority(Priority.MEDIUM);
        intervention.setStatus(status);
        intervention.setTechnician(technician);
        return intervention;
    }

    private User user(Long id, String email, Role role, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }
}