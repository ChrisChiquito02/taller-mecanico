# Configuración del agente de revisión

El repositorio ejecuta Qodo PR Agent en GitHub Actions para revisar automáticamente los pull requests en español. La revisión se enfoca en seguridad, validaciones, persistencia y compatibilidad de los módulos Spring Boot, React, MySQL y Docker.

## Requisito único

La acción necesita una clave de API de OpenAI con facturación o crédito habilitado. La clave se guarda en GitHub como secreto; no se coloca en `.env`, en archivos del repositorio ni en el código.

## Configurar el secreto en GitHub

1. Abre `https://github.com/ChrisChiquito02/taller-mecanico` e inicia sesión con permisos de administración del repositorio.
2. Ve a **Settings → Secrets and variables → Actions**.
3. Pulsa **New repository secret**.
4. En **Name**, escribe exactamente `OPENAI_KEY`.
5. En **Secret**, pega la clave API creada en `https://platform.openai.com/api-keys` y guarda.

GitHub proporciona `GITHUB_TOKEN` automáticamente; no hace falta crear ese secreto manualmente. La clave de OpenAI debe tratarse como contraseña y no compartirse en issues o pull requests.

## Activar la revisión

Los archivos `.github/workflows/pr-agent.yml` y `.pr_agent.toml` ya dejan configurado el agente. Al subir estos cambios a GitHub, crear o actualizar un pull request activa la revisión. También se puede solicitar una revisión escribiendo `/review` en un comentario del pull request.

Si el workflow no aparece, verifica que el archivo esté en la rama predeterminada de GitHub y que **Settings → Actions → General** permita GitHub Actions. Los pull requests procedentes de forks no reciben secretos por defecto y por eso no tendrán acceso a `OPENAI_KEY`.

## Archivos de configuración

- `.github/workflows/pr-agent.yml`: eventos, permisos mínimos y ejecución del agente.
- `.pr_agent.toml`: idioma y criterios de revisión específicos del sistema.
