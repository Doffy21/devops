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

### Exécution d’un profil InSpec sur un conteneur Docker

Pour exécuter les audits de sécurité InSpec sur les conteneurs (comme `backend`), il faut connaitre le nom du conteneur et qu'il soit lancé.

```bash
docker run --rm -it \
  -v "$PWD":/share \
  -v /var/run/docker.sock:/var/run/docker.sock \
  chef/inspec \
  exec /share/container-security -t docker://backend
```

Remplace `backend` par le nom réel du conteneur Docker que tu veux auditer (visible avec `docker ps`)



## La gestion des secrets avec HashiCorp Vault

Pour sécuriser les secrets et les variables sensibles, [HashiCorp Vault](https://www.vaultproject.io/) a été intégré dans l’infrastructure. Vault permet de centraliser et de gérer les secrets de manière sécurisée, tout en limitant leur exposition dans les fichiers de configuration.

---

### Intégration de Vault

- Un conteneur Vault a été ajouté à l’architecture Docker Compose.
- Les secrets sensibles, tels que les identifiants de base de données (`DB_USER`, `DB_PASS`) et d'autres clés sensibles, sont destinés à être gérés via Vault.
- Actuellement, seule la clé `API_KEY` a été stockée dans Vault pour des tests, en utilisant la commande suivante :

```bash
docker exec -e VAULT_ADDR=http://127.0.0.1:8200 \
  -e VAULT_SKIP_VERIFY=true \
  -e VAULT_TOKEN=root \
  -it vault \
  vault kv put secret/api API_KEY=your_api_key
```

- Les services peuvent accéder aux secrets via l’API de Vault, en utilisant des tokens d’authentification.

---

### Limites actuelles

- **Fichier de secret `API_KEY` non supprimé :** Bien que la clé `API_KEY` ait été stockée dans Vault, le fichier contenant cette clé n’a pas encore été supprimé. Cela nécessite une adaptation complète du code pour utiliser exclusivement Vault.
- **Secrets volatils :** Les secrets stockés dans Vault disparaissent lorsque les conteneurs sont redémarrés. Cela est dû à l’utilisation d’un backend de stockage en mémoire (non persistant). Une solution envisagée est de configurer un backend persistant, comme un stockage de fichiers ou une base de données.
- **Utilisation limitée :** Pour l’instant, seul le secret `API_KEY` est géré dans Vault. L’objectif est d’étendre cette gestion à tous les secrets sensibles de l’infrastructure.

---

### Commandes utiles pour Vault

Pour initialiser et déverrouiller Vault après le démarrage du conteneur :

```bash
# Initialiser Vault
vault operator init

# Déverrouiller Vault
vault operator unseal
```

Pour ajouter un secret dans Vault :

```bash
vault kv put secret/myapp API_KEY=your_api_key
```

Pour récupérer un secret depuis Vault :

```bash
vault kv get secret/myapp
```

---

### Améliorations futures

- Supprimer complètement les fichiers contenant des secrets sensibles, comme `API_KEY`, et migrer leur gestion vers Vault.
- Configurer un backend persistant pour Vault afin de conserver les secrets même après un redémarrage des conteneurs.
- Étendre l’utilisation de Vault pour gérer tous les secrets sensibles de l’infrastructure, tels que les identifiants de base de données et autres clés critiques.
- Automatiser l’initialisation et le déverrouillage de Vault pour simplifier son utilisation dans l’environnement Docker.
