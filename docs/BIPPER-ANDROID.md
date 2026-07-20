# Application Android Gaulix_bipper

| | |
|:--|:--|
| **Dépôt** | [F4EED/bipper_android](https://github.com/F4EED/bipper_android) |
| **Base** | Fork [meshtastic/Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android) (KMP) |
| **Nom affiché** | **Gaulix_bipper** |
| **Firmware cible** | Gaulix Bipper **v1.10.0+** |
| **Écosystème** | Voir firmware `docs/ECOSYSTEME-GAULIX.md` |

---

## Rôle

Application coordinateur / companion Meshtastic étendue pour le réseau **Gaulix** :

- envoyer des alertes pager (`#alerte`, `#secours`, `#vigilance`, `#info`, `#fin`) ;
- paramétrer un Bipper connecté (tags T1–T4, code, status, bips) ;
- accès rapide à l’envoi d’alerte depuis **n’importe quel onglet**.

---

## Accès envoi d’alerte (partout)

| Entrée | Emplacement |
|:-------|:------------|
| **Logo Gaulix** | Barre de navigation du bas, juste après **Messages** |
| **Logo Gaulix** | Coin haut gauche (écrans racine sans bouton retour) |
| **Paramètres** | Section **Envoi alerte** / **Paramétrer le Bipper** |
| Deep link | `…/bipper-alerts` → `SettingsRoute.BipperAlertSend` |

Implémentation shell : `MeshtasticNavigationSuite` + `LocalOpenBipperAlertSend`.

---

## Écrans Bipper

| Route | Écran | Module |
|:------|:------|:-------|
| `SettingsRoute.BipperAlertSend` | Envoi alerte Gaulix | `feature/settings/.../bipper/BipperAlertSendScreen.kt` |
| `SettingsRoute.BipperConfig` | Paramétrer le Bipper | `.../BipperConfigScreen.kt` |

Fonctions typiques de l’envoi :

- type : Alerte / Secours / Vigilance / Info / Fin ;
- texte + appartenance (`#…`) ;
- destination : canal nommé **Alerte** ou DM vers un nœud.

---

## Protocole (aligné web + firmware)

Code partagé KMP : `core/domain/.../bipper/`

| Fichier | Rôle |
|:--------|:-----|
| `AlertCommands.kt` | `formatPagerAlertCommand` / `parsePagerAlertCommand` |
| `ServiceTags.kt` | Tags T1–T4 |
| `PagerStatus.kt` | Parse réponses `#status` |

Format filaire :

```text
#alerte|#secours|#vigilance|#info <texte> [#appartenance]
#fin [#appartenance]
```

Miroir TypeScript côté web : `apps/web/src/lib/bipper/alertCommands.ts`.

---

## Build APK (debug fdroid)

Prérequis : **JDK 25**, Android SDK, `local.properties` (voir `AGENTS.md` / `.skills/project-overview`).

```bat
set JAVA_HOME=...
set ANDROID_HOME=C:\Android\Sdk
set VERSION_CODE=99999
set VERSION_NAME=2.7.0-local
gradlew.bat :androidApp:assembleFdroidDebug --no-daemon --no-configuration-cache
```

APK :

```text
androidApp/build/outputs/apk/fdroid/debug/androidApp-fdroid-universal-debug.apk
```

Espace disque recommandé : **≥ 15 Go** libres pour un build propre.

---

## Rebranding

| Élément | Valeur |
|:--------|:-------|
| Nom app | Gaulix_bipper (`meshtastic_app_name` / `app_name`) |
| Logo | `core/resources/.../gaulix_rond.png` |
| Package applicatif | hérité Meshtastic (`com.geeksville.mesh`) |

---

## Voir aussi

- Firmware : [BIPPER1.md](https://github.com/F4EED/Bipper_L1Pro/blob/master/docs/BIPPER1.md)
- Client web : [BIPPER-WEB.md](https://github.com/F4EED/client_web_MT_bipper/blob/main/docs/BIPPER-WEB.md)
- Conventions agents : [AGENTS.md](../AGENTS.md)
