import { useCallback, useEffect, useMemo, useState } from 'react'

const API = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
const token = () => localStorage.getItem('switchboard.token') || ''
async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...options.headers }
  if (token()) headers.Authorization = `Bearer ${token()}`
  const res = await fetch(`${API}${path}`, { ...options, headers })
  const raw = await res.text()
  let body
  try { body = raw ? JSON.parse(raw) : null } catch { body = raw }
  if (!res.ok) throw new Error(body?.message || body?.error || `Request failed (${res.status})`)
  return body
}

const navItems = [['Overview', '▦'], ['Feature flags', '⚑'], ['API keys', '⌘'], ['Activity log', '↗'], ['Settings', '⚙']]
const date = value => value ? new Date(value).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' }) : '—'
const initialUser = () => { try { return JSON.parse(localStorage.getItem('switchboard.user')) } catch { return null } }

export default function App() {
  const [user, setUser] = useState(initialUser)
  const [authMode, setAuthMode] = useState('login')
  const [auth, setAuth] = useState({ name: '', email: '', password: '' })
  const [projects, setProjects] = useState([])
  const [projectId, setProjectId] = useState('')
  const [flags, setFlags] = useState([])
  const [keys, setKeys] = useState([])
  const [logs, setLogs] = useState([])
  const [page, setPage] = useState('Overview')
  const [search, setSearch] = useState('')
  const [modal, setModal] = useState('')
  const [form, setForm] = useState({})
  const [revealedKey, setRevealedKey] = useState('')
  const [error, setError] = useState('')
  const [toast, setToast] = useState('')
  const [busy, setBusy] = useState(false)
  const project = projects.find(p => p.id === projectId)
  const enabled = flags.filter(f => f.isEnabled).length
  const filteredFlags = useMemo(() => flags.filter(f => `${f.name} ${f.flagKey} ${f.description || ''}`.toLowerCase().includes(search.toLowerCase())), [flags, search])
  const notify = message => { setToast(message); setTimeout(() => setToast(''), 2800) }

  const loadProjects = useCallback(async () => {
    const list = await api('/api/projects')
    setProjects(list || [])
    setProjectId(old => list?.some(p => p.id === old) ? old : list?.[0]?.id || '')
  }, [])
  const loadProject = useCallback(async id => {
    if (!id) { setFlags([]); setKeys([]); setLogs([]); return }
    const results = await Promise.allSettled([
      api(`/api/projects/${id}/flags`), api(`/api/projects/${id}/api-keys`), api(`/api/projects/${id}/audit-logs`),
    ])
    setFlags(results[0].status === 'fulfilled' ? results[0].value || [] : [])
    setKeys(results[1].status === 'fulfilled' ? results[1].value || [] : [])
    setLogs(results[2].status === 'fulfilled' ? results[2].value || [] : [])
    const failed = results.find(r => r.status === 'rejected')
    if (failed) setError(failed.reason.message)
  }, [])

  useEffect(() => { if (user) { setBusy(true); setError(''); loadProjects().catch(e => setError(e.message)).finally(() => setBusy(false)) } }, [user, loadProjects])
  useEffect(() => { if (user) loadProject(projectId) }, [user, projectId, loadProject])

  async function signIn(e) {
    e.preventDefault(); setBusy(true); setError('')
    try {
      const result = await api(`/api/auth/${authMode === 'register' ? 'register' : 'login'}`, { method: 'POST', body: JSON.stringify(authMode === 'register' ? auth : { email: auth.email, password: auth.password }) })
      const profile = { name: result.name, email: result.email, userId: result.userId }
      localStorage.setItem('switchboard.token', result.token); localStorage.setItem('switchboard.user', JSON.stringify(profile)); setUser(profile)
    } catch (e2) { setError(e2.message) } finally { setBusy(false) }
  }
  async function signOut() {
    try { await api('/api/auth/logout', { method: 'POST' }) } catch { /* clear the local session if offline */ }
    localStorage.removeItem('switchboard.token'); localStorage.removeItem('switchboard.user'); setUser(null); setProjects([]); setProjectId('')
  }
  function openModal(which, values = {}) { setModal(which); setForm(values); setError('') }
  async function submitModal(e) {
    e.preventDefault(); setBusy(true); setError('')
    try {
      if (modal === 'project') {
        const saved = await api(form.id ? `/api/projects/${form.id}` : '/api/projects', { method: form.id ? 'PUT' : 'POST', body: JSON.stringify({ name: form.name, projectKey: form.projectKey, description: form.description || '' }) })
        await loadProjects(); setProjectId(saved.id); notify(form.id ? 'Project updated' : 'Project created')
      }
      if (modal === 'flag') {
        const saved = await api(`/api/projects/${projectId}/flags`, { method: 'POST', body: JSON.stringify({ name: form.name, flagKey: form.flagKey, description: form.description || '', enabled: Boolean(form.enabled) }) })
        setFlags(old => [saved, ...old]); notify('Flag created')
      }
      if (modal === 'key') {
        const expiresAt = form.expiresAt ? new Date(form.expiresAt).toISOString().slice(0, 19) : null
        const saved = await api(`/api/projects/${projectId}/api-keys`, { method: 'POST', body: JSON.stringify({ name: form.name, expiresAt }) })
        setKeys(old => [{ ...saved, createdAt: new Date().toISOString(), revoked: false }, ...old]); setRevealedKey(saved.apiKey || ''); notify('API key created')
      }
      setModal('')
    } catch (e2) { setError(e2.message) } finally { setBusy(false) }
  }
  async function toggleFlag(flag) {
    try {
      const saved = await api(`/api/projects/${projectId}/flags/${flag.id}/status`, { method: 'PATCH', body: JSON.stringify({ enabled: !flag.isEnabled }) })
      setFlags(old => old.map(f => f.id === flag.id ? saved : f)); notify(`${flag.name} ${saved.isEnabled ? 'enabled' : 'disabled'}`)
    } catch (e) { setError(e.message) }
  }
  async function removeFlag(flag) {
    if (!confirm(`Delete “${flag.name}”? This cannot be undone.`)) return
    try { await api(`/api/projects/${projectId}/flags/${flag.id}`, { method: 'DELETE' }); setFlags(old => old.filter(f => f.id !== flag.id)); notify('Flag deleted') } catch (e) { setError(e.message) }
  }
  async function revokeKey(key) {
    if (!confirm(`Revoke the key “${key.name}”?`)) return
    try { await api(`/api/projects/${projectId}/api-keys/${key.id}`, { method: 'DELETE' }); setKeys(old => old.map(k => k.id === key.id ? { ...k, revoked: true } : k)); notify('API key revoked') } catch (e) { setError(e.message) }
  }

  if (!user) return <div className="auth"><section className="auth-hero"><a className="brand" href="#"><span className="brand-mark"><i/><i/><i/><i/></span>switchboard</a><div className="hero-copy"><span className="eyebrow">FEATURE MANAGEMENT, MADE SIMPLE</span><h1>Ship with<br/><em>confidence.</em></h1><p>Manage feature releases from one calm, clear workspace. Your next great idea is one flag away.</p><div className="preview"><div className="preview-head"><span className="green-dot"/> LIVE FLAGS <b>03 / 08</b></div>{[['✦', 'New checkout', 'checkout_v2', true], ['⌘', 'Team insights', 'team_insights', true], ['◈', 'Smart search', 'smart_search', false]].map(([icon, name, key, on]) => <div className="preview-row" key={key}><span className="preview-symbol">{icon}</span><span><b>{name}</b><small>{key}</small></span><i className={`switch-mini ${on ? 'active' : ''}`}/></div>)}</div></div><div className="hero-foot">A little more control. A lot more momentum.</div></section><section className="auth-main"><div className="auth-box"><span className="eyebrow">YOUR WORKSPACE STARTS HERE</span><h2>{authMode === 'login' ? 'Welcome back' : 'Create your account'}</h2><p className="muted">{authMode === 'login' ? 'Sign in to manage your feature flags.' : 'Get your projects organized in minutes.'}</p><form onSubmit={signIn} className="form-stack">{authMode === 'register' && <label>Your name<input required maxLength="100" placeholder="Alex Morgan" value={auth.name} onChange={e => setAuth({ ...auth, name: e.target.value })}/></label>}<label>Email address<input type="email" required placeholder="you@company.com" value={auth.email} onChange={e => setAuth({ ...auth, email: e.target.value })}/></label><label>Password<input type="password" required minLength={authMode === 'register' ? 8 : undefined} placeholder="••••••••••••" value={auth.password} onChange={e => setAuth({ ...auth, password: e.target.value })}/>{authMode === 'register' && <small>Use at least 8 characters.</small>}</label>{error && <div className="error">{error}</div>}<button className="button primary wide" disabled={busy}>{busy ? 'Please wait…' : authMode === 'login' ? 'Sign in' : 'Create account'} <span>→</span></button></form><div className="auth-switch">{authMode === 'login' ? 'New to Switchboard?' : 'Already have an account?'} <button onClick={() => { setAuthMode(authMode === 'login' ? 'register' : 'login'); setError('') }}>{authMode === 'login' ? 'Create an account' : 'Sign in'}</button></div><div className="api-endpoint"><i/> API endpoint <code>{API}</code></div></div></section></div>

  return <div className="layout"><aside className="sidebar"><a className="brand" href="#"><span className="brand-mark"><i/><i/><i/><i/></span>switchboard</a><div className="side-label">WORKSPACE</div><div className="workspace"><span className="workspace-avatar">{(user.name || user.email || 'W')[0].toUpperCase()}</span><span>{user.name || 'My workspace'}<small>Personal workspace</small></span><b>⌄</b></div><div className="side-label menu-label">MENU</div><nav>{navItems.map(([name, icon]) => <button key={name} onClick={() => { setPage(name); setError('') }} className={`nav-link ${page === name ? 'selected' : ''}`}><span className="nav-icon">{icon}</span>{name}{name === 'Feature flags' && flags.length > 0 && <i>{flags.length}</i>}</button>)}</nav><div className="sidebar-spacer"/><div className="tip-card"><span className="tip-spark">✳</span><b>Ship safely</b><p>Keep releases small. Learn faster. Build better.</p><button onClick={() => setPage('Feature flags')}>Explore flags ↗</button></div><button className="user-row" onClick={signOut}><span className="user-avatar">{(user.name || user.email || 'U')[0].toUpperCase()}</span><span>{user.name}<small>{user.email}</small></span><b title="Sign out">↗</b></button></aside>
    <main className="main"><header className="topbar"><div><span>Workspace</span><i>/</i><b>{page}</b></div><div className="top-right"><span className="connected"><i/> Connected</span><button className="user-avatar top-avatar" onClick={signOut} title="Sign out">{(user.name || user.email || 'U')[0].toUpperCase()}</button></div></header><div className="content"><div className="page-head"><div><span className="eyebrow">{project ? project.name.toUpperCase() : 'YOUR WORKSPACE'}</span><h1>{page === 'Overview' ? 'Good things are shipping.' : page}</h1><p>{page === 'Overview' ? 'Here’s what’s happening with your feature flags.' : page === 'Feature flags' ? 'Control what ships, and when.' : page === 'API keys' ? 'Manage the keys your apps use to connect.' : page === 'Activity log' ? 'A record of changes across this project.' : 'Configure your project and workspace.'}</p></div><div className="head-actions"><label className="project-picker"><span/> <select aria-label="Current project" value={projectId} onChange={e => setProjectId(e.target.value)}><option value="">Select a project</option>{projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}</select><b>⌄</b></label>{page === 'Feature flags' && <button className="button primary" onClick={() => openModal('flag')} disabled={!projectId}>＋ New flag</button>}{page === 'API keys' && <button className="button primary" onClick={() => openModal('key')} disabled={!projectId}>＋ Create key</button>}{(page === 'Overview' || page === 'Settings') && <button className="button subtle" onClick={() => openModal('project')}>＋ New project</button>}</div></div>
      {error && <div className="error-banner"><span>{error}</span><button onClick={() => setError('')}>×</button></div>}
      {!projects.length && !busy ? <section className="empty-project"><div className="empty-art"><i/><i/><i/><i/></div><span className="eyebrow">A FRESH START</span><h2>Start with a project</h2><p>Projects keep your flags, API keys, and activity neatly in one place.</p><button className="button primary" onClick={() => openModal('project')}>＋ Create your first project</button></section> : !projectId ? <div className="empty-project"><p>Select a project to get started.</p></div> : <>
      {(page === 'Overview' || page === 'Settings') && <><section className="stats"><article><div className="stat-title">TOTAL FLAGS <span className="stat-icon purple">✳</span></div><strong>{String(flags.length).padStart(2, '0')}</strong><small>Across this project</small></article><article><div className="stat-title">LIVE RIGHT NOW <span className="stat-icon green">↗</span></div><strong>{String(enabled).padStart(2, '0')}<small className="denom"> / {String(flags.length).padStart(2, '0')}</small></strong><small><em>{flags.length ? Math.round(enabled / flags.length * 100) : 0}%</em> of flags are enabled</small></article><article><div className="stat-title">API KEYS <span className="stat-icon orange">⌘</span></div><strong>{String(keys.filter(k => !k.revoked).length).padStart(2, '0')}</strong><small>Active credentials</small></article><article><div className="stat-title">RECENT CHANGES <span className="stat-icon blue">⌁</span></div><strong>{String(logs.length).padStart(2, '0')}</strong><small>Entries in activity log</small></article></section><section className="dash-grid"><article className="panel"><div className="panel-head"><div><h2>Feature flags</h2><p>Your project’s latest switches</p></div><button className="link-button" onClick={() => setPage('Feature flags')}>View all ↗</button></div>{flags.length ? <div>{flags.slice(0, 5).map(f => <div className="mini-flag" key={f.id}><span className="flag-badge">{f.name[0]?.toUpperCase()}</span><span className="flag-detail"><b>{f.name}</b><code>{f.flagKey}</code></span><span className={`pill ${f.isEnabled ? 'enabled' : ''}`}>{f.isEnabled ? 'Enabled' : 'Disabled'}</span><button className={`toggle ${f.isEnabled ? 'on' : ''}`} onClick={() => toggleFlag(f)} aria-label="Toggle flag"><i/></button></div>)}</div> : <p className="empty-text">No flags yet. Add one to start a controlled rollout.</p>}<button className="add-row" onClick={() => openModal('flag')}>＋ Add a feature flag</button></article><article className="panel"><div className="panel-head"><div><h2>Recent activity</h2><p>Latest changes to this project</p></div><button className="link-button" onClick={() => setPage('Activity log')}>See log ↗</button></div>{logs.length ? logs.slice(0, 6).map(log => <div className="activity-row" key={log.id}><span className="activity-icon">↗</span><span><b>{(log.action || 'Updated').replaceAll('_', ' ').toLowerCase()}</b><small>{log.featureFlagId ? `Flag · ${String(log.featureFlagId).slice(0, 8)}` : 'Project activity'}</small></span><time>{date(log.createdAt)}</time></div>) : <p className="empty-text">Changes will show up here as your team works.</p>}</article></section><section className="quickstart"><span className="quick-icon">✳</span><div><span className="eyebrow">UP AND RUNNING</span><h2>Connect your application</h2><p>Evaluate feature flags in your app with one endpoint.</p></div><code>{API}/api/evaluate/{projectId.slice(0, 8)}…/flag_key</code><button className="button subtle" onClick={() => { navigator.clipboard?.writeText(`${API}/api/evaluate/${projectId}/{flagKey}`); notify('Endpoint copied') }}>Copy endpoint</button></section></>}
      {page === 'Feature flags' && <section className="panel table-panel"><div className="table-heading"><div><h2>All flags <span className="count">{flags.length}</span></h2><p>Toggle a flag to change its live state instantly.</p></div><label className="search"><span>⌕</span><input placeholder="Search flags…" value={search} onChange={e => setSearch(e.target.value)}/><kbd>⌘ K</kbd></label></div><div className="table-scroll"><table><thead><tr><th>FLAG</th><th>KEY</th><th>STATUS</th><th>LAST UPDATED</th><th/></tr></thead><tbody>{filteredFlags.map(f => <tr key={f.id}><td><div className="flag-cell"><span className="flag-badge">{f.name[0]?.toUpperCase()}</span><span><b>{f.name}</b><small>{f.description || 'No description'}</small></span></div></td><td><code>{f.flagKey}</code></td><td><span className={`pill ${f.isEnabled ? 'enabled' : ''}`}>{f.isEnabled ? 'Enabled' : 'Disabled'}</span></td><td>{date(f.updatedAt)}</td><td className="row-controls"><button className={`toggle ${f.isEnabled ? 'on' : ''}`} onClick={() => toggleFlag(f)} aria-label="Toggle flag"><i/></button><button className="icon-button" onClick={() => removeFlag(f)} title="Delete flag">×</button></td></tr>)}</tbody></table>{!filteredFlags.length && <div className="table-empty">{search ? 'No flags match your search.' : 'No flags yet. Create one to get started.'}</div>}</div></section>}
      {page === 'API keys' && <section className="panel table-panel"><div className="table-heading"><div><h2>Project API keys <span className="count">{keys.length}</span></h2><p>Keys authenticate requests from your project integrations.</p></div></div><div className="table-scroll"><table><thead><tr><th>NAME</th><th>KEY PREFIX</th><th>CREATED</th><th>EXPIRES</th><th>STATUS</th><th/></tr></thead><tbody>{keys.map(k => <tr key={k.id}><td><b>{k.name}</b></td><td><code>{k.keyPrefix}••••••••</code></td><td>{date(k.createdAt)}</td><td>{k.expiresAt ? date(k.expiresAt) : 'Never'}</td><td><span className={`pill ${k.revoked ? '' : 'enabled'}`}>{k.revoked ? 'Revoked' : 'Active'}</span></td><td>{!k.revoked && <button className="button small danger" onClick={() => revokeKey(k)}>Revoke</button>}</td></tr>)}</tbody></table>{!keys.length && <div className="table-empty">No API keys yet. Create one to connect an application.</div>}</div><p className="key-note">ⓘ New keys are shown only once. Store them somewhere safe.</p></section>}
      {page === 'Activity log' && <section className="panel table-panel"><div className="table-heading"><div><h2>Project activity <span className="count">{logs.length}</span></h2><p>Audit trail for changes to this project.</p></div></div><div className="activity-list">{logs.map(log => <div className="activity-entry" key={log.id}><span className="activity-icon">↗</span><span><b>{(log.action || 'Updated').replaceAll('_', ' ').toLowerCase()}</b><small>{log.featureFlagId ? `Feature flag · ${log.featureFlagId}` : 'Project change'}{log.userId ? ` · User ${String(log.userId).slice(0, 8)}` : ''}</small></span><time>{log.createdAt ? new Date(log.createdAt).toLocaleString() : '—'}</time></div>)}{!logs.length && <div className="table-empty">No activity recorded yet.</div>}</div></section>}
      {page === 'Settings' && <section className="settings-grid"><article className="panel settings-card"><span className="eyebrow">PROJECT DETAILS</span><h2>{project?.name}</h2><p>{project?.description || 'No project description yet.'}</p><div className="setting"><span>Project key</span><code>{project?.projectKey}</code></div><div className="setting"><span>Created</span><span>{date(project?.createdAt)}</span></div><button className="button subtle" onClick={() => openModal('project', project)}>Edit project details</button></article><article className="panel settings-card"><span className="eyebrow">WORKSPACE</span><h2>Your account</h2><p>Signed in to Switchboard as</p><div className="account-chip"><span className="user-avatar">{(user.name || user.email || 'U')[0].toUpperCase()}</span><span><b>{user.name}</b><small>{user.email}</small></span></div><button className="button subtle danger" onClick={signOut}>Sign out</button></article></section>}
      </>}
      <footer className="footer"><span>Switchboard <i>·</i> Feature management for teams</span><span><i/> All systems operational</span></footer>
    </div></main>
    {modal && <div className="backdrop" onMouseDown={e => { if (e.target === e.currentTarget) setModal('') }}><form className="modal" onSubmit={submitModal}><div className="modal-head"><div><span className="eyebrow">{modal === 'flag' ? 'FEATURE MANAGEMENT' : modal === 'key' ? 'PROJECT ACCESS' : 'WORKSPACE'}</span><h2>{modal === 'flag' ? 'Create a flag' : modal === 'key' ? 'Create an API key' : form.id ? 'Edit project' : 'Create a project'}</h2></div><button type="button" className="icon-button" onClick={() => setModal('')}>×</button></div>
      {modal === 'project' && <><label>Project name<input required maxLength="150" placeholder="e.g. Customer portal" value={form.name || ''} onChange={e => setForm({ ...form, name: e.target.value })}/></label><label>Project key<input required maxLength="100" placeholder="customer-portal" value={form.projectKey || ''} onChange={e => setForm({ ...form, projectKey: e.target.value })}/><small>Used to identify this project in your workspace.</small></label><label>Description <i>Optional</i><textarea rows="3" placeholder="What is this project for?" value={form.description || ''} onChange={e => setForm({ ...form, description: e.target.value })}/></label></>}
      {modal === 'flag' && <><label>Flag name<input required maxLength="150" placeholder="e.g. New checkout" value={form.name || ''} onChange={e => setForm({ ...form, name: e.target.value })}/></label><label>Flag key<input required maxLength="150" placeholder="new_checkout" value={form.flagKey || ''} onChange={e => setForm({ ...form, flagKey: e.target.value })}/><small>Use this key in your application code.</small></label><label>Description <i>Optional</i><textarea rows="3" placeholder="What does this flag control?" value={form.description || ''} onChange={e => setForm({ ...form, description: e.target.value })}/></label><label className="check-label"><input type="checkbox" checked={Boolean(form.enabled)} onChange={e => setForm({ ...form, enabled: e.target.checked })}/> Enable this flag immediately</label></>}
      {modal === 'key' && <><label>Key name<input required placeholder="e.g. Production app" value={form.name || ''} onChange={e => setForm({ ...form, name: e.target.value })}/></label><label>Expiration date <i>Optional</i><input type="datetime-local" value={form.expiresAt || ''} onChange={e => setForm({ ...form, expiresAt: e.target.value })}/><small>You can revoke this key at any time.</small></label></>}
      {error && <div className="error">{error}</div>}<div className="modal-actions"><button type="button" className="button subtle" onClick={() => setModal('')}>Cancel</button><button className="button primary" disabled={busy}>{busy ? 'Saving…' : modal === 'flag' ? 'Create flag' : modal === 'key' ? 'Create API key' : form.id ? 'Save changes' : 'Create project'}</button></div></form></div>}
    {revealedKey && <div className="backdrop"><section className="modal"><div className="modal-head"><div><span className="eyebrow">COPY IT NOW</span><h2>Your API key</h2></div><button className="icon-button" onClick={() => setRevealedKey('')}>×</button></div><p>This key will only be shown once. Copy and store it securely.</p><div className="revealed-key"><code>{revealedKey}</code><button className="button subtle" onClick={() => { navigator.clipboard?.writeText(revealedKey); notify('Key copied'); setRevealedKey('') }}>Copy key</button></div><button className="button primary wide" onClick={() => setRevealedKey('')}>I’ve saved it</button></section></div>}
    {toast && <div className="toast">✓ &nbsp;{toast}</div>}
  </div>
}
