import { useCallback, useEffect, useState } from 'react' // Importa hooks para estado, carga reactiva y consultas memoizadas.
import { Link, Navigate } from 'react-router-dom' // Provee navegación local y redirección ante falta de sesión.
import { ArrowLeft, ArrowRight, BadgeCheck, Car, CircleHelp, Eye, Plus, Search, ShieldCheck, Trash2, Wrench, X } from 'lucide-react' // Usa iconos consistentes con el resto de Torque.
import { hasAnyRole, request, request_blob } from './api.js' // Centraliza JWT, JSON API y descarga de imágenes protegidas.

const emptyForm = { vin: '', plate: '', make: '', model: '', year: '', version: '', color: '', mileage: '', client_id: '' } // Inicializa las entradas del dominio sin datos previos.
const vinPattern = /^[A-HJ-NPR-Z0-9]{17}$/ // Exige VIN de 17 posiciones y excluye I, O y Q.
const plateRawPattern = /^[A-Za-z0-9 -]{5,10}$/ // Limita la entrada de placa a símbolos y separadores aceptados.
const plateNormalizedPattern = /^[A-HJ-NPR-Z0-9]{5,8}$/ // Revisa la forma canónica de placa mexicana soportada.
const textPattern = /^[\p{L}0-9][\p{L}0-9 .&'/-]{0,99}$/u // Acepta texto de marca, modelo y versión sin caracteres de control.
const colorPattern = /^[\p{L}][\p{L} .'-]{1,49}$/u // Restringe el color a palabras y separadores legibles.
const imageTypes = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/bmp']) // Mantiene la lista de imágenes decodificables del backend.

/** Carga una foto protegida y libera su URL temporal al cerrar o cambiar la ficha. */
function VehiclePhoto({ vehicleId, photo }) {
  const [source, setSource] = useState('') // Conserva la URL de objeto solo durante la vida del componente.
  const [error, setError] = useState(false) // Permite informar que una imagen ya no está disponible.
  useEffect(() => { // Solicita el archivo con el JWT y administra el ciclo de vida de la URL.
    let active = true // Evita actualizar React después de desmontar este componente.
    let objectUrl = '' // Guarda la URL temporal para poder liberarla al limpiar el efecto.
    request_blob(`/vehicles/${vehicleId}/photos/${photo.id}`).then(blob => { // Descarga bytes desde el endpoint autorizado.
      objectUrl = URL.createObjectURL(blob) // Crea una URL local apta para un elemento de imagen.
      if (active) setSource(objectUrl) // Publica la imagen solo si el componente sigue montado.
    }).catch(() => { if (active) setError(true) }) // Muestra un estado neutro ante errores del archivo.
    return () => { active = false; if (objectUrl) URL.revokeObjectURL(objectUrl) } // Libera memoria al salir del modal.
  }, [vehicleId, photo.id]) // Recarga cuando la ficha o la foto seleccionada cambian.
  if (error) return <span className="vehicle-photo-error">Foto no disponible</span> // Evita romper la ficha cuando no se encuentra el archivo.
  return <figure className="vehicle-photo-card"><img src={source || undefined} alt={photo.file_name} loading="lazy"/><figcaption>{photo.file_name}</figcaption></figure> // Presenta foto y nombre de archivo.
}

/** Facade que reúne catálogo, registro, consulta detallada y suspensión lógica de vehículos. */
export default function VehicleFacade() {
  const [pageData, setPageData] = useState({ content: [], number: 0, totalPages: 0, totalElements: 0 }) // Mantiene filas y metadatos de paginación del servidor.
  const [form, setForm] = useState(emptyForm) // Conserva las entradas del formulario en React.
  const [photos, setPhotos] = useState([]) // Conserva varios archivos sin codificarlos dentro del DTO JSON.
  const [clientTerm, setClientTerm] = useState('') // Conserva el texto de búsqueda de clientes activos.
  const [clientOptions, setClientOptions] = useState([]) // Muestra las coincidencias activas devueltas por la API.
  const [clientSearchMessage, setClientSearchMessage] = useState('') // Explica el resultado de la búsqueda directamente bajo el control.
  const [searchingClients, setSearchingClients] = useState(false) // Indica que el servidor está buscando clientes activos.
  const [page, setPage] = useState(0) // Guarda la página actual del listado de vehículos.
  const [direction, setDirection] = useState('DESC') // Mantiene la dirección cronológica solicitada al backend.
  const [term, setTerm] = useState('') // Conserva el texto de búsqueda visible.
  const [search, setSearch] = useState('') // Conserva el filtro aplicado en la última consulta.
  const [loading, setLoading] = useState(true) // Indica que el listado está pendiente del servidor.
  const [busy, setBusy] = useState(false) // Evita enviar dos altas simultáneas desde la vista.
  const [formOpen, setFormOpen] = useState(false) // Controla el modal de registro.
  const [dialog, setDialog] = useState(null) // Controla confirmación, error o ficha de detalle.
  const [error, setError] = useState('') // Conserva errores del listado sin ocultar la página.

  /** Consulta el servidor con el máximo de diez filas y el orden actualmente seleccionado. */
  const loadPage = useCallback(async (requestedPage = page, requestedSearch = search, requestedDirection = direction) => {
    setLoading(true) // Informa al usuario mientras cambia el resultado paginado.
    setError('') // Limpia errores de consultas anteriores.
    try { // Captura errores HTTP sin perder el resto de la pantalla.
      const result = await request(`/vehicles?page=${requestedPage}&size=10&term=${encodeURIComponent(requestedSearch)}&direction=${requestedDirection}`) // Pide una página protegida al backend.
      setPageData(result) // Guarda filas y total devueltos por Spring.
      setPage(result.number ?? requestedPage) // Sincroniza el índice si el servidor ajustó la página.
    } catch (requestError) { setError(requestError.message) } // Presenta el mensaje del servidor.
    finally { setLoading(false) } // Quita el indicador tanto en éxito como en error.
  }, [direction, page, search]) // Recrea la consulta cuando cambia alguno de sus parámetros.

  useEffect(() => { if (localStorage.getItem('taller-token')) loadPage(0, '', 'DESC') }, []) // Carga el listado inicial solo cuando hay sesión.

  /** Busca clientes activos mediante la paginación existente para evitar asociar clientes suspendidos. */
  async function findActiveClients(event) {
    event?.preventDefault() // Evita recargar la página si la búsqueda viene de un formulario.
    setSearchingClients(true) // Muestra actividad mientras el API procesa la búsqueda.
    setClientSearchMessage('') // Quita el resultado anterior para evitar confusión.
    setClientOptions([]) // Impide seleccionar coincidencias de una consulta anterior.
    try { // Consulta únicamente la primera página de coincidencias activas.
      const result = await request(`/clients?page=0&size=10&term=${encodeURIComponent(clientTerm.trim())}&direction=ASC`) // Reutiliza el buscador protegido de clientes.
      setClientOptions(result.content) // Presenta solo las filas que el endpoint excluye si están inactivas.
      setClientSearchMessage(result.content.length ? `${result.totalElements} cliente${result.totalElements === 1 ? '' : 's'} activo${result.totalElements === 1 ? '' : 's'} encontrado${result.totalElements === 1 ? '' : 's'}.` : 'No se encontraron clientes activos. Registra o reactiva un cliente antes de asociar el vehículo.') // Informa cuántos resultados activos devolvió el servidor.
    } catch (requestError) { setClientSearchMessage(requestError.message) } // Expone errores de permisos o red en el mismo formulario.
    finally { setSearchingClients(false) } // Restaura el botón después de resolver la consulta.
  }

  /** Actualiza una entrada textual y retira el error de formulario anterior. */
  function change(event) { setForm(current => ({ ...current, [event.target.name]: event.target.value })); setError('') } // Mantiene el estado del formulario sincronizado con sus controles.

  /** Normaliza la placa antes de validar o comunicarla para que la UI detecte duplicados canónicos. */
  function normalizePlate(raw) { return raw.trim().toUpperCase().replace(/[ -]/g, '') } // Unifica mayúsculas y elimina guiones y espacios.

  /** Valida todos los campos del formulario y cada archivo antes de consultar duplicados o guardar. */
  function validateForm() {
    const vin = form.vin.trim().toUpperCase() // Calcula el VIN canónico esperado por el servidor.
    const plate = normalizePlate(form.plate) // Calcula la placa canónica esperada por la Strategy.
    if (!vinPattern.test(vin)) return 'El VIN debe tener 17 caracteres válidos y no incluir I, O o Q.' // Rechaza VIN incompletos antes del HTTP.
    if (!plateRawPattern.test(form.plate.trim()) || !plateNormalizedPattern.test(plate)) return 'La placa debe contener de 5 a 8 caracteres alfanuméricos válidos; se excluyen I, Ñ, O y Q.' // Rechaza placas no compatibles con la normalización mexicana.
    for (const key of ['make', 'model', 'version']) if (!textPattern.test(form[key].trim())) return `Revisa el campo ${key}.` // Comprueba texto del fabricante y versión.
    if (!colorPattern.test(form.color.trim())) return 'Escribe un color válido.' // Comprueba que el color sea legible.
    if (!/^[0-9]{4}$/.test(form.year) || Number(form.year) < 1886 || Number(form.year) > 2100) return 'El año debe ser un número de cuatro dígitos entre 1886 y 2100.' // Revisa rango de año igual al DTO backend.
    if (form.mileage && !/^[0-9]{1,7}$/.test(form.mileage)) return 'El kilometraje debe ser entero no negativo o dejarse vacío.' // Valida kilometraje opcional.
    if (!form.client_id || !clientOptions.some(client => client.id === form.client_id)) return 'Selecciona un cliente activo de los resultados de búsqueda.' // Exige escoger una fila obtenida de la API.
    if (photos.some(file => !imageTypes.has(file.type) || file.size > 15 * 1024 * 1024)) return 'Cada archivo debe ser una imagen JPEG, PNG, GIF o BMP de máximo 15 MB.' // Bloquea formatos y tamaños inválidos en frontend.
    return '' // Señala que los controles locales permiten continuar.
  }

  /** Abre un formulario limpio y reinicia las coincidencias de cliente y fotografías. */
  function openForm() { setForm(emptyForm); setPhotos([]); setClientOptions([]); setClientTerm(''); setClientSearchMessage(''); setFormOpen(true); setError('') } // Evita arrastrar datos de un intento previo.

  /** Consulta la ficha completa de un vehículo activo y abre el popup con sus datos y fotos. */
  async function showDetails(value) {
    try { const detail = await request(`/vehicles/${value.id}`); setDialog({ kind: 'detail', vehicle: detail }) } // Pide una ficha actual al backend.
    catch (requestError) { setDialog({ kind: 'error', message: requestError.message }) } // Presenta fallos de consulta.
  }

  /** Confirma una baja lógica que conserva al vehículo, sus fotos y su relación con el cliente. */
  async function deactivate(value) {
    if (!window.confirm(`¿Desactivar el vehículo ${value.plate}? El registro y sus fotos se conservarán.`)) return // Requiere una acción consciente de la persona operadora.
    try { await request(`/vehicles/${value.id}`, { method: 'DELETE' }); setDialog({ kind: 'success', message: 'El vehículo se desactivó y dejó de aparecer en el listado.' }); await loadPage(page, search, direction) } // Ejecuta la baja y recarga el listado activo.
    catch (requestError) { setDialog({ kind: 'error', message: requestError.message }) } // Informa si la API rechazó la solicitud.
  }

  /** Valida entradas, detecta VIN/placa repetidos en API y registra el agregado con fotos. */
  async function submit(event) {
    event.preventDefault() // Mantiene el flujo dentro de React.
    const validation = validateForm() // Ejecuta todas las expresiones regulares locales.
    if (validation) { setDialog({ kind: 'error', message: validation }); return } // Usa popup para errores antes de la red.
    setBusy(true) // Bloquea el botón hasta recibir el resultado definitivo.
    try {
      const vin = form.vin.trim().toUpperCase() // Canoniza VIN antes de consultar y registrar.
      const plate = normalizePlate(form.plate) // Canoniza placa antes de consultar y registrar.
      const duplicate = await request(`/vehicles/duplicates?vin=${encodeURIComponent(vin)}&plate=${encodeURIComponent(plate)}`) // Solicita al backend su detector de duplicados.
      if (duplicate.vin_exists || duplicate.plate_exists) { setDialog({ kind: 'error', message: duplicate.vin_exists ? 'Ya existe un vehículo con ese VIN.' : 'Ya existe un vehículo con esa placa.' }); return } // No inicia el POST cuando una clave ya existe.
      const payload = { ...form, vin, plate, make: form.make.trim(), model: form.model.trim(), year: Number(form.year), version: form.version.trim(), color: form.color.trim(), mileage: form.mileage ? Number(form.mileage) : null } // Envía campos normalizados con año y kilometraje numéricos.
      const body = new FormData() // Construye multipart para JSON y varios archivos.
      body.append('data', new Blob([JSON.stringify(payload)], { type: 'application/json' })) // Agrega el DTO validado como parte JSON.
      photos.forEach(file => body.append('photos', file, file.name)) // Agrega cada imagen como parte repetida del mismo nombre.
      const created = await request('/vehicles', { method: 'POST', body }) // Ejecuta el Factory Method a través del API.
      setFormOpen(false) // Cierra el formulario después de la persistencia exitosa.
      setDialog({ kind: 'success', message: `El vehículo ${created.plate} quedó registrado con estatus activo.`, id: created.id }) // Confirma estado y UUIDv7 creado.
      await loadPage(0, search, direction) // Actualiza la página para mostrar el registro nuevo.
    } catch (requestError) { setDialog({ kind: 'error', message: requestError.message }) } // Comunica validación o fallo del servidor.
    finally { setBusy(false) } // Permite volver a operar al terminar la solicitud.
  }

  /** Envía la búsqueda del listado y reinicia el paginador para el nuevo filtro. */
  function submitSearch(event) {
    event.preventDefault() // Evita la navegación por envío HTML estándar.
    const normalized = term.trim() // Recorta espacios antes de construir el filtro.
    if (normalized && !/^[A-Za-z0-9 .'-]{1,120}$/.test(normalized)) { setError('El filtro acepta letras, números, espacios, punto, apóstrofe y guion.'); return } // Rechaza caracteres extraños antes del servidor.
    setSearch(normalized) // Guarda la búsqueda vigente en React.
    setPage(0) // Reinicia el índice de resultados.
    loadPage(0, normalized, direction) // Solicita la primera página filtrada.
  }

  /** Cambia el orden en el servidor y vuelve a la primera página de resultados. */
  function changeDirection(event) {
    const next = event.target.value // Lee solo ASC o DESC desde la lista controlada.
    setDirection(next) // Actualiza el selector visible.
    setPage(0) // Reinicia la navegación al cambiar el orden.
    loadPage(0, search, next) // Solicita al backend ordenar el conjunto completo.
  }

  if (!localStorage.getItem('taller-token')) return <Navigate to="/" replace/> // Exige una sesión activa antes de mostrar contenido protegido.
  if (!hasAnyRole(['ADMIN', 'RECEPCIONISTA'])) return <main className="role-page"><section className="role-content"><div className="role-card"><span className="form-kicker"><span/> ACCESO RESTRINGIDO</span><h1>Permiso <em>requerido.</em></h1><p>Solo administración y recepción pueden consultar o registrar vehículos.</p><Link to="/inicio" className="submit-button client-back-button">Volver al portal <ArrowRight size={17}/></Link></div></section></main> // Evita exponer la pantalla a roles no autorizados.

  return <main className="role-page client-page vehicle-page">{/* Presenta el catálogo con el mismo marco del módulo de clientes. */}
    <header className="role-nav"><Link className="brand" to="/inicio"><span className="brand-mark"><Wrench size={20}/></span><span>torque<span className="brand-period">.</span><small>TALLER MECÁNICO</small></span></Link><span className="role-user-badge"><Car size={15}/> ADMINISTRACIÓN DE VEHÍCULOS</span></header>
    <section className="role-content client-content"><Link to="/inicio" className="back-link"><ArrowLeft size={15}/> Volver al portal</Link>
      <div className="client-heading"><div><span className="form-kicker"><span/> FLOTILLA DEL TALLER</span><h1>Vehículos <em>activos.</em></h1><p className="role-description">Consulta fichas, propietarios y fotografías; desactiva sin perder el historial.</p></div><button className="submit-button client-new-button" type="button" onClick={openForm}><Plus size={17}/> Registrar vehículo</button></div>
      <div className="client-toolbar"><form className="client-search" onSubmit={submitSearch}><Search size={17}/><input value={term} onChange={event => setTerm(event.target.value)} maxLength={120} placeholder="Buscar por VIN, placa, marca o cliente"/><button className="role-list-button" type="submit">Buscar</button></form><label className="client-sort">Ordenar por fecha<select value={direction} onChange={changeDirection}><option value="DESC">Más recientes primero</option><option value="ASC">Más antiguos primero</option></select></label></div>
      {error && <div className="notice error" role="alert"><CircleHelp size={16}/>{error}</div>}
      <div className="client-table-wrap"><table className="client-table vehicle-table"><thead><tr><th>Vehículo</th><th>Identificación</th><th>Cliente activo</th><th>Estatus</th><th>Acciones</th></tr></thead><tbody>{loading ? <tr><td colSpan="5">Cargando vehículos...</td></tr> : pageData.content.length ? pageData.content.map(value => <tr key={value.id}><td><b>{value.make} {value.model} · {value.year}</b><small>{value.version} · {value.color} · {value.mileage == null ? 'Kilometraje no capturado' : `${value.mileage.toLocaleString('es-MX')} km`}</small></td><td><b>{value.plate}</b><small>{value.vin}</small></td><td>{value.client.name} {value.client.paternal_surname}<small>{value.client.email}</small></td><td><span className="vehicle-active-pill">{value.status}</span></td><td><div className="client-row-actions"><button type="button" onClick={() => showDetails(value)} aria-label="Consultar detalle"><Eye size={16}/></button><button type="button" onClick={() => deactivate(value)} aria-label="Desactivar vehículo"><Trash2 size={16}/></button></div></td></tr>) : <tr><td colSpan="5">No hay vehículos activos para mostrar.</td></tr>}</tbody></table></div>
      <div className="client-pagination"><span>{pageData.totalElements} vehículos activos · página {pageData.totalPages ? page + 1 : 0} de {pageData.totalPages}</span><div><button type="button" disabled={page <= 0 || loading} onClick={() => loadPage(page - 1)}><ArrowLeft size={15}/> Anterior</button><button type="button" disabled={page + 1 >= pageData.totalPages || loading} onClick={() => loadPage(page + 1)}>Siguiente <ArrowRight size={15}/></button></div></div>
    </section>
    {formOpen && <div className="workshop-modal-backdrop"><section className="workshop-modal client-modal vehicle-modal" role="dialog" aria-modal="true" aria-labelledby="vehicle-form-title"><header><div><span className="form-kicker"><span/> REGISTRO DE VEHÍCULO</span><h2 id="vehicle-form-title">Nuevo <em>vehículo.</em></h2></div><button className="workshop-modal-close" type="button" aria-label="Cerrar" onClick={() => setFormOpen(false)}><X size={18}/></button></header><form onSubmit={submit} noValidate>
      <div className="client-form-grid"><label className="field client-field"><span>VIN · 17 caracteres</span><input name="vin" value={form.vin} maxLength={17} onChange={change} autoCapitalize="characters" required/></label><label className="field client-field"><span>Placa mexicana</span><input name="plate" value={form.plate} maxLength={10} onChange={change} autoCapitalize="characters" placeholder="ABC-1234" required/><small>Se guarda en mayúsculas y sin guiones ni espacios; excluye I, Ñ, O y Q.</small></label><label className="field client-field"><span>Marca</span><input name="make" value={form.make} maxLength={80} onChange={change} required/></label><label className="field client-field"><span>Modelo</span><input name="model" value={form.model} maxLength={100} onChange={change} required/></label><label className="field client-field"><span>Año</span><input name="year" type="number" min="1886" max="2100" value={form.year} onChange={change} required/></label><label className="field client-field"><span>Versión</span><input name="version" value={form.version} maxLength={100} onChange={change} required/></label><label className="field client-field"><span>Color</span><input name="color" value={form.color} maxLength={50} onChange={change} required/></label><label className="field client-field"><span>Kilometraje · opcional</span><input name="mileage" type="number" min="0" max="2000000" value={form.mileage} onChange={change}/></label></div>
      <fieldset className="vehicle-client-picker"><legend>Asociar cliente activo · obligatorio</legend><div className="client-search"><Search size={16}/><input value={clientTerm} onChange={event => setClientTerm(event.target.value)} maxLength={120} placeholder="Buscar por nombre o correo" onKeyDown={event => { if (event.key === 'Enter') findActiveClients(event) }}/><button className="role-list-button" type="button" disabled={searchingClients} onClick={findActiveClients}>{searchingClients ? 'Buscando…' : 'Buscar cliente'}</button></div>{clientSearchMessage && <p className={`vehicle-client-search-message${clientOptions.length ? ' has-results' : ''}`} role="status">{clientSearchMessage}</p>}{clientOptions.length > 0 && <div className="vehicle-client-results" role="radiogroup" aria-label="Resultados de clientes activos">{clientOptions.map(client => <label className={`vehicle-client-result${form.client_id === client.id ? ' selected' : ''}`} key={client.id}><input type="radio" name="active_client" value={client.id} checked={form.client_id === client.id} onChange={() => setForm(current => ({ ...current, client_id: client.id }))}/><span><b>{client.name} {client.paternal_surname} {client.maternal_surname}</b><small>{client.email} · {client.phone}</small></span><span className="vehicle-client-status">Activo</span></label>)}</div>}</fieldset>
      <label className="field vehicle-photo-input"><span>Fotografías · varias imágenes, máximo 15 MB cada una</span><input type="file" accept="image/jpeg,image/png,image/gif,image/bmp" multiple onChange={event => setPhotos(Array.from(event.target.files ?? []))}/><small>{photos.length ? photos.map(file => `${file.name} (${(file.size / 1048576).toFixed(1)} MB)`).join(' · ') : 'Puedes adjuntar más de una foto.'}</small></label>
      <div className="secure-note"><ShieldCheck size={14}/> Solo clientes activos; el vehículo se registrará con estatus activo. El mecánico queda sin asignar.</div>{error && <div className="notice error" role="alert">{error}</div>}<button className="submit-button" disabled={busy}>{busy ? 'Validando y registrando...' : 'Registrar vehículo'}<ArrowRight size={17}/></button>
    </form></section></div>}
    {dialog && <div className="workshop-modal-backdrop"><section className={`workshop-modal ${dialog.kind === 'detail' ? 'vehicle-detail-modal' : ''}`} role="dialog" aria-modal="true" aria-labelledby="vehicle-dialog-title"><header><div>{dialog.kind === 'detail' ? <><span className="form-kicker"><span/> FICHA DEL VEHÍCULO</span><h2 id="vehicle-dialog-title">{dialog.vehicle.plate} <em>· {dialog.vehicle.status}</em></h2></> : <div className={dialog.kind === 'success' ? 'modal-status-success' : 'modal-status-error'}>{dialog.kind === 'success' ? <BadgeCheck size={23}/> : <CircleHelp size={23}/>}</div>}</div><button className="workshop-modal-close" type="button" aria-label="Cerrar" onClick={() => setDialog(null)}><X size={18}/></button></header>{dialog.kind === 'detail' ? <><dl className="vehicle-detail-grid"><dt>VIN</dt><dd>{dialog.vehicle.vin}</dd><dt>Marca y modelo</dt><dd>{dialog.vehicle.make} {dialog.vehicle.model} · {dialog.vehicle.year}</dd><dt>Versión y color</dt><dd>{dialog.vehicle.version} · {dialog.vehicle.color}</dd><dt>Kilometraje</dt><dd>{dialog.vehicle.mileage == null ? 'No capturado' : `${dialog.vehicle.mileage.toLocaleString('es-MX')} km`}</dd><dt>Cliente activo asociado</dt><dd>{dialog.vehicle.client.name} {dialog.vehicle.client.paternal_surname} {dialog.vehicle.client.maternal_surname} · {dialog.vehicle.client.email}</dd><dt>Mecánico</dt><dd>{dialog.vehicle.mechanic_id || 'Sin asignar · preparado para fase futura'}</dd><dt>Fecha de registro</dt><dd>{new Date(dialog.vehicle.fecha).toLocaleString('es-MX')}</dd></dl><div className="vehicle-photo-gallery">{dialog.vehicle.photos.length ? dialog.vehicle.photos.map(photo => <VehiclePhoto key={photo.id} vehicleId={dialog.vehicle.id} photo={photo}/>) : <p>Este vehículo aún no tiene fotografías.</p>}</div><small className="role-result">UUIDv7: {dialog.vehicle.id}</small></> : <><h2 id="vehicle-dialog-title">{dialog.kind === 'success' ? <>Proceso <em>completado.</em></> : <>Revisa la <em>solicitud.</em></>}</h2><p>{dialog.message}</p>{dialog.id && <small className="role-result">ID UUIDv7: {dialog.id}</small>}</>}<button className="submit-button" type="button" onClick={() => setDialog(null)}>Aceptar</button></section></div>}
  </main>
}
