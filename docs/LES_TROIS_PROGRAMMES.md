# Mixeo — Les trois programmes du pipeline

> Document de présentation pour le projet académique.
> Le sujet demande de réaliser **trois programmes** qui communiquent entre eux.
> Dans Mixeo, ces trois programmes forment une **chaîne de traitement de fichiers MP3** :
> du scan d'un dossier jusqu'à l'enregistrement en base de données.

---

## 1. Vue d'ensemble

Les trois programmes ne se parlent pas directement : ils communiquent **de façon asynchrone**
via un **broker de messages RabbitMQ** (modèle producteur / consommateur). Chaque programme
lit dans une file (*queue*), fait son travail, puis écrit le résultat dans la file suivante.

```
┌────────────┐   mp3.files    ┌────────────┐  mp3.metadata  ┌────────────┐      HTTP POST     ┌─────────┐
│ PROGRAMME 1│ ─────────────▶ │ PROGRAMME 2│ ─────────────▶ │ PROGRAMME 3│ ─────────────────▶ │   API   │
│   (scan)   │   (chemins)    │ (métadata) │  (métadonnées) │  (upload)  │  /api/mp3/upload   │  + BDD  │
└────────────┘                └────────────┘                └────────────┘                    └─────────┘
       │                             │                              │
       └──── publie les chemins      └──── extrait titre,           └──── envoie le fichier à
             des fichiers .mp3             artiste, album, etc.            l'API puis SUPPRIME
                                                                          l'original du disque
```

Les deux files RabbitMQ utilisées (définies dans
[RabbitConfig.java](../Mixeo.JavaSuite/common/src/main/java/com/mixeo/common/RabbitConfig.java)) :

| File (queue)   | Contenu transmis                    | Producteur  | Consommateur |
|----------------|-------------------------------------|-------------|--------------|
| `mp3.files`    | Chemin absolu d'un fichier `.mp3`   | Programme 1 | Programme 2  |
| `mp3.metadata` | Métadonnées du MP3 (format JSON)    | Programme 2 | Programme 3  |

> ⚙️ **Pourquoi RabbitMQ ?** Cela découple les programmes : chacun peut tourner
> indépendamment, à son rythme, et même planter et redémarrer sans bloquer les autres.
> C'est le cœur de l'exercice « trois programmes qui communiquent ».

---

## 2. Programme 1 — Scan du dossier et blacklist

**Rôle :** parcourir un dossier, lister les fichiers `.mp3`, écarter ceux qui sont
sur liste noire, puis **publier le chemin** de chaque fichier valide dans la file `mp3.files`.

**Où dans le code :**
- Interface + déclenchement : [MixeoDesktopApp.java](../Mixeo.JavaSuite/desktop/src/main/java/com/mixeo/desktop/MixeoDesktopApp.java) — méthode `scanNow()` (publication vers RabbitMQ).
- Logique de scan : [FolderWatcherService.java](../Mixeo.JavaSuite/desktop/src/main/java/com/mixeo/desktop/service/FolderWatcherService.java).

**Ce qu'il fait, étape par étape :**
1. L'utilisateur choisit un dossier dans l'interface graphique (JavaFX).
2. `FolderWatcherService.scanFolder()` liste tous les fichiers `.mp3` du dossier.
3. Pour chaque fichier, il lit les métadonnées et vérifie deux **blacklists** :
   - `blacklist/bl_artist.csv` (artistes interdits)
   - `blacklist/bl_genre.csv` (genres interdits)
4. Si l'artiste ou le genre est blacklisté → le fichier est **supprimé du disque**.
5. Sinon → le chemin du fichier est **publié dans la file `mp3.files`**.
6. Un scan automatique se relance **toutes les 60 secondes**.

**Technologies :** Java, JavaFX (interface), RabbitMQ (publication), jaudiotagger (lecture des tags).

---

## 3. Programme 2 — Extraction des métadonnées

**Rôle :** écouter la file `mp3.files`, et pour chaque chemin reçu, **extraire les métadonnées**
du MP3 (titre, artiste, album, genre, année, durée), puis les **publier au format JSON**
dans la file `mp3.metadata`.

**Où dans le code :**
- Worker : [MixeoDesktopApp.java](../Mixeo.JavaSuite/desktop/src/main/java/com/mixeo/desktop/MixeoDesktopApp.java) — méthode `startProgram2()`.
- Extraction des tags : [MetadataService.java](../Mixeo.JavaSuite/desktop/src/main/java/com/mixeo/desktop/service/MetadataService.java).

