import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight, BadgeCheck, CircleHelp, UserRound, Wrench, X, ClipboardList } from 'lucide-react'
import { request } from './api.js'

const ROLE_NAME_REGEX = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+(?:[ '-][A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$/u

/** Centraliza la entrada, validación, envío y salida del módulo de creación de roles. */
export default function RolesFacade() {
  const [name, setName] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [createdRole, setCreatedRole] = useState(null)
  const [busy, setBusy] = useState(false)
  const [showRoles, setShowRoles] = useState(false)
  const [roles, setRoles] = useState([])
  const [listError, setListError] = useState('')
  const [editingRole, setEditingRole] = useState(null)
  const [editedName, setEditedName] = useState('')
  if (!localStorage.getItem('taller-token')) return <Navigate to="/" replace />

  /** Valida el patrón y la longitud, consulta duplicados y envía el rol nuevo. */
  async function submitRole(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    setCreatedRole(null)

    const normalizedName = name.trim().replace(/\s+/g, ' ')
    if (normalizedName.length > 80 || !ROLE_NAME_REGEX.test(normalizedName)) {
      setError('Usa entre 1 y 80 caracteres: letras, espacios, apóstrofes o guiones.')
      return
    }

    setBusy(true)
    try {
      const duplicate = await request(`/roles/exists?name=${encodeURIComponent(normalizedName)}`)
      if (duplicate.exists) {
        setError('Ya existe un rol con ese nombre.')
        return
      }
      const result = await request('/roles', {
        method: 'POST',
        body: JSON.stringify({ name: normalizedName }),
      })
      setCreatedRole(result)
      setMessage('El rol se creó correctamente.')
      setName('')
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setBusy(false)
    }
  }

  /** Actualiza el valor escrito por el administrador y limpia resultados anteriores. */
  function updateName(event) {
    setName(event.target.value)
    setError('')
    setMessage('')
    setCreatedRole(null)
  }

  /** Consulta el catálogo y abre el modal para visualizar los roles creados. */
  async function openRoles() {
    setListError('')
    try { setRoles(await request('/roles')); setShowRoles(true) }
    catch (requestError) { setListError(requestError.message); setShowRoles(true) }
  }

  /** Guarda un nombre nuevo después de comprobar el patrón y actualizar la fila persistida. */
  async function renameRole(role) {
    const normalized = editedName.trim().replace(/\s+/g, ' ')
    if (normalized.length > 80 || !ROLE_NAME_REGEX.test(normalized)) { setListError('El nombre debe contener letras y separadores permitidos, con máximo 80 caracteres.'); return }
    try {
      const updated = await request(`/roles/${role.id}`, { method: 'PUT', body: JSON.stringify({ name: normalized }) })
      setRoles(current => current.map(item => item.id === updated.id ? updated : item))
      setEditingRole(null)
      setListError('')
    } catch (requestError) { setListError(requestError.message) }
  }

  /** Suspende o reactiva un rol usando borrado lógico sin retirar su registro del catálogo. */
  async function toggleRole(role) {
    try {
      const updated = await request(`/roles/${role.id}/status`, { method: 'PATCH', body: JSON.stringify({ active: !role.active }) })
      setRoles(current => current.map(item => item.id === updated.id ? updated : item))
      setListError('')
    } catch (requestError) { setListError(requestError.message) }
  }

  return (
    <main className="role-page">
      <header className="role-nav">
        <Link className="brand" to="/inicio">
          <span className="brand-mark"><Wrench size={20} strokeWidth={2.4} /></span>
          <span>torque<span className="brand-period">.</span><small>TALLER MECÁNICO</small></span>
        </Link>
        <span className="role-user-badge"><UserRound size={15} /> USUARIO AUTENTICADO</span>
      </header>
      <section className="role-content">
        <Link to="/inicio" className="back-link"><ArrowLeft size={15} /> Volver al portal</Link>
        <div className="role-card">
          <span className="form-kicker"><span /> CATÁLOGO DE ROLES</span>
          <h1>Crear un <em>rol.</em></h1>
          <p className="role-description">Registra un nuevo rol en el catálogo del taller.</p>
          <button className="role-list-button" type="button" onClick={openRoles}><ClipboardList size={16}/> Consultar roles creados</button>
          <form className="role-form" onSubmit={submitRole} noValidate>
            <label className="field" htmlFor="role-name">
              <span>Nombre del rol</span>
              <div className="input-wrap">
                <Wrench size={17} />
                <input
                  id="role-name"
                  name="name"
                  type="text"
                  autoComplete="off"
                  placeholder="Ej. Jefe de taller"
                  value={name}
                  onChange={updateName}
                  maxLength={80}
                  pattern={ROLE_NAME_REGEX.source}
                  aria-describedby="role-hint"
                  required
                />
              </div>
              <small id="role-hint" className="role-hint">Letras, espacios, apóstrofes y guiones. Máximo 80 caracteres.</small>
            </label>
            {error && <div className="notice error" role="alert"><CircleHelp size={17} /><span>{error}</span></div>}
            {message && <div className="notice success" role="status"><BadgeCheck size={17} /><span>{message}{createdRole && <small className="role-result">ID: {createdRole.id}<br />Fecha: {new Date(createdRole.fecha).toLocaleString('es-MX')}</small>}</span></div>}
            <button className="submit-button" type="submit" disabled={busy}>
              {busy ? 'Validando y creando...' : 'Crear rol'} <ArrowRight size={17} />
            </button>
          </form>
        </div>
      </section>
      {showRoles && <div className="workshop-modal-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) setShowRoles(false) }}><section className="workshop-modal" role="dialog" aria-modal="true" aria-labelledby="roles-modal-title"><header><div><span className="form-kicker"><span/> CATÁLOGO</span><h2 id="roles-modal-title">Administrar <em>roles.</em></h2></div><button className="workshop-modal-close" type="button" aria-label="Cerrar" onClick={() => setShowRoles(false)}><X size={18}/></button></header>{listError && <div className="notice error" role="alert">{listError}</div>}{roles.length ? <div className="role-list">{roles.map(role => <article className={!role.active ? 'role-suspended' : ''} key={role.id}><span className="role-list-icon"><Wrench size={15}/></span><span className="role-manage-name">{editingRole === role.id ? <input value={editedName} maxLength={80} onChange={event => setEditedName(event.target.value)} aria-label="Nuevo nombre de rol"/> : <b>{role.name}</b>}<small>{new Date(role.fecha).toLocaleString('es-MX')} · {role.active ? 'Activo' : 'Suspendido'}</small></span><div className="role-manage-actions">{editingRole === role.id ? <><button type="button" onClick={() => renameRole(role)}>Guardar</button><button type="button" onClick={() => setEditingRole(null)}>Cancelar</button></> : <button type="button" onClick={() => { setEditingRole(role.id); setEditedName(role.name); setListError('') }}>Renombrar</button>}<button type="button" onClick={() => toggleRole(role)}>{role.active ? 'Suspender' : 'Reactivar'}</button></div><code>{role.id}</code></article>)}</div> : <p className="role-description">Aún no hay roles creados.</p>}<button className="submit-button" type="button" onClick={() => setShowRoles(false)}>Cerrar</button></section></div>}
    </main>
  )
}

