# Mixeo
Mixeo est une application composée de plusieurs composants permettant la collecte automatique de fichiers MP3 depuis un répertoire surveillé, l’extraction de métadonnées audio, le transfert asynchrone vers une API centrale et la gestion web des musiques et playlists.

## Stack technique

Le backend a été **entièrement migré de C# (.NET) vers Java**. Plus aucun composant C#.

| Composant | Dossier | Techno |
|-----------|---------|--------|
| API REST | [Mixeo.ApiJava](Mixeo.ApiJava/) | Java · Spring Boot · JPA/Hibernate · PostgreSQL (port `5021`) |
| Pipeline (lib + worker + GUI) | [Mixeo.JavaSuite](Mixeo.JavaSuite/) | Java multi-modules Maven |
| └ `common` | [Mixeo.JavaSuite/common](Mixeo.JavaSuite/common/) | Lib partagée (RabbitMQ, métadonnées, logger) |
| └ `uploader` | [Mixeo.JavaSuite/uploader](Mixeo.JavaSuite/uploader/) | Worker console : queue `mp3.metadata` → API |
| └ `desktop` | [Mixeo.JavaSuite/desktop](Mixeo.JavaSuite/desktop/) | GUI **JavaFX** : scan dossier + blacklist + RabbitMQ |
| Frontend Web | [Mixeo.Front](Mixeo.Front/) | React · TypeScript · Vite (inchangé) |
| Paroles | [Mixeo.Lyric](Mixeo.Lyric/) | Script Python (inchangé) |

### Pré-requis
- **JDK 17+** · **Maven 3.9+** · **PostgreSQL** (base `pg4` via [bd/00_script.sql](bd/00_script.sql)) · **RabbitMQ** (pipeline).

### Lancer (résumé)
```bash
# API
cd Mixeo.ApiJava && mvn spring-boot:run

# Pipeline (build commun puis worker / GUI)
cd Mixeo.JavaSuite && mvn install
mvn -pl uploader exec:java        # worker upload
mvn -pl desktop  javafx:run       # interface graphique

# Front
cd Mixeo.Front && npm run dev
```
> ⚠️ Windows : le `java` par défaut peut être un JRE 8. Forcez le JDK 17 (`JAVA_HOME`) avant Maven.
