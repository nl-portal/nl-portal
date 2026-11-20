# Configuration Panel versleuteling

Het Configuration Panel levert configuratiewaarden versleuteld aan, als `{cipher}…`. NL Portal ontsleutelt ze bij het
opstarten en bij elke refresh met dezelfde sleutel als het Configuration Panel. Waarden zonder `{cipher}` worden
ongewijzigd gebruikt.

## Instellingen

| Env-variabele | Helm-waarde (`nl-portal-backend`) | Standaard | Verplicht |
|---|---|---|---|
| `CONFIGURATION_PANEL_ENCRYPTION_ENABLED` | `settings.app.features.configurationPanel.encryption.enabled` | `true` | nee |
| `CONFIGURATION_PANEL_ENCRYPTION_KEY` | `settings.app.features.configurationPanel.encryption.key` (Secret) | geen | als `CONFIGURATION_PANEL_ENABLED` en versleuteling aan staan |

- De instellingen werken alleen met `CONFIGURATION_PANEL_ENABLED=true`. Zonder Configuration Panel is geen sleutel nodig.
- De sleutel is een Base64-gecodeerde sleutel van 32 bytes, gelijk aan `CONFIG_ENCRYPTION_KEY` van het Configuration
  Panel.
- Met een bestaand Secret zet je de sleutel via key `CONFIGURATION_PANEL_ENCRYPTION_KEY`.

## Configureren

1. Genereer een sleutel: `openssl rand -base64 32`. Bewaar hem bij je databaseback-ups; zonder sleutel zijn de waarden
   niet terug te halen.
2. Zet de sleutel als `CONFIGURATION_PANEL_ENCRYPTION_KEY` in NL Portal en als `CONFIG_ENCRYPTION_KEY` in het
   Configuration Panel. Raadpleeg de documentatie van het Configuration Panel voor de versie die je gebruikt.
3. Start NL Portal en het Configuration Panel.

### Versleuteling uitzetten

1. Zet in het Configuration Panel `CONFIG_ENCRYPTION_ENABLED=false` en laat de sleutel staan. Herstart het Configuration
   Panel: het ontsleutelt alle waarden.
2. Herstart NL Portal terwijl het de sleutel nog heeft.
3. Zet in NL Portal `CONFIGURATION_PANEL_ENCRYPTION_ENABLED=false` en verwijder daarna de sleutel uit beide applicaties.

## Controleren

- NL Portal start en gebruikt de waarden uit het Configuration Panel.
- Na een wijziging in het Configuration Panel gebruikt NL Portal na de refresh de nieuwe waarde.

## Problemen oplossen

| Melding | Oplossing |
|---|---|
| `CONFIGURATION_PANEL_ENCRYPTION_KEY is required when the Configuration Panel is enabled; set CONFIGURATION_PANEL_ENCRYPTION_ENABLED=false to opt out` | Zet de sleutel, of zet versleuteling uit. |
| `CONFIGURATION_PANEL_ENCRYPTION_KEY must be Base64` | Gebruik de uitvoer van `openssl rand -base64 32` ongewijzigd. |
| `CONFIGURATION_PANEL_ENCRYPTION_KEY must be a Base64-encoded 32-byte (AES-256) key, got <n> bytes` | Genereer een nieuwe sleutel met `openssl rand -base64 32`, en zet hem in beide applicaties. |
| `Cannot decrypt Configuration Panel value: CONFIGURATION_PANEL_ENCRYPTION_KEY must equal the panel's CONFIG_ENCRYPTION_KEY` | De sleutels verschillen. Zet in beide applicaties dezelfde waarde. |
| `Cannot decrypt: key=<property>` terwijl versleuteling hier uit staat | Het Configuration Panel levert nog versleutelde waarden. Zet versleuteling weer aan, of volg de stappen bij [Versleuteling uitzetten](#versleuteling-uitzetten). |
| `encrypt.key and encrypt.key-store cannot be combined with Configuration Panel encryption; set CONFIGURATION_PANEL_ENCRYPTION_ENABLED=false to use Spring Cloud's own encryption` | Verwijder `encrypt.key` / `encrypt.key-store`, of zet versleuteling van het Configuration Panel uit. |
