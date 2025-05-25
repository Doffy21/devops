# Pull request DevOps & Security
Réalisé par : **OUATTARA Daouda**, **GUEI Floraine**, **KONE Séfihait**, **DURAND Corentin**

Ce projet a pour objectif de mettre en pratique les principes DevOps et sécurité autour d'une architecture web composée d’un frontend Angular et d’un backend Quarkus, interconnectés à travers une API, et orchestrés à l’aide de Docker Compose. 

Dans une logique de sécurité et de bonnes pratiques DevSecOps, l’ensemble de l’infrastructure a été renforcé par :

- **La conteneurisation complète** des services (Angular, Quarkus, MySQL, Etherpad, SMTP) via Docker
- **L’analyse de vulnérabilités** sur les images Docker avec [Trivy](https://github.com/aquasecurity/trivy)
- **L’audit de sécurité des conteneurs** avec [InSpec](https://www.inspec.io/)
- **La gestion des secrets et des variables sensibles** avec [HashiCorp Vault](https://www.vaultproject.io/) (envisagée ou intégrée selon l'étape du projet)

## La conteneurisation complète


L'ensemble de l'architecture a été entièrement conteneurisé à l’aide de **Docker** et orchestré via **Docker Compose**, afin de garantir la portabilité, la cohérence des environnements, et une facilité de déploiement en local ou sur serveur.

### Services conteneurisés

| Service     | Description                             | Image utilisée                      | Port exposé |
|-------------|-----------------------------------------|-------------------------------------|-------------|
| **frontend** | Application Angular (mode dev)          | `node:16`                           | `4200`      |
| **backend**  | Application Quarkus (mode dev)          | `maven:3.9-eclipse-temurin-17`      | `8080`      |
| **database** | Base de données relationnelle MySQL     | `mysql:8`                           | Interne     |
| **etherpad** | Éditeur collaboratif de texte           | `etherpad/etherpad`                 | `9001`      |
| **smtp**     | Serveur SMTP de test (mail local)       | `bytemark/smtp`                     | Interne     |

---

### Réseau et isolation

Tous les services sont interconnectés au sein d’un **réseau Docker privé** nommé `backend` :
- Ce réseau isole les services des appels extérieurs non désirés.
- Seuls les ports utiles (4200 pour Angular, 8080 pour Quarkus) sont exposés à l’extérieur.
- MySQL, SMTP et Etherpad sont accessibles uniquement **par les autres services**, pas depuis l'extérieur.
- **Important :** dans le code (Angular et Quarkus), les appels à localhost ont été remplacés par les noms des services Docker (ex : http://backend:8080, http://etherpad:9001), car localhost dans un conteneur ne pointe pas vers les autres services, mais vers lui-même.

---

### Configuration des services

- Le **frontend Angular** est lancé via `ng serve` dans le conteneur, avec `--host 0.0.0.0` pour autoriser l’accès depuis l’extérieur.
- Le **backend Quarkus** est exécuté en mode `dev` via `./mvnw quarkus:dev`, avec `quarkus.http.host=0.0.0.0` configuré dans `application.properties`. Un `Dockerfile.dev` a été créé dans le dossier `/api` pour permettre l'exécution de Quarkus en mode développement dans un conteneur. Il utilise l’image Maven avec JDK 17.
- Les **fichiers `.env`** permettent de centraliser les variables sensibles (par ex. `DB_USER`, `DB_PASS`, `MYSQL_ROOT_PASSWORD`).

---

### Docker Compose

Tous les services sont définis dans un fichier `docker-compose.yaml` (situé dans le dossier `/api`) et peuvent être lancés simultanément avec :

```bash
docker compose up --build
```

## L’analyse de vulnérabilités

Afin de renforcer la sécurité de l'infrastructure conteneurisée, l'outil [Trivy](https://github.com/aquasecurity/trivy) a été utilisé pour détecter les vulnérabilités connues (CVE) dans :

- les **images Docker utilisées** (OS + librairies)
- les **fichiers de configuration** comme les `Dockerfile`
- les **dépendances applicatives** (Node.js pour Angular, Java/Maven pour Quarkus)

---

### Scan d'images Docker

Trivy permet de scanner les images locales créées par Docker Compose :

```bash
# Exemple : scanner l'image backend (nom réel selon docker images)
trivy image backend
```
Il retourne un rapport clair classant les vulnérabilités par sévérité (LOW, MEDIUM, HIGH, CRITICAL).

## L’audit de sécurité des conteneurs

Dans une logique DevSecOps, l’outil [InSpec](https://www.inspec.io/) a été utilisé pour auditer les conteneurs Docker au niveau :
- des utilisateurs (droits root, utilisateurs non privilégiés)
- des services exposés
- des ports écoutés
- des fichiers critiques (droits, existence)

InSpec permet de définir des **tests de sécurité sous forme de code**, et de les exécuter régulièrement sur les conteneurs.

---

### Création du profil InSpec

Un profil personnalisé a été créé à l’aide de la commande suivante (via Docker) :

```bash
docker run --rm -it \
  -v "$PWD":/share \
  chef/inspec \
  init profile /share/container-security
```