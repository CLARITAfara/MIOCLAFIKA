# Mixeo.ApiJava

Port **Java / Spring Boot** de l'API `Mixeo.Api` (anciennement ASP.NET Core / C#).

Le frontend React (`Mixeo.Front`) n'a **aucune modification à faire** : cette API expose
exactement les mêmes routes, le même port (`5021`) et le même JSON (camelCase) que l'API C#.

## Correspondance C# → Java

| ASP.NET Core (C#)            | Spring Boot (Java)                         |
|------------------------------|--------------------------------------------|
| `Program.cs`                 | `MixeoApiApplication` + `config/WebConfig` |
| `AppDbContext` (EF Core)     | repositories `repository/*Repository`      |
| Modèles `Models/*.cs`        | entités `model/*.java` (JPA)               |
| `Controllers/*.cs`           | `controller/*.java`                        |
| `Services/LyricsService.cs`  | `service/LyricsService.java`               |
| `TagLibSharp`                | `jaudiotagger`                             |
| `ZipArchive`                 | `java.util.zip.ZipOutputStream`            |
| `enableRangeProcessing`      | `ResourceRegion` + `HttpRange` (stream)    |

## Pré-requis

- **JDK 17+** (le projet a été compilé avec Eclipse Adoptium 17).
- **Maven 3.9+**.
- **PostgreSQL** avec la base `pg4` créée via `../bd/00_script.sql`.

> ⚠️ Sous Windows, le `java` par défaut peut être un JRE 8. Forcez le JDK 17 avant de lancer Maven :
> ```powershell
> $env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot'
> $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
> ```

## Configuration

Voir `src/main/resources/application.properties` :
- Port : `5021`
- Base : `jdbc:postgresql://localhost:5432/pg4` (user/pass `postgres`/`postgres`)
- `spring.jpa.hibernate.ddl-auto=none` → le schéma reste géré par `bd/00_script.sql` (Hibernate ne le modifie pas).

## Lancer

```powershell
# Compiler
mvn clean compile

# Démarrer l'API
mvn spring-boot:run
```

L'API écoute alors sur `http://localhost:5021`. Démarrez le front (`cd ../Mixeo.Front && npm run dev`)
comme avant : il discute avec cette API Java de façon transparente.

## Endpoints (identiques à l'API C#)

**Auth** — `/api/auth`
- `POST /register` · `POST /login`

**MP3** — `/api/mp3`
- `POST /upload` (multipart) · `GET /` · `GET /{id}` · `PUT /{id}` · `DELETE /{id}` · `GET /{id}/lyrics`

**Playlists** — `/api/playlists`
- `POST /generate` · `POST /save` · `GET /?userId=` · `DELETE /{id}`
- `POST /{id}/add-track` · `POST /{id}/remove-track` · `POST /{id}/replace-track`
- `GET /stream/{mp3Id}` (avec support du Range) · `GET /{id}/download-zip`

## Notes de migration

- **Hash de mot de passe** : conservé identique au C# (Base64 du mot de passe) pour rester
  compatible avec les utilisateurs déjà en base. Ce n'est **pas** sécurisé — à remplacer par
  BCrypt (`spring-boot-starter-security`) pour un usage réel.
- **Paroles** : `LyricsService` appelle toujours le script Python `../Mixeo.Lyric/main.py`.
- Les composants `Mixeo.Uploader` et `Mixeo.Desktop` (WPF) restent en .NET ; seul le backend API
  a été porté. Le Desktop WPF est lié à Windows/.NET et n'a pas d'équivalent Java direct.
