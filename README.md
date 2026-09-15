# SmartFlow — Gestion des interventions techniques

Application **Full Stack** de gestion des interventions techniques : création de demandes clients,
affectation des techniciens, suivi du cycle de vie, notifications, statistiques et assistance IA.

> Java 21 · Spring Boot · Spring Security (JWT) · PostgreSQL · React · TypeScript · Tailwind CSS ·
> Docker · GitHub Actions · IA (LLM + analyseur local)

---

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Démarrage rapide avec Docker](#démarrage-rapide-avec-docker)
- [Développement local](#développement-local)
- [Comptes de démonstration](#comptes-de-démonstration)
- [API REST](#api-rest)
- [Intelligence artificielle](#intelligence-artificielle)
- [Tests](#tests)
- [CI/CD](#cicd)
- [Déploiement (Azure)](#déploiement-azure)
- [Structure du projet](#structure-du-projet)
- [Fonctionnalités bonus](#fonctionnalités-bonus)

---

## Fonctionnalités

| Domaine | Détail |
|---|---|
| **Utilisateurs & rôles** | ADMIN, MANAGER, TECHNICIAN, CLIENT — gestion des comptes, activation, permissions. |
| **Clients & techniciens** | Profils dédiés, compétences (skills), disponibilité, localisation. |
| **Interventions** | Création, cycle de vie complet, priorités, catégories, localisation, temps estimé/réel, compte rendu. |
| **Machine à états** | `NOUVELLE → ASSIGNED → ACCEPTED → IN_PROGRESS → RESOLVED → CLOSED` et `IN_PROGRESS ⇄ BLOCKED`. |
| **Affectation intelligente** | Sélection manuelle ou suggestion automatique du meilleur technicien (compétences, disponibilité, localisation, charge, priorité). |
| **IA** | Classification automatique des demandes (catégorie, priorité, temps estimé, problème probable) et génération de résumés du compte rendu. |
| **Notifications** | Nouvelle intervention, affectation, acceptation, changement de statut, commentaire, résolution, clôture. |
| **Commentaires & pièces jointes** | Échanges sur chaque intervention, photos/documents téléchargeables. |
| **Évaluation client** | Note /5 et commentaire après résolution. |
| **Tableaux de bord** | Statistiques globales, graphiques mensuels/par catégorie/priorité, performance des techniciens. |
| **Sécurité** | Spring Security + JWT (access + refresh tokens), BCrypt, validation Bean Validation, CORS, protection par rôle. |
| **Historique** | Trace complète de chaque changement de statut. |

---

## Architecture

```
┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│      React       │ ─────▶│   Spring Boot    │ ─────▶│   PostgreSQL     │
│  (Vite + Tail)   │ REST  │    (Java 21)     │  JPA  │     16 / H2*     │
└──────────────────┘       └──────────────────┘       └──────────────────┘
    :8081 (nginx)              :8080                 * H2 en mode test
```

Backend — Layered Architecture :

```
com.smartflow
├── controller      → REST API + @PreAuthorize
├── service         → logique métier (état, affectation, IA, stats)
├── repository      → Spring Data JPA
├── entity          → entités JPA (+ enums Role/Status/Priority/NotificationType)
├── dto             → records de requête/réponse (validation)
├── mapper          → conversion entité ⇄ DTO
├── security        → JwtService, filtre JWT, SecurityConfig, CORS
├── ai              → client LLM (OpenAI-compatible) + propriétés
├── exception       → gestion centralisée des erreurs
└── configuration   → DataSeeder (données de démonstration), propriétés fichiers
```

---

## Démarrage rapide avec Docker

Prérequis : [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
# 1. Lancement des trois conteneurs (PostgreSQL + backend + frontend)
docker compose up --build

# 2. Ouvrir l'application
#    Frontend : http://localhost:8081
#    API      : http://localhost:8080/api
#    Santé    : http://localhost:8080/actuator/health
```

Variables d'environnement (optionnelles via `.env`) :

| Variable | Défaut |
|---|---|
| `JWT_SECRET` | secret à changer impérativement en production (≥ 32 caractères) |
| `AI_API_KEY` | clé API du LLM (vide → analyseur local) |
| `AI_PROVIDER` | `local` ou `openai` |
| `SEED_ENABLED` | `true` → crée les données de démonstration |

---

## Développement local

### Backend (Spring Boot)

```bash
# Prérequis : JDK 21+ et Maven (ou mvnw fourni)
mvn spring-boot:run          # ou .\mvnw.cmd spring-boot:run

# PostgreSQL requis (ou utilisez docker compose up db)
# Connexion configurée via : DB_URL, DB_USERNAME, DB_PASSWORD
```

### Frontend (React + Vite)

```bash
cd frontend
npm install
npm run dev                  # http://localhost:5173 (proxy /api → :8080)
```

---

## Comptes de démonstration

Renseignés automatiquement au premier démarrage (`SEED_ENABLED=true`).

| Rôle | Email | Mot de passe |
|---|---|---|
| Administrateur | `admin@smartflow.fr` | `Admin@123` |
| Manager | `manager@smartflow.fr` | `Manager@123` |
| Technicien | `tech@smartflow.fr` | `Tech@123` |
| Technicien | `sophie@smartflow.fr` | `Sophie@123` |
| Client | `client@smartflow.fr` | `Client@123` |

<!-- PART2 -->
## API REST

### Authentification (`/api/auth`)

| Méthode | Endpoint | Rôle |
|---|---|---|
| POST | `/api/auth/register` | public (CLIENT / TECHNICIAN) |
| POST | `/api/auth/login` | public |
| POST | `/api/auth/refresh` | public (refresh token) |
| POST | `/api/auth/logout` | authentifié |
| GET | `/api/auth/me` | authentifié |

### Interventions (`/api/interventions`)

| Méthode | Endpoint | Rôle |
|---|---|---|
| GET/POST | `/api/interventions` | selon profil (client → ses demandes, technicien → ses interventions) |
| GET/PUT/DELETE | `/api/interventions/{id}` | selon permissions |
| PUT | `/api/interventions/{id}/assign` | MANAGER / ADMIN |
| GET | `/api/interventions/{id}/assign/suggestions` | MANAGER / ADMIN |
| PUT | `/api/interventions/{id}/status` | technicien (accepte/refuse/statut), MANAGER/ADMIN |
| PUT | `/api/interventions/{id}/account` | technicien / MANAGER / ADMIN |
| GET | `/api/interventions/{id}/history` | participants |
| GET/POST | `/api/interventions/{id}/comments` | participants |
| GET/POST | `/api/interventions/{id}/attachments` | participants (multipart) |
| GET | `/api/interventions/{id}/attachments/{a}/download` | participants |
| GET/POST | `/api/interventions/{id}/rating` | client (évaluation) |

### Administration

`/api/users`, `/api/clients`, `/api/technicians`, `/api/categories` — CRUD protégé (ADMIN, MANAGER en lecture).

### Statistiques & IA

`/api/dashboard/admin|manager|technician` · `/api/ai/analyze` · `/api/ai/summarize`

---

## Intelligence artificielle

Deux modes selon `AI_API_KEY` :

1. **LLM** (par défaut s'il est renseigné) — appel `POST /chat/completions` (compatible OpenAI)
   vers `AI_BASE_URL` avec le modèle `AI_MODEL`. Réponse JSON structurée.
2. **Analyseur local** — moteur par mots-clés (Imprimante, Matériel, Réseau, Logiciel, Électricité…),
   détection de priorité et estimation du temps. Fonctionne **hors ligne**, idéal pour la démo.

Exemple — « L'imprimante du bureau affiche une erreur et ne veut plus imprimer » :

```json
{
  "category": "Imprimante",
  "type": "Matériel",
  "priority": "HIGH",
  "probableProblem": "Erreur matérielle, bourrage papier ou problème de connexion de l'imprimante...",
  "estimatedTimeMinutes": 60,
  "analyzer": "Analyseur local"
}
```

---

## Tests

Technologies : **JUnit 5 · Mockito · Spring Boot Test (MockMvc)** — base H2 en mémoire.

```bash
mvn test
```

Cas couverts : authentification, règles de transition de statut, création/affectation/refus
d'intervention, contrôle des droits, validation des entrées (MockMvc), génération JWT,
suggestion automatique de technicien, analyse IA locale, chargement du contexte Spring.

---

## CI/CD

Pipeline GitHub Actions (`.github/workflows/ci.yml`) :

```
Git Push → Build Maven → Tests (JUnit/Mockito/MockMvc) → Build npm/TypeScript → Docker Build → [Deploy]
```

---

## Déploiement (Azure)

Architecture cible :

| Composant | Service Azure |
|---|---|
| React | Azure Static Web Apps ou conteneur nginx |
| Spring Boot | Azure Container Apps |
| PostgreSQL | Azure Database for PostgreSQL |
| Notifications temps réel (option) | Azure WebSocket / SignalR |

Un squelette de job de déploiement (`azure/login` + ACR + Container Apps) est fourni
(décommenter dans `ci.yml` et renseigner les secrets `AZURE_CREDENTIALS`).

---

## Structure du projet

```
├── src/                    # Backend Spring Boot (com.smartflow)
├── frontend/               # Frontend React + TypeScript + Vite
│   ├── src/api/            # Client Axios (JWT + refresh) et appels REST
│   ├── src/pages/          # Pages (login, dashboard, interventions, admin…)
│   └── src/components/     # Layout, notifications, badges, graphiques
├── docker-compose.yml      # PostgreSQL + backend + frontend
├── Dockerfile              # Image backend multi-stage
├── .github/workflows/ci.yml
└── README.md
```

---

## Fonctionnalités bonus (feuille de route)

- [x] Historique complet des modifications (`InterventionHistory`)
- [x] Monitoring Spring Boot Actuator (`/actuator/health`, `/actuator/info`)
- [ ] Swagger / OpenAPI (`springdoc-openapi`)
- [ ] WebSocket pour les notifications temps réel
- [ ] Chat Client ⇄ Technicien
- [ ] Génération de rapports PDF
- [ ] QR Code pour identifier un équipement
- [ ] SonarQube