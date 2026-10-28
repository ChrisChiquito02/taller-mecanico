import { useCallback, useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight, BadgeCheck, CircleHelp, Pencil, Plus, Search, ShieldCheck, Trash2, Users, Wrench, X } from 'lucide-react'
import { hasAnyRole, request } from './api.js'

const emptyForm = { name: '', paternal_surname: '', maternal_surname: '', email: '', phone: '', personal_phone: '', work_phone: '', alternate_contact_phone: '', alternate_contact_name: '', street: '', street_number: '', colonia: '', state: '', municipality: '', postal_code: '', workshop_ids: [] }
const textPattern = /^[\p{L}][\p{L} .'-]{1,119}$/u
const contactNamePattern = /^[\p{L}][\p{L} .'-]{1,239}$/u
const phonePattern = /^\+?[0-9 ()-]{10,20}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/
const addressPatterns = { street: /^[\p{L}0-9 .,'#/-]{1,120}$/u, street_number: /^[\p{L}0-9 .#/-]{1,20}$/u, colonia: /^[\p{L}0-9 .,'#/-]{2,120}$/u, state: /^[\p{L} .'-]{2,100}$/u, municipality: /^[\p{L} .'-]{2,100}$/u, postal_code: /^[0-9]{5}$/ }

/** Concentra formulario, validación, paginación, llamadas y resultados del módulo de clientes. */
export default function ClientFacade() {
  const [pageData, setPageData] = useState({ content: [], number: 0, totalPages: 0, totalElements: 0 })
  const [workshops, setWorkshops] = useState([])
  const [states, setStates] = useState([])
  const [municipalities, setMunicipalities] = useState([])
  const [settlements, setSettlements] = useState([])
  const [stateId, setStateId] = useState('')
  const [loadingCatalog, setLoadingCatalog] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)
  const [formOpen, setFormOpen] = useState(false)
  const [term, setTerm] = useState('')
  const [search, setSearch] = useState('')
  const [direction, setDirection] = useState('DESC')
  const [page, setPage] = useState(0)
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [dialog, setDialog] = useState(null)

  /** Descarga una página del servidor y conserva solo los clientes activos. */
  const loadPage = useCallback(async (requestedPage = page, requestedTerm = search) => {
    setLoading(true)
    setError('')
    try {
      const result = await request(`/clients?page=${requestedPage}&size=10&term=${encodeURIComponent(requestedTerm)}&direction=${direction}`)
      setPageData(result)
      setPage(result.number ?? requestedPage)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }, [direction, page, search])

  /** Carga el catálogo de talleres que pueden asociarse a un cliente nuevo o actualizado. */
  useEffect(() => {
    request('/workshops/catalog').then(setWorkshops).catch(requestError => setError(requestError.message))
    loadPage(0, '')
  }, [])

  /** Unwraps PostalKit list responses, which may nest entries in data or results. */
  function arrayData(body) {
    const data = body?.data ?? body?.results ?? body
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.settlements)) return data.settlements
    if (Array.isArray(data?.colonias)) return data.colonias
    return []
  }

  /** Reads the display label used by PostalKit catalog objects. */
  function itemName(item) {
    return String(item?.name ?? item?.nombre ?? item?.state ?? item?.municipality ?? item?.settlement ?? item?.colonia ?? '')
  }

  /** Finds a scalar property in PostalKit's nested postal lookup response. */
  function findValue(node, keys) {
    if (!node || typeof node !== 'object') return ''
    for (const key of keys) if (node[key] != null && typeof node[key] !== 'object') return String(node[key])
    for (const child of Object.values(node)) { const match = findValue(child, keys); if (match) return match }
    return ''
  }

  /** Loads the state catalog when a user opens a client address form. */
  useEffect(() => {
    if (!formOpen || states.length) return
    request('/workshops/catalog/states').then(body => setStates(arrayData(body))).catch(requestError => setDialog({ kind: 'error', message: requestError.message }))
  }, [formOpen, states.length])

  /** Restores the selected state and municipality options for edited addresses and postal lookups. */
  useEffect(() => {
    if (!formOpen || !states.length || !form.state) return
    const matched = states.find(item => itemName(item).toLocaleLowerCase('es-MX') === form.state.toLocaleLowerCase('es-MX'))
    if (!matched) return
    const id = String(matched.id ?? matched.state_id ?? matched.code ?? itemName(matched))
    setStateId(id)
    let current = true
    setLoadingCatalog(true)
    request(`/workshops/catalog/states/${encodeURIComponent(id)}/municipalities`).then(body => { if (current) setMunicipalities(arrayData(body)) }).catch(requestError => { if (current) setDialog({ kind: 'error', message: requestError.message }) }).finally(() => { if (current) setLoadingCatalog(false) })
    return () => { current = false }
  }, [formOpen, form.state, states])

  /** Selects a state and fetches its municipality catalog through the workshop PostalKit proxy. */
  async function selectState(event) {
    const selected = states.find(item => String(item.id ?? item.state_id ?? item.code ?? itemName(item)) === event.target.value)
    setStateId(event.target.value)
    setForm(current => ({ ...current, state: itemName(selected), municipality: '', colonia: '', postal_code: '' }))
    setMunicipalities([]); setSettlements([])
    if (!selected) return
    setLoadingCatalog(true)
    try { setMunicipalities(arrayData(await request(`/workshops/catalog/states/${encodeURIComponent(event.target.value)}/municipalities`))) }
    catch (requestError) { setDialog({ kind: 'error', message: requestError.message }) }
    finally { setLoadingCatalog(false) }
  }

  /** Selects a municipality and resets the dependent settlement and postal code fields. */
  function selectMunicipality(event) {
    const selected = municipalities.find(item => String(item.id ?? item.municipality_id ?? itemName(item)) === event.target.value)
    setForm(current => ({ ...current, municipality: itemName(selected), colonia: '', postal_code: '' }))
    setSettlements([])
  }

  /** Looks up a five-digit postal code and fills its state, municipality, and settlements. */
  async function lookupPostalCode(code) {
    if (!/^[0-9]{5}$/.test(code)) return
    setLoadingCatalog(true)
    try {
      const body = await request(`/workshops/catalog/postal-codes/${encodeURIComponent(code)}`)
      const data = body?.data ?? body
      const state = typeof data?.state === 'string' ? data.state : data?.state?.name ?? data?.estado?.nombre ?? data?.state_name ?? data?.nombre_estado ?? ''
      const municipality = typeof data?.municipality === 'string' ? data.municipality : data?.municipality?.name ?? data?.municipio?.nombre ?? data?.municipality_name ?? data?.nombre_municipio ?? ''
      const rows = arrayData(body)
      const postalSettlements = rows.length ? rows : arrayData(data)
      setSettlements(postalSettlements)
      setForm(current => ({ ...current, state: state || current.state, municipality: municipality || current.municipality, colonia: postalSettlements.length === 1 ? itemName(postalSettlements[0]) : current.colonia, postal_code: code }))
    } catch (requestError) { setDialog({ kind: 'error', message: requestError.message }) }
    finally { setLoadingCatalog(false) }
  }

  /** Selects a settlement and resolves its postal code from PostalKit when not included directly. */
  async function selectSettlement(event) {
    const colonia = event.target.value
    const selected = settlements.find(item => itemName(item) === colonia)
    const directCode = selected?.postal_code?.code ?? selected?.postal_code ?? selected?.postalCode?.code ?? selected?.postalCode ?? selected?.codigo_postal?.codigo ?? selected?.codigo_postal ?? selected?.cp
    setForm(current => ({ ...current, colonia, postal_code: directCode ? String(directCode) : current.postal_code }))
    if (directCode || !form.state || !form.municipality || !colonia) return
    try {
      const query = new URLSearchParams({ state: form.state, municipality: form.municipality, colonia })
      const result = await request(`/workshops/catalog/postal-codes/search?${query}`)
      const postalCode = findValue(result, ['postal_code', 'postalCode', 'codigo_postal', 'zip_code', 'cp', 'code'])
      if (postalCode) setForm(current => ({ ...current, postal_code: postalCode }))
    } catch (requestError) { setDialog({ kind: 'error', message: requestError.message }) }
  }

  /** Bloquea la pantalla a cualquier cuenta que no tenga ADMIN o RECEPCIONISTA. */
  if (!localStorage.getItem('taller-token')) return <Navigate to="/" replace />
  if (!hasAnyRole(['ADMIN', 'RECEPCIONISTA'])) return <main className="role-page"><section className="role-content"><div className="role-card"><span className="form-kicker"><span/> ACCESO RESTRINGIDO</span><h1>Permiso <em>requerido.</em></h1><p>Solo administración y recepción pueden consultar y administrar clientes.</p><Link to="/inicio" className="submit-button client-back-button">Volver al portal <ArrowRight size={17}/></Link></div></section></main>

  /** Actualiza un campo y descarta el mensaje anterior del diálogo. */
  function change(event) {
    const { name, value } = event.target
    setForm(current => ({ ...current, [name]: value }))
  }

  /** Marca o desmarca un taller, manteniendo asociaciones muchos a muchos. */
  function toggleWorkshop(id) {
    setForm(current => ({ ...current, workshop_ids: current.workshop_ids.includes(id) ? current.workshop_ids.filter(item => item !== id) : [...current.workshop_ids, id] }))
  }

  /** Comprueba todos los campos con expresiones regulares antes de enviar datos al backend. */
  function validateForm() {
    for (const key of ['name', 'paternal_surname', 'maternal_surname']) if (!textPattern.test(form[key].trim())) return `Revisa el campo ${key.replaceAll('_', ' ')}.`
    if (!contactNamePattern.test(form.alternate_contact_name.trim())) return 'Revisa el nombre del contacto alternativo.'
    if (!emailPattern.test(form.email.trim())) return 'Escribe un correo de contacto válido.'
    for (const key of ['phone', 'personal_phone', 'work_phone', 'alternate_contact_phone']) {
      const digits = form[key].replace(/[^0-9]/g, '')
      if (!phonePattern.test(form[key].trim()) || digits.length < 10 || digits.length > 15) return `Revisa el campo ${key.replaceAll('_', ' ')}; debe contener de 10 a 15 dígitos.`
    }
    for (const [key, pattern] of Object.entries(addressPatterns)) if (!pattern.test(form[key].trim())) return `Revisa el campo ${key.replaceAll('_', ' ')}.`
    if (form.workshop_ids.length === 0) return 'Asocia al cliente con al menos un taller registrado.'
    return ''
  }

  /** Abre el formulario limpio para un cliente nuevo. */
  function createNew() {
    setEditingId(null)
    setForm(emptyForm)
    setFormOpen(true)
  }

  /** Copia los campos de la fila elegida al formulario de actualización. */
  function editClient(value) {
    setEditingId(value.id)
    setForm({ ...emptyForm, ...value, workshop_ids: value.workshops.map(item => item.id) })
    setFormOpen(true)
  }

  /** Valida, crea o actualiza al cliente y recarga la página para reflejar el resultado persistido. */
  async function submit(event) {
    event.preventDefault()
    const validation = validateForm()
    if (validation) { setDialog({ kind: 'error', message: validation }); return }
    setBusy(true)
    try {
      const payload = { ...form, email: form.email.trim().toLowerCase() }
      const result = await request(editingId ? `/clients/${editingId}` : '/clients', { method: editingId ? 'PUT' : 'POST', body: JSON.stringify(payload) })
      setFormOpen(false)
      setDialog({ kind: 'success', message: `El cliente ${result.name} ${result.paternal_surname} se guardó correctamente.`, id: result.id })
      await loadPage(0, search)
    } catch (requestError) {
      setDialog({ kind: 'error', message: requestError.message })
    } finally {
      setBusy(false)
    }
  }

  /** Confirma y solicita suspensión lógica; la API conserva los datos históricos. */
  async function deactivate(value) {
    if (!window.confirm(`¿Suspender a ${value.name} ${value.paternal_surname}? Sus datos permanecerán guardados.`)) return
    try {
      await request(`/clients/${value.id}`, { method: 'DELETE' })
      setDialog({ kind: 'success', message: 'El cliente se suspendió y dejó de aparecer en el listado activo.' })
      await loadPage(page, search)
    } catch (requestError) {
      setDialog({ kind: 'error', message: requestError.message })
    }
  }

  /** Envía el filtro al servicio paginado y vuelve al primer bloque de resultados. */
  function submitSearch(event) {
    event.preventDefault()
    const normalized = term.trim()
    if (normalized && !/^[\p{L}0-9 .@_' -]{1,120}$/u.test(normalized)) { setError('El filtro contiene caracteres no permitidos.'); return }
    setSearch(normalized)
    loadPage(0, normalized)
  }

  /** Cambia el orden cronológico del servidor y recarga desde la primera página. */
  function changeDirection(event) {
    const nextDirection = event.target.value
    setDirection(nextDirection)
    setPage(0)
    setLoading(true)
    request(`/clients?page=0&size=10&term=${encodeURIComponent(search)}&direction=${nextDirection}`)
      .then(result => { setPageData(result); setPage(result.number ?? 0); setError('') })
      .catch(requestError => setError(requestError.message))
      .finally(() => setLoading(false))
  }

  /** Renderiza un control validado con el estado del registro que se administra. */
  function field(key, label, type = 'text', maxLength = 120) {
    return <label className="field client-field" key={key}><span>{label}</span><input name={key} type={type} value={form[key]} maxLength={maxLength} onChange={change} required /></label>
  }

  return <main className="role-page client-page">
    <header className="role-nav"><Link className="brand" to="/inicio"><span className="brand-mark"><Wrench size={20}/></span><span>torque<span className="brand-period">.</span><small>TALLER MECÁNICO</small></span></Link><span className="role-user-badge"><Users size={15}/> ADMINISTRACIÓN DE CLIENTES</span></header>
    <section className="role-content client-content"><Link to="/inicio" className="back-link"><ArrowLeft size={15}/> Volver al portal</Link>
      <div className="client-heading"><div><span className="form-kicker"><span/> DIRECTORIO DEL TALLER</span><h1>Clientes <em>activos.</em></h1><p className="role-description">Consulta, actualiza o suspende registros sin borrar su historial.</p></div><button className="submit-button client-new-button" type="button" onClick={createNew}><Plus size={17}/> Registrar cliente</button></div>
      <div className="client-toolbar"><form className="client-search" onSubmit={submitSearch}><Search size={17}/><input value={term} onChange={event => setTerm(event.target.value)} maxLength={120} placeholder="Buscar por nombre o correo"/><button className="role-list-button" type="submit">Buscar</button></form><label className="client-sort">Ordenar por fecha<select value={direction} onChange={changeDirection}><option value="DESC">Más recientes primero</option><option value="ASC">Más antiguos primero</option></select></label></div>
      {error && <div className="notice error" role="alert"><CircleHelp size={16}/>{error}</div>}
      <div className="client-table-wrap"><table className="client-table"><thead><tr><th>Cliente</th><th>Contacto</th><th>Talleres asociados</th><th>Alta</th><th>Acciones</th></tr></thead><tbody>{loading ? <tr><td colSpan="5">Cargando registros...</td></tr> : pageData.content.length ? pageData.content.map(value => <tr key={value.id}><td><b>{value.name} {value.paternal_surname} {value.maternal_surname}</b><small>{value.id}</small></td><td>{value.email}<small>{value.phone}</small></td><td>{value.workshops.map(item => item.name).join(', ')}</td><td>{new Date(value.fecha).toLocaleDateString('es-MX')}</td><td><div className="client-row-actions"><button type="button" onClick={() => editClient(value)} aria-label="Actualizar cliente"><Pencil size={16}/></button><button type="button" onClick={() => deactivate(value)} aria-label="Suspender cliente"><Trash2 size={16}/></button></div></td></tr>) : <tr><td colSpan="5">No hay clientes activos para mostrar.</td></tr>}</tbody></table></div>
      <div className="client-pagination"><span>{pageData.totalElements} clientes activos · página {pageData.totalPages ? page + 1 : 0} de {pageData.totalPages}</span><div><button type="button" disabled={page <= 0 || loading} onClick={() => loadPage(page - 1)}><ArrowLeft size={15}/> Anterior</button><button type="button" disabled={page + 1 >= pageData.totalPages || loading} onClick={() => loadPage(page + 1)}>Siguiente <ArrowRight size={15}/></button></div></div>
    </section>
    {formOpen && <div className="workshop-modal-backdrop"><section className="workshop-modal client-modal" role="dialog" aria-modal="true" aria-labelledby="client-form-title"><header><div><span className="form-kicker"><span/> REGISTRO DE PERSONA</span><h2 id="client-form-title">{editingId ? 'Actualizar' : 'Nuevo'} <em>cliente.</em></h2></div><button className="workshop-modal-close" type="button" aria-label="Cerrar" onClick={() => setFormOpen(false)}><X size={18}/></button></header><form onSubmit={submit} noValidate>
      <div className="client-form-grid">{field('name','Nombre')}{field('paternal_surname','Apellido paterno')}{field('maternal_surname','Apellido materno')}{field('email','Correo de contacto','email',190)}{field('phone','Teléfono general','tel',20)}{field('personal_phone','Teléfono personal','tel',20)}{field('work_phone','Teléfono del trabajo','tel',20)}{field('alternate_contact_phone','Teléfono del contacto alternativo','tel',20)}{field('alternate_contact_name','Nombre del contacto alternativo','text',240)}{field('street','Calle')}{field('street_number','Número','text',20)}
        <label className="field client-field"><span>Código postal</span><input name="postal_code" inputMode="numeric" value={form.postal_code} maxLength={5} onChange={event => { change(event); if (event.target.value.length === 5) lookupPostalCode(event.target.value) }} onBlur={() => lookupPostalCode(form.postal_code)} placeholder="00000" required/></label>
        <label className="field client-field"><span>Estado</span><select value={stateId} onChange={selectState} required><option value="">Selecciona estado</option>{states.map((item, index) => <option key={item.id ?? item.state_id ?? itemName(item) ?? index} value={item.id ?? item.state_id ?? item.code ?? itemName(item)}>{itemName(item)}</option>)}</select></label>
        <label className="field client-field"><span>Municipio</span><select value={municipalities.find(item => itemName(item) === form.municipality)?.id ?? municipalities.find(item => itemName(item) === form.municipality)?.municipality_id ?? form.municipality} onChange={selectMunicipality} required disabled={!municipalities.length}><option value="">{loadingCatalog ? 'Cargando catálogo…' : 'Selecciona municipio'}</option>{municipalities.map((item, index) => <option key={item.id ?? item.municipality_id ?? itemName(item) ?? index} value={item.id ?? item.municipality_id ?? itemName(item)}>{itemName(item)}</option>)}</select></label>
        <label className="field client-field"><span>Colonia</span>{settlements.length ? <select name="colonia" value={form.colonia} onChange={selectSettlement} required><option value="">Selecciona colonia</option>{settlements.map((item, index) => <option key={item.id ?? itemName(item) ?? index} value={itemName(item)}>{itemName(item)}</option>)}</select> : <input name="colonia" value={form.colonia} maxLength={120} onChange={change} onBlur={event => selectSettlement(event)} placeholder="Captura el código postal para consultar colonias" required/>}</label>
      </div>
      <fieldset className="client-workshops"><legend>Taller asociado · selecciona uno o más</legend>{workshops.length ? workshops.map(item => <label key={item.id}><input type="checkbox" checked={form.workshop_ids.includes(item.id)} onChange={() => toggleWorkshop(item.id)}/><span>{item.name} <small>{item.rfc} · {item.commercial_status}</small></span></label>) : <p>No hay talleres registrados. Registra un taller antes de continuar.</p>}</fieldset>
      <div className="secure-note"><ShieldCheck size={14}/> Los datos se validan en frontend y backend.</div><button className="submit-button" disabled={busy || workshops.length === 0}>{busy ? 'Guardando...' : editingId ? 'Guardar cambios' : 'Registrar cliente'}<ArrowRight size={17}/></button>
    </form></section></div>}
    {dialog && <div className="workshop-modal-backdrop"><section className="workshop-modal" role="dialog" aria-modal="true"><header><div className={dialog.kind === 'success' ? 'modal-status-success' : 'modal-status-error'}>{dialog.kind === 'success' ? <BadgeCheck size={23}/> : <CircleHelp size={23}/>}</div><button className="workshop-modal-close" type="button" aria-label="Cerrar" onClick={() => setDialog(null)}><X size={18}/></button></header><h2>{dialog.kind === 'success' ? <>Proceso <em>completado.</em></> : <>Revisa la <em>solicitud.</em></>}</h2><p>{dialog.message}</p>{dialog.id && <small className="role-result">ID UUIDv7: {dialog.id}</small>}<button className="submit-button" type="button" onClick={() => setDialog(null)}>Aceptar</button></section></div>}
  </main>
}
