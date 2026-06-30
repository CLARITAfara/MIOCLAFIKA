# Mixeo — Bons à savoir (démarrage & pièges)

Mémo des points importants après la migration **C# → Java**. Tout le backend est désormais en Java (Spring Boot + JavaFX). Plus aucun composant C#.

## 1. Architecture & ports

| Composant | Dossier | Techno | Port |
|-----------|---------|--------|------|
| API REST | `Mixeo.ApiJava` | Java · Spring Boot | **5021** |
| Lib partagée | `Mixeo.JavaSuite/common` | Java | — |
| Worker upload | `Mixeo.JavaSuite/uploader` | Java (console) | — |
| GUI scan dossier | `Mixeo.JavaSuite/desktop` | Java · JavaFX | — |
| Front web | `Mixeo.Front` | React · Vite | 5173 |
| Paroles | `Mixeo.Lyric` | Python | — |
| PostgreSQL | (service Windows) | base `pg4` | **5432** |
| RabbitMQ | (Docker) | queues `mp3.files`, `mp3.metadata` | **5672** / admin **15672** |

Le flux : **GUI** scanne un dossier + blacklist → publie les chemins vers `mp3.files` → **program2** extrait les métadonnées → publie vers `mp3.metadata` → **uploader** poste vers `/api/mp3/upload` → **PostgreSQL** → **React** affiche.

## 2. Prérequis & identifiants

- **JDK 17** obligatoire (Spring Boot 3). ⚠️ Le `java` par défaut de la machine peut être un **JRE 8** → vérifier avec `java -version`. Si besoin, forcer :
  ```powershell
  $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
  $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
  ```
- **Maven 3.9+**.
- **PostgreSQL 17** : utilisateur `postgres`, mot de passe **`root`**, base `pg4`.
  - Le mot de passe est dans `Mixeo.ApiJava/src/main/resources/application.properties`.
  - `psql` n'est pas dans le PATH → chemin complet : `C:\Program Files\PostgreSQL\17\bin\psql.exe`.
- **RabbitMQ** : via Docker, user/pass `guest`/`guest`.

## 3. Démarrer chaque service

```powershell
# (1) RabbitMQ — Docker Desktop doit tourner
docker start mixeo-rabbit        # 1re fois : docker run -d --name mixeo-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# (2) PostgreSQL : service Windows "postgresql-x64-17" (démarre automatiquement)

# (3) API REST
cd C:\Users\bruel\Documents\Mixeo\Mixeo.ApiJava
mvn spring-boot:run              # http://localhost:5021

# (4) Pipeline (2 terminaux séparés, depuis Mixeo.JavaSuite)
mvn -pl uploader exec:java       # worker upload
mvn -pl desktop  javafx:run      # GUI JavaFX

# (5) Front
cd C:\Users\bruel\Documents\Mixeo\Mixeo.Front
npm run dev
```

> `mvn spring-boot:run` se lance **dans `Mixeo.ApiJava`**, PAS dans `Mixeo.JavaSuite` (sinon erreur `No plugin found for prefix 'spring-boot'`).

## 4. Pièges connus (⚠️)

1. **Mot de passe Postgres = `root`** (pas `postgres`). Un mauvais mot de passe → l'API renvoie **500 sur TOUS les endpoints** (échec de connexion DB). Diagnostic : les logs Postgres (`C:\Program Files\PostgreSQL\17\data\log\*.log`) montrent `authentification par mot de passe échouée`.

2. **Changer `application.properties` nécessite de redémarrer l'API** — Spring ne recharge pas le mot de passe à chaud.

3. **Séquences SERIAL désynchronisées** : le script `bd/00_script.sql` insère des lignes avec un `id` explicite sans avancer la séquence. Résultat : le prochain `INSERT` (ex. inscription) tente un `id` déjà pris → **erreur clé dupliquée (500)**. Corriger une fois :
   ```powershell
   $psql='C:\Program Files\PostgreSQL\17\bin\psql.exe'; $env:PGPASSWORD='root'
   & $psql -U postgres -d pg4 -c "SELECT setval('users_id_seq',     (SELECT MAX(id) FROM users));"
   & $psql -U postgres -d pg4 -c "SELECT setval('mp3_files_id_seq', (SELECT MAX(id) FROM mp3_files));"
   & $psql -U postgres -d pg4 -c "SELECT setval('playlists_id_seq', (SELECT MAX(id) FROM playlists));"
   ```

4. **Le pipeline SUPPRIME les fichiers d'origine** après un upload réussi (`File.delete` dans l'uploader). Garder une copie des MP3 de test.

5. **Hash des mots de passe = Base64** (repris à l'identique du C#). Ce n'est **pas** sécurisé — OK pour un projet d'école, à remplacer par BCrypt en production.

6. **Blacklist** : fichiers `blacklist/bl_artist.csv` et `blacklist/bl_genre.csv` (valeurs séparées par des virgules). Résolus relativement à la racine du repo.

## 5. Tester rapidement

MP3 de test déjà téléchargés dans `test-mp3/` (SoundHelix, libres).

```powershell
# Upload direct via l'API (API redémarrée requise)
$f = 'C:\Users\bruel\Documents\Mixeo\test-mp3\SoundHelix-Song-1.mp3'
$form = @{ title='Test 1'; artist='SoundHelix'; album='Demo'; genre='Electronic'; language='English'; year='2024'; useMetadata='false'; file=Get-Item $f }
Invoke-RestMethod -Uri 'http://localhost:5021/api/mp3/upload' -Method Post -Form $form

# Lister les MP3 en base
Invoke-RestMethod -Uri 'http://localhost:5021/api/mp3'
```

## 6. Endpoints de l'API

- **Auth** `/api/auth` : `POST /register`, `POST /login`
- **MP3** `/api/mp3` : `POST /upload`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`, `GET /{id}/lyrics`
- **Playlists** `/api/playlists` : `POST /generate`, `POST /save`, `GET /?userId=`, `DELETE /{id}`, `POST /{id}/add-track`, `POST /{id}/remove-track`, `POST /{id}/replace-track`, `GET /stream/{mp3Id}`, `GET /{id}/download-zip`

## 7. Logs

Log applicatif partagé du pipeline : `logs/mixeo-app.log` (à la racine du repo). Modules : `program1` (scan/blacklist), `program2` (extraction métadonnées), `program3` (upload).
