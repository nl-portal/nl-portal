# Beveiliging

Beveiligingsinstellingen van de NL Portal backend, onder `nl-portal.security.*`. Het inloggen van inwoners staat bij
[Keycloak configuratie](../keycloak.md).

| Onderdeel | Env-variabelen | Helm-waarden (`nl-portal-backend`) |
|---|---|---|
| CORS | `NLPORTAL_SECURITY_CORS_0_PATH`, `NLPORTAL_SECURITY_CORS_0_CONFIG_*` | `settings.app.security.cors.*` |
| Publieke endpoints | `NLPORTAL_SECURITY_ENDPOINTS_UNSECURED` | `settings.app.security.endpoints.unsecured` |
| Security headers | `NLPORTAL_SECURITY_HEADERS_*` | geen |
| [Actuator](actuator.md) | `NLPORTAL_SECURITY_ACTUATOR_*` | `settings.app.security.actuator.*` |
