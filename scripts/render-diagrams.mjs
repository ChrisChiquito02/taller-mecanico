import { readdirSync } from 'node:fs'; // Descubre las fuentes de workflow mantenidas por el proyecto.
import { join, resolve } from 'node:path'; // Construye rutas portables desde cualquier terminal.
import { spawnSync } from 'node:child_process'; // Ejecuta las herramientas oficiales de Archify.

const root = resolve(import.meta.dirname, '..'); // Resuelve la carpeta raíz del repositorio.
const directory = join(root, 'diagramas', 'archify'); // Usa el directorio exclusivo de diagramas.
const renderer = join(root, 'archify', 'archify', 'renderers', 'workflow', 'render-workflow.mjs'); // Localiza el renderer integrado.
const checker = join(root, 'archify', 'archify', 'scripts', 'check-render-output.mjs'); // Localiza el validador del HTML.
const sources = readdirSync(directory).filter(name => name.endsWith('.workflow.json')); // Selecciona solo fuentes workflow.

if (sources.length === 0) { // Comprueba que exista trabajo que ejecutar.
  console.error('No se encontraron fuentes workflow en diagramas/archify.'); // Informa cómo resolver la carpeta vacía.
  process.exit(1); // Devuelve error a npm.
} // Termina la comprobación de fuentes.

let failed = false; // Guarda el estado agregado de todos los diagramas.
for (const name of sources) { // Procesa cada fuente de forma independiente.
  const source = join(directory, name); // Resuelve la fuente actual.
  const output = join(directory, name.replace('.json', '.html')); // Coloca el artefacto en la carpeta dedicada.
  const render = spawnSync(process.execPath, [renderer, source, output], { cwd: root, encoding: 'utf8' }); // Genera el HTML interactivo.
  if (render.status !== 0) { // Revisa errores de esquema o composición.
    failed = true; // Marca fallida la ejecución conjunta.
    console.error(`ERROR al renderizar ${name}:\n${render.stderr || render.stdout}`); // Presenta el diagnóstico del renderer.
    continue; // Continúa para reportar los demás flujos.
  } // Termina el manejo de errores del renderer.
  const validation = spawnSync(process.execPath, [checker, output], { cwd: root, encoding: 'utf8' }); // Valida el HTML y su SVG.
  if (validation.status !== 0) { // Revisa errores del comprobador.
    failed = true; // Marca fallido el resultado global.
    console.error(`ERROR al validar ${name}:\n${validation.stderr || validation.stdout}`); // Presenta el diagnóstico del comprobador.
    continue; // Continúa con los otros diagramas.
  } // Termina el manejo de errores del comprobador.
  const report = JSON.parse(validation.stdout); // Interpreta el resumen de validación.
  const summary = report.composition?.summary ?? {}; // Lee métricas opcionales sin asumir versión concreta.
  console.log(`OK ${name}: HTML validado; ${summary.errors ?? 0} errores, ${summary.warnings ?? 0} advertencias.`); // Resume el estado del artefacto.
  if (report.composition?.status && report.composition.status !== 'pass') failed = true; // Falla si la composición no pasa.
} // Termina el recorrido de fuentes.

process.exitCode = failed ? 1 : 0; // Devuelve el resultado conjunto al shell.