> ℹ️ **À savoir pour la soutenance :** dans cette version, le Programme 2 est démarré
> *à l'intérieur* de la même application que le Programme 1 (méthode `startProgram2()`
> appelée au lancement de l'interface). Ce sont deux **rôles logiques distincts** —
> un producteur et un consommateur séparés par RabbitMQ — même s'ils s'exécutent dans
> le même processus. On peut tout à fait les présenter comme deux programmes indépendants.

**Ce qu'il fait, étape par étape :**
1. Se connecte à RabbitMQ et écoute la file `mp3.files`.
2. À la réception d'un chemin, vérifie que le fichier existe encore.
3. Extrait les métadonnées via `MetadataService.extract()` (bibliothèque jaudiotagger).
4. Publie l'objet métadonnées (JSON) dans la file `mp3.metadata`.
5. Confirme le traitement à RabbitMQ (`ACK`) — ou le rejette (`NACK`) en cas d'erreur.

**Technologies :** Java, RabbitMQ (consommation + publication), jaudiotagger.

---

## 4. Programme 3 — Upload vers l'API

**Rôle :** écouter la file `mp3.metadata`, et pour chaque message reçu, **envoyer le fichier MP3
et ses métadonnées à l'API REST**, puis **supprimer le fichier original** du disque une fois l'upload réussi.

**Où dans le code :**
- Point d'entrée : [Main.java](../Mixeo.JavaSuite/uploader/src/main/java/com/mixeo/uploader/Main.java).
- Logique : [UploaderService.java](../Mixeo.JavaSuite/uploader/src/main/java/com/mixeo/uploader/UploaderService.java)
  (constante `PROGRAM_NAME = "program3"`).

**Ce qu'il fait, étape par étape :**
1. Se connecte à RabbitMQ et écoute la file `mp3.metadata`.
2. À la réception, désérialise le JSON en objet `Mp3Metadata`.
3. Construit une requête `multipart/form-data` (champs texte + fichier binaire).
4. Envoie un `POST` à `http://localhost:5021/api/mp3/upload`.
5. Si la réponse est un succès (HTTP 2xx) → **supprime le fichier MP3 d'origine** du disque.
6. Confirme le traitement à RabbitMQ (`ACK`).

> ⚠️ **Attention en démo :** le Programme 3 **supprime le fichier original** après un upload réussi.
> Toujours garder une copie des MP3 de test (voir le dossier `test-mp3/`).

**Technologies :** Java, RabbitMQ (consommation), `java.net.http.HttpClient` (appel API), Jackson (JSON).

---

## 5. Comment lancer les trois programmes

Prérequis : RabbitMQ démarré (Docker), PostgreSQL démarré, et l'API REST lancée.

```powershell
# API REST (nécessaire pour le Programme 3)
cd C:\Users\bruel\Documents\Mixeo\Mixeo.ApiJava
mvn spring-boot:run                       # http://localhost:5021

# Programmes 1 + 2 — l'application desktop JavaFX
cd C:\Users\bruel\Documents\Mixeo\Mixeo.JavaSuite
mvn -pl desktop javafx:run

# Programme 3 — le worker uploader (autre terminal)
cd C:\Users\bruel\Documents\Mixeo\Mixeo.JavaSuite
mvn -pl uploader exec:java
```

---

## 6. Vérifier que la chaîne fonctionne (logs)

Les trois programmes écrivent dans un journal partagé : [logs/mixeo-app.log](../logs/mixeo-app.log),
en s'identifiant par leur nom (`program1`, `program2`, `program3`). On peut donc suivre
le parcours d'un fichier d'un bout à l'autre :

| Étape attendue dans le log         | Programme   |
|------------------------------------|-------------|
| `Started folder scan...`           | program1    |
| `[RABBITMQ] Requesting publish...` | program1    |
| `Extracted metadata for...`        | program2    |
| `[RABBITMQ] Publish metadata...`   | program2    |
| `[RABBITMQ] Consume metadata...`   | program3    |
| `✅ Upload success...`             | program3    |
| `🗑 Deleted original file...`      | program3    |

---

## 7. Résumé en une phrase

> **Programme 1** trouve les MP3 et filtre les indésirables →
> **Programme 2** lit leurs métadonnées →
> **Programme 3** les envoie à l'API et nettoie le disque.
> Le tout relié par **RabbitMQ**, ce qui rend les trois programmes indépendants et communicants.
