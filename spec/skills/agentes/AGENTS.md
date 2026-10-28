# Repository Guidelines

## Project Structure & Modules

- `backend/` is the Spring Boot API (Java 17); features live under `src/main/java/com/tallermecanico/` (`auth`, `role`, `security`, `user`, `workshop`, `vehicle`).
- `frontend/` is the React/Vite application; pages and API helpers are in `src/`, with static assets in `public/`.
- `database/` contains MySQL initialization scripts. `docker-compose.yml` runs MySQL, the API, and the web app; database files persist in the `mysql_data` volume.
- `documentacion/` contains Markdown guides. Keep Archify workflow sources and generated HTML in `diagramas/archify/`; the renderer is under `archify/`.

## Build, Test & Development

- `docker compose up --build -d` builds and starts the complete stack. Open the UI at `http://localhost:5173`.
- `docker compose ps` checks service health; `docker compose logs -f backend` follows API logs.
- `cd backend; mvn test` runs backend tests; `mvn package` builds the API jar. There are currently no backend test sources.
- `cd frontend; npm install; npm run dev` starts Vite. `npm run build` checks and builds the production bundle; no frontend test script is currently defined.
- From the repository root, `npm run diagrams:build` regenerates and validates Archify diagrams.

## Style & Naming

Use four spaces for Java and two spaces for JavaScript/JSX. Keep packages and directories lowercase. Follow the existing API field naming (`snake_case`) and React component/file conventions in the module being changed; avoid broad renames. Validate request DTOs at the API boundary and keep business rules in services. Add concise Javadoc to Java methods and JSDoc to exported or non-obvious frontend functions. Store secrets and local credentials in `.env`, never in source or documentation.

## Testing Expectations

Run the relevant build after changes: `mvn test` for backend work, `npm run build` for frontend work, and `npm run diagrams:build` for diagram edits. For cross-layer changes, use Docker Compose and check the affected API and browser flow. Do not add sample records to the persistent database as part of verification.

## Commits & Pull Requests

This checkout has no Git history to infer a commit convention. Use short imperative Conventional Commit messages, for example `feat(clients): add paginated directory`. Pull requests should explain the behavior change, list verification commands and results, note configuration requirements, and include screenshots for visible UI changes. Link related issues when available.

## Configuration & Security

Copy `.env.example` to `.env` for local setup and configure database/JWT secrets and `POSTALKIT_API_KEY` as needed. Keep the MySQL volume when restarting services; avoid destructive volume removal. Password reset currently exposes its development link in backend logs, so configure an email provider before production use.
