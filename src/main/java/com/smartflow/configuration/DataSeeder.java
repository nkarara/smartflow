package com.smartflow.configuration;

import com.smartflow.entity.Category;
import com.smartflow.entity.Client;
import com.smartflow.entity.Intervention;
import com.smartflow.entity.InterventionHistory;
import com.smartflow.entity.Priority;
import com.smartflow.entity.Role;
import com.smartflow.entity.Skill;
import com.smartflow.entity.Status;
import com.smartflow.entity.Technician;
import com.smartflow.entity.User;
import com.smartflow.repository.CategoryRepository;
import com.smartflow.repository.ClientRepository;
import com.smartflow.repository.InterventionHistoryRepository;
import com.smartflow.repository.InterventionRepository;
import com.smartflow.repository.SkillRepository;
import com.smartflow.repository.TechnicianRepository;
import com.smartflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Crée les données de démarrage lorsque smartflow.seed.enabled=true.
 * Comptes de démo : admin@smartflow.fr/Admin@123, manager@smartflow.fr/Manager@123,
 * tech@smartflow.fr/Tech@123, sophie@smartflow.fr/Sophie@123, client@smartflow.fr/Client@123.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final TechnicianRepository technicianRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final InterventionRepository interventionRepository;
    private final InterventionHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${smartflow.seed.enabled:true}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || userRepository.count() > 0) {
            return;
        }
        log.info("Initialisation des données de démonstration SmartFlow...");

        Category materiel = seedCategory("Matériel informatique", "Dépannage du matériel (postes, écrans, périphériques)", "#ef4444", "💻");
        seedCategory("Logiciel", "Installations, mises à jour et dysfonctionnements logiciels", "#3b82f6", "🖥️");
        Category reseau = seedCategory("Réseau", "Connectivité, Wi-Fi et équipements réseau", "#8b5cf6", "🌐");
        Category imprimante = seedCategory("Imprimante", "Impression, scanners et copieurs", "#f97316", "🖨️");
        seedCategory("Électricité", "Problèmes d'alimentation et d'installation électrique", "#eab308", "⚡");
        seedCategory("Général", "Toute autre demande", "#64748b", "📋");

        Skill reseauSkill = seedSkill("Réseau");
        Skill materielSkill = seedSkill("Matériel");
        Skill logicielSkill = seedSkill("Logiciel");
        Skill impressionSkill = seedSkill("Impression");

        userRepository.save(User.builder().email("admin@smartflow.fr").password(passwordEncoder.encode("Admin@123"))
                .firstName("Nadia").lastName("Admin").role(Role.ADMIN).enabled(true).build());
        userRepository.save(User.builder().email("manager@smartflow.fr").password(passwordEncoder.encode("Manager@123"))
                .firstName("Karim").lastName("Manager").role(Role.MANAGER).enabled(true).build());

        Technician tech1 = seedTechnician("tech@smartflow.fr", "Tech@123", "Lucas", "Moreau", "Paris",
                "Génie informatique", reseauSkill, materielSkill, impressionSkill);
        Technician tech2 = seedTechnician("sophie@smartflow.fr", "Sophie@123", "Sophie", "Martin", "Lyon",
                "Systèmes et réseaux", reseauSkill, logicielSkill);

        Client client1 = seedClient("client@smartflow.fr", "Client@123", "Jean", "Dupont", "Acme SARL", "Paris");
        Client client2 = seedClient("marie@smartflow.fr", "Marie@123", "Marie", "Bernard", "Beta SAS", "Lyon");

        seedIntervention(client1, reseau, "Coupure Internet au bureau",
                "La connexion Internet ne fonctionne plus depuis ce matin, tous les postes sont touchés.",
                "Paris", Priority.HIGH, tech1, Status.IN_PROGRESS, null, tech1.getUser(),
                "Diagnostic routeur : l'interface WAN ne reçoit plus de signal. Remplacement du câble RJ45 en attente.");

        seedIntervention(client1, imprimante, "Imprimante en panne",
                "L'imprimante du bureau affiche une erreur et ne veut plus imprimer depuis hier.",
                "Paris", Priority.HIGH, tech1, Status.RESOLVED, 60, tech1.getUser(),
                "Bourrage papier retiré, impression testée avec succès, consommables vérifiés.");

        seedIntervention(client1, materiel, "Ordinateur ne démarre plus",
                "Mon ordinateur ne démarre plus depuis ce matin, écran noir au démarrage.",
                "Paris", Priority.URGENT, null, Status.NOUVELLE, null, null, null);

        seedIntervention(client2, reseau, "Problème de Wi-Fi en mesure",
                "La box Wi-Fi doit être reconfigurée après déménagement des bureaux.",
                "Lyon", Priority.MEDIUM, tech2, Status.BLOCKED, null, tech2.getUser(),
                "Attente du référencement client pour accéder à la box.");

        seedIntervention(client2, materiel, "Installation d'un poste de travail",
                "Installation et configuration d'un nouveau poste pour la comptabilité.",
                "Lyon", Priority.LOW, tech2, Status.CLOSED, 120, tech2.getUser(),
                "Poste installé, logiciels paramétrés et utilisateur formé.");

        log.info("Données de démonstration SmartFlow créées.");
    }

    private Category seedCategory(String name, String description, String color, String icon) {
        return categoryRepository.save(Category.builder().name(name).description(description).color(color).icon(icon).build());
    }

    private Skill seedSkill(String name) {
        return skillRepository.save(Skill.builder().name(name).build());
    }

    private Technician seedTechnician(String email, String password, String firstName, String lastName,
                                      String location, String specialty, Skill... skills) {
        User user = userRepository.save(User.builder().email(email).password(passwordEncoder.encode(password))
                .firstName(firstName).lastName(lastName).role(Role.TECHNICIAN).enabled(true).build());
        Technician technician = Technician.builder().user(user).location(location).specialty(specialty).available(true).build();
        technician.getSkills().addAll(List.of(skills));
        return technicianRepository.save(technician);
    }

    private Client seedClient(String email, String password, String firstName, String lastName,
                              String companyName, String city) {
        User user = userRepository.save(User.builder().email(email).password(passwordEncoder.encode(password))
                .firstName(firstName).lastName(lastName).role(Role.CLIENT).enabled(true).build());
        return clientRepository.save(Client.builder().user(user).companyName(companyName).city(city).build());
    }

    private void seedIntervention(Client client, Category category, String title, String description,
                                  String location, Priority priority, Technician technician, Status status,
                                  Integer actualTime, User changedBy, String report) {
        Intervention intervention = Intervention.builder()
                .title(title)
                .description(description)
                .client(client)
                .category(category)
                .priority(priority)
                .location(location)
                .technician(technician)
                .status(status)
                .actualTimeMinutes(actualTime)
                .report(report)
                .build();
        if (status == Status.CLOSED) {
            intervention.setClosedAt(java.time.LocalDateTime.now());
        }
        intervention = interventionRepository.save(intervention);

        historyRepository.save(InterventionHistory.builder()
                .intervention(intervention)
                .changedBy(changedBy)
                .fromStatus(Status.NOUVELLE)
                .toStatus(status)
                .comment("Création en démonstration")
                .build());
    }
}