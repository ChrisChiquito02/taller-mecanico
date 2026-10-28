import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight, BadgeCheck, CircleHelp, MapPin, Wrench, X, ImagePlus } from 'lucide-react'
import { request } from './api.js'

const patterns = {
  name: /^[\p{L}0-9 .,'&()/-]{2,120}$/u,
  rfc: /^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}$/,
  contact_email: /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/,
  commercial_status: /^(activo|inactivo|suspendido|mantenimiento)$/,
  street: /^[\p{L}0-9 .,'#/-]{1,120}$/u,
  street_number: /^[\p{L}0-9 .#/-]{1,20}$/u,
  colonia: /^[\p{L}0-9 .,'#/-]{2,120}$/u,
  postal_code: /^[0-9]{5}$/,
  state: /^[\p{L} .'-]{2,100}$/u,
  municipality: /^[\p{L} .'-]{2,100}$/u,
}

/** Agrupa la vista, validación, catálogos postales y alta del módulo de talleres. */
export default function workshop_facade() {
  const [form, set_form] = useState({ name: '', rfc: '', contact_email: '', commercial_status: 'activo', street: '', street_number: '', colonia: '', postal_code: '', state: '', municipality: '' })
  const [states, set_states] = useState([])
  const [municipalities, set_municipalities] = useState([])
  const [settlements, set_settlements] = useState([])
  const [state_id, set_state_id] = useState('')
  const [photo, set_photo] = useState(null)
  const [idempotency_key, set_idempotency_key] = useState('')
  const [error, set_error] = useState('')
  const [busy, set_busy] = useState(false)
  const [modal, set_modal] = useState(null)
  const [loading_catalog, set_loading_catalog] = useState(false)
  if (!localStorage.getItem('taller-token')) return <Navigate to="/" replace />

  /** Obtiene la colección de distintas envolturas JSON del catálogo del proveedor. */
  function array_data(body) {
    const data = body?.data ?? body?.results ?? body
    if (Array.isArray(data)) return data
    if (Array.isArray(data?.settlements)) return data.settlements
    if (Array.isArray(data?.colonias)) return data.colonias
    return []
  }
  /** Normaliza las variantes de nombre e identificador entregadas por PostalKit. */
  function item_name(item) { return String(item?.name ?? item?.nombre ?? item?.state ?? item?.municipality ?? item?.settlement ?? item?.colonia ?? '') }
  /** Busca una propiedad en los nodos del resultado postal sin asumir mayúsculas. */
  function find_value(node, keys) {
    if (!node || typeof node !== 'object') return ''
    for (const key of keys) if (node[key] != null && typeof node[key] !== 'object') return String(node[key])
    for (const child of Object.values(node)) { const match = find_value(child, keys); if (match) return match }
    return ''
  }
  /** Carga estados al abrir la pantalla del formulario. */
  useEffect(() => { request('/workshops/catalog/states').then(body => set_states(array_data(body))).catch(catalog_error => set_error(catalog_error.message)) }, [])

  /** Cambia un dato del formulario y descarta mensajes anteriores. */
  function change(event) {
    const { name, value } = event.target
    set_form(current => ({ ...current, [name]: name === 'rfc' ? value.toUpperCase() : value }))
    set_idempotency_key('')
    set_error('')
  }
  /** Carga municipios al elegir estado y mantiene nombre e identificador en el formulario. */
  async function select_state(event) {
    const chosen = states.find(item => String(item.id ?? item.state_id ?? item.code ?? item_name(item)) === event.target.value)
    const value = item_name(chosen)
    set_state_id(event.target.value)
    set_form(current => ({ ...current, state: value, municipality: '', colonia: '', postal_code: '' }))
    set_idempotency_key('')
    set_municipalities([]); set_settlements([]); set_error('')
    if (!chosen) return
    try { set_loading_catalog(true); set_municipalities(array_data(await request(`/workshops/catalog/states/${encodeURIComponent(event.target.value)}/municipalities`))) }
    catch (catalog_error) { set_error(catalog_error.message) } finally { set_loading_catalog(false) }
  }
  /** Cambia el municipio y reinicia la selección dependiente de colonia y código postal. */
  function select_municipality(event) {
    const chosen = municipalities.find(item => String(item.id ?? item.municipality_id ?? item_name(item)) === event.target.value)
    set_form(current => ({ ...current, municipality: item_name(chosen), colonia: '', postal_code: '' }))
    set_idempotency_key('')
    set_settlements([]); set_error('')
  }
  /** Busca el catálogo postal al capturar cinco dígitos y autocompleta los campos de dirección. */
  async function lookup_postal_code(code) {
    if (!patterns.postal_code.test(code)) return
    try {
      set_loading_catalog(true)
      const body = await request(`/workshops/catalog/postal-codes/${encodeURIComponent(code)}`)
      const data = body?.data ?? body
      const rows = array_data(body)
      const state = typeof data?.state === 'string' ? data.state : data?.state?.name ?? data?.estado?.nombre ?? data?.state_name ?? data?.nombre_estado ?? ''
      const municipality = typeof data?.municipality === 'string' ? data.municipality : data?.municipality?.name ?? data?.municipio?.nombre ?? data?.municipality_name ?? data?.nombre_municipio ?? ''
      const postal_settlements = rows.length ? rows : array_data(data)
      set_settlements(postal_settlements)
      set_form(current => ({ ...current, state: state || current.state, municipality: municipality || current.municipality, colonia: postal_settlements.length === 1 ? item_name(postal_settlements[0]) : current.colonia, postal_code: code }))
      set_idempotency_key('')
      const matched_state = states.find(item => item_name(item).toLocaleLowerCase('es-MX') === state.toLocaleLowerCase('es-MX'))
      if (matched_state) {
        const id = String(matched_state.id ?? matched_state.state_id ?? matched_state.code ?? item_name(matched_state)); set_state_id(id)
        set_municipalities(array_data(await request(`/workshops/catalog/states/${encodeURIComponent(id)}/municipalities`)))
      }
    } catch (catalog_error) { set_error(catalog_error.message) } finally { set_loading_catalog(false) }
  }
  /** Busca y asigna código postal al elegir una colonia bajo estado y municipio. */
  async function select_colonia(event) {
    const colonia = event.target.value
    const chosen = settlements.find(item => item_name(item) === colonia)
    const direct_code = chosen?.postal_code?.code ?? chosen?.postal_code ?? chosen?.postalCode?.code ?? chosen?.postalCode ?? chosen?.codigo_postal?.codigo ?? chosen?.codigo_postal ?? chosen?.cp
    set_form(current => ({ ...current, colonia, postal_code: direct_code ? String(direct_code) : current.postal_code }))
    set_idempotency_key('')
    if (direct_code || !form.state || !form.municipality || !colonia) return
    try {
      const query = new URLSearchParams({ state: form.state, municipality: form.municipality, colonia })
      const result = await request(`/workshops/catalog/postal-codes/search?${query}`)
      const postal_code = find_value(result, ['postal_code', 'postalCode', 'codigo_postal', 'zip_code', 'cp', 'code'])
      if (postal_code) set_form(current => ({ ...current, postal_code }))
    } catch (catalog_error) { set_error(catalog_error.message) }
  }
  /** Valida todos los campos y archivo con expresiones regulares antes de solicitar o guardar. */
  function validate_form() {
    for (const [field, pattern] of Object.entries(patterns)) if (!pattern.test(String(form[field] ?? '').trim())) return `Revisa el campo ${field.replaceAll('_', ' ')}; el formato no es válido.`
    if (!photo) return 'Selecciona una fotografía del taller.'
    if (!photo.type.startsWith('image/')) return 'El archivo seleccionado debe ser una imagen.'
    if (photo.size > 15 * 1024 * 1024) return 'La fotografía no puede superar 15 MB.'
    return ''
  }
  /** Comprueba duplicado por RFC y crea el taller con token de idempotencia. */
  async function submit_workshop(event) {
    event.preventDefault(); set_error(''); const validation = validate_form()
    if (validation) { set_error(validation); return }
    set_busy(true)
    try {
      const request_key = idempotency_key || crypto.randomUUID()
      set_idempotency_key(request_key)
      const duplicate = await request(`/workshops/exists?rfc=${encodeURIComponent(form.rfc)}`)
      if (duplicate.exists) throw new Error('Ya existe un taller registrado con ese RFC.')
      const data = new FormData(); data.append('data', new Blob([JSON.stringify(form)], { type: 'application/json' })); data.append('photo', photo)
      const result = await request('/workshops', { method: 'POST', body: data, headers: { 'Idempotency-Key': request_key } })
      set_modal({ kind: 'success', message: `El taller ${result.name} se registró correctamente.`, id: result.id })
      set_form({ name: '', rfc: '', contact_email: '', commercial_status: 'activo', street: '', street_number: '', colonia: '', postal_code: '', state: '', municipality: '' }); set_photo(null); set_settlements([])
      set_idempotency_key('')
    } catch (request_error) { set_modal({ kind: 'error', message: request_error.message }) }
    finally { set_busy(false) }
  }
  /** Cierra el cuadro modal de confirmación o error. */
  function close_modal() { set_modal(null) }

  return <main className="role-page workshop-page"><header className="role-nav"><Link className="brand" to="/inicio"><span className="brand-mark"><Wrench size={20}/></span><span>torque<span className="brand-period">.</span><small>TALLER MECÁNICO</small></span></Link><span className="role-user-badge"><MapPin size={15}/> ALTA DE TALLER</span></header><section className="role-content workshop-content"><Link to="/inicio" className="back-link"><ArrowLeft size={15}/> Volver al portal</Link><div className="role-card workshop-card"><span className="form-kicker"><span/> REGISTRO COMERCIAL</span><h1>Nuevo <em>taller.</em></h1><p className="role-description">Completa la información comercial y la dirección del taller.</p><form className="role-form workshop-form" onSubmit={submit_workshop} noValidate>
    <div className="workshop-fields"><label className="field"><span>Nombre del taller</span><input name="name" value={form.name} maxLength={120} onChange={change} placeholder="Ej. Torque Garage" required/></label><label className="field"><span>RFC</span><input name="rfc" value={form.rfc} maxLength={13} onChange={change} placeholder="XAXX010101000" required/></label><label className="field"><span>Correo de contacto</span><input name="contact_email" type="email" value={form.contact_email} maxLength={254} onChange={change} placeholder="contacto@taller.mx" required/></label><label className="field"><span>Estatus comercial</span><select name="commercial_status" value={form.commercial_status} onChange={change}><option value="activo">Activo</option><option value="inactivo">Inactivo</option><option value="suspendido">Suspendido</option><option value="mantenimiento">En mantenimiento</option></select></label>
    <label className="field workshop-photo"><span>Fotografía del taller · máximo 15 MB</span><div className="photo-input"><ImagePlus size={18}/><input type="file" accept="image/*" onChange={event => { set_photo(event.target.files?.[0] ?? null); set_idempotency_key(''); set_error('') }}/></div>{photo && <small>{photo.name} · {(photo.size / 1048576).toFixed(2)} MB</small>}</label><label className="field"><span>Calle</span><input name="street" value={form.street} maxLength={120} onChange={change} required/></label><label className="field"><span>Número</span><input name="street_number" value={form.street_number} maxLength={20} onChange={change} required/></label><label className="field"><span>Código postal</span><input name="postal_code" inputMode="numeric" value={form.postal_code} maxLength={5} onChange={event => { change(event); if (event.target.value.length === 5) lookup_postal_code(event.target.value) }} onBlur={() => lookup_postal_code(form.postal_code)} placeholder="00000" required/></label>
    <label className="field"><span>Estado</span><select value={state_id} onChange={select_state} required><option value="">Selecciona estado</option>{states.map((item,index) => <option key={item.id ?? item.state_id ?? item_name(item) ?? index} value={item.id ?? item.state_id ?? item.code ?? item_name(item)}>{item_name(item)}</option>)}</select></label><label className="field"><span>Municipio</span><select value={municipalities.find(item => item_name(item) === form.municipality)?.id ?? municipalities.find(item => item_name(item) === form.municipality)?.municipality_id ?? form.municipality} onChange={select_municipality} required><option value="">Selecciona municipio</option>{municipalities.map((item,index) => <option key={item.id ?? item.municipality_id ?? item_name(item) ?? index} value={item.id ?? item.municipality_id ?? item_name(item)}>{item_name(item)}</option>)}</select></label><label className="field"><span>Colonia</span>{settlements.length ? <select name="colonia" value={form.colonia} onChange={select_colonia} required><option value="">Selecciona colonia</option>{settlements.map((item,index) => <option key={item.id ?? item_name(item) ?? index} value={item_name(item)}>{item_name(item)}</option>)}</select> : <input name="colonia" value={form.colonia} maxLength={120} onChange={change} onBlur={select_colonia} placeholder="Teclea código postal primero" required/>}</label></div>
    {loading_catalog && <small className="workshop-catalog-loading">Consultando catálogo postal...</small>}{error && <div className="notice error" role="alert"><CircleHelp size={16}/><span>{error}</span></div>}<button className="submit-button" type="submit" disabled={busy}>{busy ? 'Validando y registrando...' : 'Registrar taller'}<ArrowRight size={17}/></button>
  </form></div></section>{modal && <div className="workshop-modal-backdrop" role="presentation"><section className="workshop-modal" role="dialog" aria-modal="true"><header><div className={modal.kind === 'success' ? 'modal-status-success' : 'modal-status-error'}>{modal.kind === 'success' ? <BadgeCheck size={23}/> : <CircleHelp size={23}/>}</div><button className="workshop-modal-close" type="button" aria-label="Cerrar" onClick={close_modal}><X size={18}/></button></header><h2>{modal.kind === 'success' ? <>Registro <em>confirmado.</em></> : <>No se pudo <em>registrar.</em></>}</h2><p>{modal.message}</p>{modal.id && <small className="role-result">ID UUIDv7: {modal.id}</small>}<button className="submit-button" type="button" onClick={close_modal}>Aceptar</button></section></div>}</main>
}
