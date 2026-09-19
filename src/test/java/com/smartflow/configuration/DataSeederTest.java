package com.smartflow.configuration;

import com.smartflow.repository.AppSettingRepository;
import com.smartflow.repository.CategoryRepository;
import com.smartflow.repository.ClientRepository;
import com.smartflow.repository.InterventionRepository;
import com.smartflow.repository.TechnicianRepository;
import com.smartflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Charge le contexte applicatif avec les données de démonstration activées
 * et vérifie que le schéma JPA (H2) accueille bien le seed.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartflowseed;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "smartflow.seed.enabled=true"
})
class DataSeederTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private TechnicianRepository technicianRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private InterventionRepository interventionRepository;
    @Autowired
    private AppSettingRepository appSettingRepository;

    @Test
    void seedCreatesDemoData() {
        assertTrue(userRepository.findByEmailIgnoreCase("admin@smartflow.fr").isPresent());
        assertTrue(userRepository.findByEmailIgnoreCase("manager@smartflow.fr").isPresent());
        assertTrue(userRepository.findByEmailIgnoreCase("tech@smartflow.fr").isPresent());
        assertTrue(userRepository.findByEmailIgnoreCase("client@smartflow.fr").isPresent());

        assertEquals(2, technicianRepository.count());
        assertEquals(2, clientRepository.count());
        assertTrue(categoryRepository.findAll().size() >= 6);
        assertTrue(interventionRepository.count() >= 5);
        assertTrue(appSettingRepository.findAll().size() >= 5);
    }
}