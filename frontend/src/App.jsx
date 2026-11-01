import { useCallback, useEffect, useMemo, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { Elements, PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js'
import { loadStripe } from '@stripe/stripe-js'
import { api, authCallbackToken, patch, post } from './api.js'

const blankData = { rides: [], offers: [], payments: [], tickets: [], users: [], promos: [], metrics: null }
const money = (amount, currency = 'INR') => amount == null ? '—' : new Intl.NumberFormat('en-IN', { style: 'currency', currency }).format(amount)
const date = (value) => value ? new Date(value).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' }) : '—'
const activeStatus = (status) => ['REQUESTED', 'ACCEPTED', 'IN_PROGRESS'].includes(status)

export default function App() {
  const [token, setToken] = useState(() => authCallbackToken() || localStorage.getItem('cabline-token'))
  const [user, setUser] = useState(null)
  const [data, setData] = useState(blankData)
  const [view, setView] = useState('home')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState('')
  const [error, setError] = useState('')
  const [authError, setAuthError] = useState(new URLSearchParams(window.location.search).get('oauthError') ? 'Google sign-in could not be completed.' : '')

  const refresh = useCallback(async () => {
    if (!token) return
    const account = await api('/api/auth/me', {}, token)
    setUser(account)
    const calls = [account.role === 'ADMIN' ? Promise.resolve([]) : api('/api/rides/me', {}, token), api('/api/support/tickets/me', {}, token)]
    if (account.role === 'RIDER') calls.push(api('/api/payments/me', {}, token))
    if (account.role === 'DRIVER') calls.push(api('/api/drivers/me/offers', {}, token))
    if (account.role === 'ADMIN') calls.push(
      api('/api/admin/metrics', {}, token), api('/api/admin/users', {}, token),
      api('/api/admin/rides', {}, token), api('/api/admin/support/tickets', {}, token),
      api('/api/admin/promos', {}, token),
    )
    const result = await Promise.all(calls)
    const next = { ...blankData, rides: result[0] || [], tickets: result[1] || [] }
    if (account.role === 'RIDER') next.payments = result[2] || []
    if (account.role === 'DRIVER') next.offers = result[2] || []
    if (account.role === 'ADMIN') {
      ;[next.metrics, next.users, next.rides, next.tickets, next.promos] = result.slice(2)
    }
    setData(next)
    return next
  }, [token])

  useEffect(() => {
    if (!token) { setUser(null); setData(blankData); return }
    refresh().catch(() => {
      localStorage.removeItem('cabline-token')
      setToken(null)
      setUser(null)
      setAuthError('Your session ended. Please sign in again.')
    })
  }, [token, refresh])

  const activeRideIds = data.rides.filter((ride) => activeStatus(ride.status)).map((ride) => ride.id).join(',')
  useEffect(() => {
    if (!token || !user || (!activeRideIds && user.role !== 'DRIVER')) return
    const configuredApi = import.meta.env.VITE_API_BASE_URL
    const wsOrigin = configuredApi ? new URL(configuredApi).origin : window.location.origin
    const client = new Client({
      brokerURL: `${wsOrigin.replace(/^http/, 'ws')}/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      onConnect: () => activeRideIds.split(',').filter(Boolean).forEach((rideId) => {
        client.subscribe(`/topic/rides/${rideId}`, () => refresh().catch(() => {}))
      }),
    })
    client.activate()
    const poll = window.setInterval(() => refresh().catch(() => {}), user.role === 'DRIVER' ? 7000 : 15000)
    return () => { window.clearInterval(poll); client.deactivate() }
  }, [token, user?.role, activeRideIds, refresh])

  const pendingPaymentIds = data.payments.filter((payment) => payment.status === 'PENDING').map((payment) => payment.id).join(',')
  useEffect(() => {
    if (!token || user?.role !== 'RIDER' || !pendingPaymentIds) return
    const poll = window.setInterval(() => refresh().catch(() => {}), 5000)
    return () => window.clearInterval(poll)
  }, [token, user?.role, pendingPaymentIds, refresh])

  const act = useCallback(async (task, success = 'Saved.') => {
    setBusy(true); setError(''); setNotice('')
    try {
      const result = await task()
      await refresh()
      setNotice(success)
      return { ok: true, value: result }
    } catch (exception) {
      setError(exception.message || 'Something went wrong. Please try again.')
      return { ok: false }
    } finally { setBusy(false) }
  }, [refresh])

  const signIn = async (payload, registering) => {
    setBusy(true); setAuthError('')
    try {
      const result = await post(registering ? '/api/auth/register' : '/api/auth/login', payload, null)
      localStorage.setItem('cabline-token', result.accessToken)
      setToken(result.accessToken)
    } catch (exception) { setAuthError(exception.message || 'Sign in failed.') }
    finally { setBusy(false) }
  }

  const logout = () => {
    localStorage.removeItem('cabline-token'); setToken(null); setUser(null); setData(blankData); setView('home')
  }

  if (!token || !user) return <AuthScreen onSubmit={signIn} busy={busy} error={authError} />

  return <div className="app-shell">
    <aside className="sidebar">
      <a className="brand" href="#home" onClick={(event) => { event.preventDefault(); setView('home') }}>
        <span className="brand-mark">c</span><span>cabline<span className="brand-dot">.</span></span>
      </a>
      <div className="workspace-label">WORKSPACE</div>
      <nav className="side-nav" aria-label="Main navigation">
        <button className={view === 'home' ? 'selected' : ''} onClick={() => setView('home')}><Glyph name="grid" />Overview</button>
        {user.role !== 'ADMIN' && <button className={view === 'history' ? 'selected' : ''} onClick={() => setView('history')}><Glyph name="clock" />Ride history</button>}
        {user.role !== 'ADMIN' && <button className={view === 'support' ? 'selected' : ''} onClick={() => setView('support')}><Glyph name="help" />Help & support</button>}
      </nav>
      <div className="sidebar-bottom">
        <div className="profile-chip"><div className="avatar">{user.name?.slice(0, 1).toUpperCase()}</div><div className="profile-copy"><strong>{user.name}</strong><span>{user.role.toLowerCase()}</span></div><button className="icon-button logout" title="Sign out" onClick={logout}><Glyph name="logout" /></button></div>
        <div className="sidebar-foot">A smoother way to get there.</div>
      </div>
    </aside>
    <main className="main-content">
      <header className="topbar"><div className="breadcrumb">Cabline <span>/</span> {view === 'home' ? (user.role === 'ADMIN' ? 'Admin overview' : 'Overview') : view === 'history' ? 'Ride history' : 'Help & support'}</div><div className="topbar-right"><span className="live-pill"><i />Service online</span><div className="avatar small">{user.name?.slice(0, 1).toUpperCase()}</div></div></header>
      <div className="content-wrap">
        {notice && <div className="toast success"><span>✓</span>{notice}<button onClick={() => setNotice('')}>×</button></div>}
        {error && <div className="toast error"><span>!</span>{error}<button onClick={() => setError('')}>×</button></div>}
        {view === 'support' ? <SupportView data={data} run={act} /> : view === 'history' ? <HistoryView user={user} data={data} run={act} /> : user.role === 'ADMIN' ? <AdminView data={data} run={act} /> : user.role === 'DRIVER' ? <DriverHome user={user} data={data} run={act} busy={busy} /> : <RiderHome user={user} data={data} run={act} busy={busy} />}
      </div>
    </main>
  </div>
}

function AuthScreen({ onSubmit, busy, error }) {
  const [registering, setRegistering] = useState(false)
  const [role, setRole] = useState('RIDER')
  const oauthEnabled = import.meta.env.VITE_GOOGLE_OAUTH_ENABLED === 'true'
  const submit = (event) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    const payload = registering
      ? { name: form.get('name'), email: form.get('email'), password: form.get('password'), role }
      : { email: form.get('email'), password: form.get('password') }
    onSubmit(payload, registering)
  }
  return <div className="auth-page">
    <section className="auth-left">
      <a className="brand auth-brand" href="#"><span className="brand-mark">c</span><span>cabline<span className="brand-dot">.</span></span></a>
      <div className="auth-copy"><span className="eyebrow light">MOVE THROUGH YOUR DAY</span><h1>Your city.<br /><em>Your pace.</em></h1><p>Reliable rides, transparent fares, and a little more room in your day.</p>
        <div className="auth-proof"><span className="proof-icon"><Glyph name="spark" /></span><span><strong>Thoughtful from pickup to drop-off</strong><small>Made for the way you move.</small></span></div>
      </div>
      <div className="auth-visual"><div className="route-line"><i /><i /><i /></div><div className="map-pin pin-one">●</div><div className="map-pin pin-two">●</div><div className="map-label label-one">Your pickup</div><div className="map-label label-two">On your way</div><div className="mini-car">↗</div></div>
      <div className="auth-footer">© 2026 Cabline Mobility</div>
    </section>
    <section className="auth-right">
      <div className="auth-form-wrap">
        <span className="eyebrow">WELCOME {registering ? 'ABOARD' : 'BACK'}</span>
        <h2>{registering ? 'Create your account' : 'Sign in to Cabline'}</h2>
        <p className="muted">{registering ? 'A better ride is just around the corner.' : 'Your next ride is closer than you think.'}</p>
        <div className="auth-tabs"><button className={!registering ? 'active' : ''} onClick={() => setRegistering(false)}>Sign in</button><button className={registering ? 'active' : ''} onClick={() => setRegistering(true)}>Create account</button></div>
        <form className="form-stack" onSubmit={submit}>
          {registering && <label>Full name<input name="name" autoComplete="name" placeholder="Jordan Lee" required maxLength="100" /></label>}
          <label>Email address<input name="email" type="email" autoComplete="email" placeholder="you@example.com" required maxLength="254" /></label>
          <label>Password<input name="password" type="password" autoComplete={registering ? 'new-password' : 'current-password'} placeholder="At least 10 characters" required minLength={registering ? 10 : 1} maxLength="72" /></label>
          {registering && <label>Account type<select value={role} onChange={(event) => setRole(event.target.value)}><option value="RIDER">I need rides</option><option value="DRIVER">I drive</option></select></label>}
          {error && <div className="inline-error">{error}</div>}
          <button className="button primary full" disabled={busy}>{busy ? 'Please wait…' : registering ? 'Create account' : 'Sign in'}<Glyph name="arrow" /></button>
        </form>
        {!registering && oauthEnabled && <><div className="divider"><span>or continue with</span></div><a className="button google full" href={`${import.meta.env.VITE_API_BASE_URL || ''}/oauth2/authorization/google`}><GoogleMark />Continue with Google</a></>}
        <p className="auth-terms">By continuing, you agree to our <a href="#terms">Terms of Service</a> and <a href="#privacy">Privacy Policy</a>.</p>
      </div>
    </section>
  </div>
}

function RiderHome({ user, data, run, busy }) {
  return <>
    <section className="page-heading"><div><span className="eyebrow">YOUR NEXT JOURNEY</span><h1>Good day, {user.name?.split(' ')[0]} <span className="wave">✳</span></h1><p>Where would you like to go?</p></div><div className="heading-meta"><div className="meta-icon"><Glyph name="shield" /></div><span><strong>Ride with peace of mind</strong><small>Your trip details stay private</small></span></div></section>
    <div className="rider-grid"><BookingCard run={run} busy={busy} /><section className="side-stack"><QuickStat label="TOTAL RIDES" value={data.rides.length} icon="route" /><RecentRides rides={data.rides.slice(0, 3)} /></section></div>
    <PaymentsPanel payments={data.payments} run={run} />
  </>
}

function BookingCard({ run, busy }) {
  const [quote, setQuote] = useState(null)
  const [quoting, setQuoting] = useState(false)
  const [quoteError, setQuoteError] = useState('')
  const [coords, setCoords] = useState(null)
  const [locationMessage, setLocationMessage] = useState('')
  const [form, setForm] = useState({ pickup: '', dropoff: '', distanceKm: '', promoCode: '' })
  const update = (field) => (event) => { setForm((current) => ({ ...current, [field]: event.target.value })); setQuote(null) }
  const requestBody = () => ({ pickup: form.pickup.trim(), dropoff: form.dropoff.trim(), ...(form.distanceKm ? { distanceKm: Number(form.distanceKm) } : {}), ...(coords || {}), ...(form.promoCode.trim() ? { promoCode: form.promoCode.trim() } : {}) })
  const estimate = async (event) => {
    event.preventDefault(); setQuoting(true); setQuoteError(''); setQuote(null)
    try { setQuote(await post('/api/routes/estimate', requestBody())) }
    catch (exception) { setQuoteError(exception.message) }
    finally { setQuoting(false) }
  }
  const book = () => run(() => post('/api/rides', requestBody()), 'Ride requested. We’re finding your driver.').then((result) => { if (result?.ok) setQuote(null) })
  const locate = () => {
    if (!navigator.geolocation) { setLocationMessage('Location is not available in this browser.'); return }
    navigator.geolocation.getCurrentPosition((position) => {
      setCoords({ pickupLatitude: position.coords.latitude, pickupLongitude: position.coords.longitude })
      setQuote(null)
      setLocationMessage('Pickup location added for nearby driver matching.')
    }, () => setLocationMessage('Location access was skipped. You can still request your ride.'), { timeout: 6000 })
  }
  return <section className="card booking-card">
    <div className="card-heading"><div><span className="eyebrow">BOOK A RIDE</span><h2>Let’s get you there</h2></div><div className="heading-icon yellow"><Glyph name="route" /></div></div>
    <form onSubmit={estimate} className="booking-form">
      <div className="location-fields"><div className="location-track"><span className="track-dot start" /><span className="track-dash" /><span className="track-dot end" /></div><div className="fields"><label>Pickup location<input value={form.pickup} onChange={update('pickup')} placeholder="Enter pickup address" required maxLength="200" /></label><label>Drop-off location<input value={form.dropoff} onChange={update('dropoff')} placeholder="Where are you headed?" required maxLength="200" /></label></div></div>
      <div className="booking-extras"><label className="compact-label">Promo code<input value={form.promoCode} onChange={update('promoCode')} placeholder="Add a code" maxLength="30" /></label><label className="compact-label">Distance (km)<input type="number" min="0.1" max="500" step="0.1" value={form.distanceKm} onChange={update('distanceKm')} placeholder="Maps fills this in" /></label></div>
      <div className="location-note"><button type="button" className="text-button" onClick={locate}><Glyph name="pin" />Use my pickup location</button><span>{locationMessage || 'Helps match you with a nearby driver'}</span></div>
      {quoteError && <div className="inline-error">{quoteError}</div>}
      {!quote ? <button className="button primary full" disabled={busy || quoting}>{quoting ? 'Checking route…' : 'See fare estimate'}<Glyph name="arrow" /></button> : <div className="quote-box"><div className="quote-main"><div><small>{quote.liveRoute ? 'GOOGLE MAPS ROUTE' : 'FARE ESTIMATE'}</small><strong>{money(quote.estimatedFare, quote.currency)}</strong><span>{quote.distanceKm} km {quote.durationSeconds ? `· ${Math.ceil(quote.durationSeconds / 60)} min` : '· estimated distance'}</span></div><div className="quote-check">✓</div></div>{quote.discountAmount > 0 && <div className="discount-line"><span>{quote.promoCode} applied</span><strong>− {money(quote.discountAmount, quote.currency)}</strong></div>}<div className="quote-actions"><button type="button" className="button secondary" onClick={() => setQuote(null)}>Edit trip</button><button type="button" className="button primary" disabled={busy} onClick={book}>Confirm ride<Glyph name="arrow" /></button></div></div>}
    </form>
  </section>
}

function QuickStat({ label, value, icon }) {
  return <div className="card quick-stat"><div className="stat-top"><span>{label}</span><span className="stat-icon"><Glyph name={icon} /></span></div><strong>{value}</strong><small>Since you joined Cabline</small></div>
}

function RecentRides({ rides }) {
  return <div className="card recent-card"><div className="section-title"><div><span className="eyebrow">RECENT ACTIVITY</span><h3>Latest rides</h3></div><span className="count-badge">{rides.length}</span></div>{rides.length ? rides.map((ride) => <RideRow key={ride.id} ride={ride} />) : <EmptyState title="Your next ride starts here" text="Booked trips will show up in this space." />}</div>
}

function RideRow({ ride }) {
  return <div className="ride-row"><div className="ride-route-icon"><span /></div><div className="ride-row-copy"><strong>{ride.dropoff}</strong><small>{ride.pickup} · {date(ride.createdAt)}</small></div><StatusPill status={ride.status} /></div>
}

function DriverHome({ user, data, run, busy }) {
  const [available, setAvailable] = useState(false)
  useEffect(() => { api('/api/drivers/me/availability').then((value) => setAvailable(value.available)).catch(() => {}) }, [])
  const setAvailability = (next) => {
    const send = (coords) => run(() => patch('/api/drivers/me/availability', { available: next, ...(coords || {}) }), next ? 'You’re online and ready for trip requests.' : 'You are now offline.').then((result) => { if (result?.ok) setAvailable(next) })
    if (next && navigator.geolocation) navigator.geolocation.getCurrentPosition((position) => send({ latitude: position.coords.latitude, longitude: position.coords.longitude }), () => send(null), { timeout: 5000 })
    else send(null)
  }
  const respond = (offer, accept) => run(() => post(`/api/driver-offers/${offer.offerId}/${accept ? 'accept' : 'decline'}`, {}), accept ? 'Ride accepted.' : 'Offer declined.')
  const transition = (ride, action) => run(() => post(`/api/rides/${ride.id}/${action}`, {}), action === 'start' ? 'Ride marked as in progress.' : 'Ride completed.')
  return <>
    <section className="page-heading"><div><span className="eyebrow">DRIVER SPACE</span><h1>Good day, {user.name?.split(' ')[0]}</h1><p>Your next trip is just around the corner.</p></div><div className={`availability ${available ? 'online' : ''}`}><span className="online-dot" /><span><strong>{available ? 'You’re online' : 'You’re offline'}</strong><small>{available ? 'Available for ride requests' : 'Not receiving requests'}</small></span><button className={`toggle ${available ? 'on' : ''}`} aria-label="Toggle availability" onClick={() => setAvailability(!available)} disabled={busy}><i /></button></div></section>
    <div className="driver-grid"><section className="card"><div className="section-title"><div><span className="eyebrow">RIDE REQUESTS</span><h2>Offers near you</h2></div><span className="count-badge">{data.offers.length}</span></div>{data.offers.length ? data.offers.map((offer) => <div className="offer-card" key={offer.offerId}><div className="offer-route"><span className="mini-route"><i /><i /></span><div><strong>{offer.pickup}</strong><small>{offer.dropoff}</small></div></div><div className="offer-details"><span>{offer.distanceKm} km</span><strong>{money(offer.estimatedFare, offer.currency)}</strong></div><div className="offer-actions"><button className="button secondary" onClick={() => respond(offer, false)} disabled={busy}>Decline</button><button className="button primary" onClick={() => respond(offer, true)} disabled={busy}>Accept ride<Glyph name="arrow" /></button></div></div>) : <EmptyState title="No new offers" text="Stay online and we’ll let you know when a ride is nearby." />}</section>
      <section className="card driver-activity"><span className="eyebrow">YOUR DAY</span><h2>Trip overview</h2><div className="driver-metric"><strong>{data.rides.filter((ride) => ride.status === 'COMPLETED').length}</strong><span>completed trips</span></div><div className="divider-line" /><div className="driver-metric"><strong>{data.rides.filter((ride) => activeStatus(ride.status)).length}</strong><span>active trips</span></div><p>Keep your status up to date so riders know when you’re available.</p></section></div>
    <section className="card table-card"><div className="section-title"><div><span className="eyebrow">ASSIGNED TO YOU</span><h2>Active trips</h2></div></div><RideTable rides={data.rides} role="DRIVER" run={run} busy={busy} /></section>
  </>
}

function RideTable({ rides, role, run, busy }) {
  if (!rides.length) return <EmptyState title="No trips to show" text="Your ride activity will appear here." />
  return <div className="ride-list">{rides.map((ride) => <article className="ride-card" key={ride.id}><div className="ride-card-main"><div className="ride-route-icon"><span /></div><div><div className="ride-card-title">{ride.pickup} <span>→</span> {ride.dropoff}</div><small>{date(ride.createdAt)} · {ride.distanceKm} km · {money(ride.estimatedFare, ride.currency)}</small></div></div><StatusPill status={ride.status} /><div className="ride-card-actions">{role === 'RIDER' && ['REQUESTED', 'ACCEPTED'].includes(ride.status) && <button className="button danger-outline small-button" onClick={() => run(() => post(`/api/rides/${ride.id}/cancel`, {}), 'Ride cancelled.')} disabled={busy}>Cancel</button>}{role === 'DRIVER' && ride.status === 'ACCEPTED' && <button className="button primary small-button" onClick={() => run(() => post(`/api/rides/${ride.id}/start`, {}), 'Trip started.')} disabled={busy}>Start trip</button>}{role === 'DRIVER' && ride.status === 'IN_PROGRESS' && <button className="button primary small-button" onClick={() => run(() => post(`/api/rides/${ride.id}/complete`, {}), 'Trip complete.')} disabled={busy}>Complete</button>}{role === 'RIDER' && ride.status === 'COMPLETED' && <RatingForm ride={ride} run={run} busy={busy} />}</div></article>)}</div>
}

function RatingForm({ ride, run, busy }) {
  const [open, setOpen] = useState(false)
  const [stars, setStars] = useState(5)
  const [comment, setComment] = useState('')
  if (!open) return <button className="button secondary small-button" onClick={() => setOpen(true)}>Rate ride</button>
  return <form className="rating-inline" onSubmit={(event) => { event.preventDefault(); run(() => post(`/api/rides/${ride.id}/rating`, { stars, comment }), 'Thanks for sharing your feedback.').then((result) => { if (result?.ok) setOpen(false) }) }}><select value={stars} onChange={(event) => setStars(Number(event.target.value))} aria-label="Rating"><option value="5">★★★★★ — Great</option><option value="4">★★★★ — Good</option><option value="3">★★★ — Okay</option><option value="2">★★ — Poor</option><option value="1">★ — Bad</option></select><input value={comment} maxLength="500" onChange={(event) => setComment(event.target.value)} placeholder="Optional note" /><button className="button primary small-button" disabled={busy}>Send</button></form>
}

function PaymentsPanel({ payments, run }) {
  if (!payments.length) return null
  return <section className="card table-card payments-table"><div className="section-title"><div><span className="eyebrow">PAYMENTS</span><h2>Receipts & payment status</h2></div></div><div className="payment-list">{payments.map((payment) => <PaymentCard key={payment.id} payment={payment} run={run} />)}</div></section>
}

function PaymentCard({ payment, run }) {
  const [checkout, setCheckout] = useState(null)
  const [opening, setOpening] = useState(false)
  useEffect(() => { if (payment.status === 'PAID') setCheckout(null) }, [payment.status])
  const openCheckout = async () => {
    setOpening(true)
    try {
      const result = await api(`/api/payments/${payment.id}/checkout`, { method: 'POST' })
      setCheckout(result.mode === 'PAID' ? null : result)
      return result
    }
    catch (exception) { throw exception }
    finally { setOpening(false) }
  }
  const downloadReceipt = async () => {
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || ''}${payment.receiptUrl}`, { headers: { Authorization: `Bearer ${localStorage.getItem('cabline-token')}` } })
    if (!response.ok) throw new Error('Receipt could not be downloaded.')
    const url = URL.createObjectURL(await response.blob())
    const link = document.createElement('a'); link.href = url; link.download = `cabline-receipt-${payment.id}.pdf`; link.click(); URL.revokeObjectURL(url)
  }
  return <div className="payment-card"><div className="payment-info"><div className="payment-icon"><Glyph name="card" /></div><div><strong>{money(payment.amount, payment.currency)}</strong><small>{date(payment.createdAt)} · Ride {payment.rideId.slice(0, 8)}</small></div></div><StatusPill status={payment.status} /><div className="payment-actions">{payment.status === 'PAID' ? <button className="button secondary small-button" onClick={() => run(downloadReceipt, 'Receipt downloaded.')}>Download receipt ↓</button> : <button className="button primary small-button" disabled={opening} onClick={() => run(openCheckout, 'Checkout is ready.')}>{opening ? 'Loading…' : 'Pay now'}</button>}</div>{checkout?.mode === 'DEMO' && <div className="checkout-box"><div><strong>Demo checkout</strong><small>Stripe is not configured; confirm this sample payment.</small></div><button className="button primary small-button" onClick={() => run(() => post(`/api/payments/${payment.id}/confirm-demo`, {}), 'Payment complete. Your receipt is ready.').then((result) => { if (result?.ok) setCheckout(null) })}>Confirm demo payment</button></div>}{checkout?.mode === 'STRIPE' && <StripeCheckout checkout={checkout} onComplete={() => { setCheckout(null); run(() => Promise.resolve(), 'Payment submitted. Your receipt will be ready when Stripe confirms it.') }} />}</div>
}

function StripeCheckout({ checkout, onComplete }) {
  const stripePromise = useMemo(() => loadStripe(checkout.publishableKey), [checkout.publishableKey])
  return <div className="stripe-box"><Elements stripe={stripePromise} options={{ clientSecret: checkout.clientSecret, appearance: { theme: 'stripe', variables: { colorPrimary: '#156b52', borderRadius: '8px', fontFamily: 'DM Sans, sans-serif' } } }}><StripePaymentForm onComplete={onComplete} /></Elements></div>
}

function StripePaymentForm({ onComplete }) {
  const stripe = useStripe()
  const elements = useElements()
  const [processing, setProcessing] = useState(false)
  const [message, setMessage] = useState('')
  const submit = async (event) => {
    event.preventDefault(); if (!stripe || !elements) return
    setProcessing(true); setMessage('')
    const result = await stripe.confirmPayment({ elements, redirect: 'if_required' })
    if (result.error) setMessage(result.error.message || 'Payment could not be confirmed.')
    else onComplete()
    setProcessing(false)
  }
  return <form onSubmit={submit}><PaymentElement /><button className="button primary full stripe-submit" disabled={!stripe || processing}>{processing ? 'Processing…' : 'Pay securely'}</button>{message && <div className="inline-error">{message}</div>}</form>
}

function HistoryView({ user, data, run, busy }) {
  return <><section className="page-heading"><div><span className="eyebrow">YOUR ACTIVITY</span><h1>Ride history</h1><p>Every ride, all in one place.</p></div><div className="history-total"><strong>{data.rides.length}</strong><span>total rides</span></div></section><section className="card table-card history-card"><RideTable rides={data.rides} role={user.role} run={run} busy={busy} /></section>{user.role === 'RIDER' && <PaymentsPanel payments={data.payments} run={run} />}</>
}

function SupportView({ data, run }) {
  const [form, setForm] = useState({ subject: '', message: '', rideId: '' })
  return <><section className="page-heading"><div><span className="eyebrow">WE’RE HERE FOR YOU</span><h1>Help & support</h1><p>Tell us what happened and our team will take a look.</p></div></section><div className="support-grid"><section className="card support-form"><span className="eyebrow">NEW REQUEST</span><h2>How can we help?</h2><form className="form-stack" onSubmit={(event) => { event.preventDefault(); const body = { subject: form.subject, message: form.message, ...(form.rideId ? { rideId: form.rideId } : {}) }; run(() => post('/api/support/tickets', body), 'Support request sent.').then((result) => { if (result?.ok) setForm({ subject: '', message: '', rideId: '' }) }) }}><label>Subject<input value={form.subject} onChange={(event) => setForm({ ...form, subject: event.target.value })} placeholder="A short summary" maxLength="140" required /></label><label>Ride reference <span className="optional">(optional)</span><input value={form.rideId} onChange={(event) => setForm({ ...form, rideId: event.target.value })} placeholder="Paste a ride ID" /></label><label>Message<textarea value={form.message} onChange={(event) => setForm({ ...form, message: event.target.value })} placeholder="Share a few details so we can help…" minLength="10" maxLength="4000" rows="5" required /></label><button className="button primary" type="submit">Send to support<Glyph name="arrow" /></button></form></section><section className="card ticket-card"><div className="section-title"><div><span className="eyebrow">YOUR REQUESTS</span><h2>Support history</h2></div></div>{data.tickets.length ? data.tickets.map((ticket) => <TicketItem ticket={ticket} key={ticket.id} />) : <EmptyState title="No open conversations" text="When you contact us, your request will appear here." />}</section></div></>
}

function TicketItem({ ticket, admin = false, run }) {
  return <div className="ticket-item"><div className="ticket-item-top"><strong>{ticket.subject}</strong><StatusPill status={ticket.status} /></div><p>{ticket.message}</p><small>{admin ? `${ticket.userName} · ` : ''}{date(ticket.createdAt)}{ticket.rideId ? ` · Ride ${ticket.rideId.slice(0, 8)}` : ''}</small>{admin && ticket.status === 'OPEN' && <button className="button secondary small-button resolve-button" onClick={() => run(() => patch(`/api/admin/support/tickets/${ticket.id}`, { status: 'RESOLVED' }), 'Ticket marked resolved.')}>Mark resolved</button>}</div>
}

function AdminView({ data, run }) {
  const [promo, setPromo] = useState({ code: '', discountPercent: 10, expiresAt: '', maxUses: '' })
  const metrics = data.metrics || {}
  return <><section className="page-heading"><div><span className="eyebrow">OPERATIONS</span><h1>Admin overview</h1><p>Monitor service activity and take care of your community.</p></div><div className="admin-badge"><Glyph name="shield" /> Admin workspace</div></section><div className="metric-grid">{[['TOTAL USERS', metrics.users ?? 0, 'users'], ['DRIVERS', metrics.drivers ?? 0, 'driver'], ['TOTAL RIDES', metrics.rides ?? 0, 'route'], ['ACTIVE RIDES', metrics.activeRides ?? 0, 'activity'], ['PAYMENTS PAID', metrics.paymentsPaid ?? 0, 'card'], ['OPEN SUPPORT', metrics.ticketsOpen ?? 0, 'help']].map(([label, value, icon]) => <div className="card metric-card" key={label}><div className="stat-top"><span>{label}</span><span className="stat-icon"><Glyph name={icon} /></span></div><strong>{value}</strong></div>)}</div>
    <div className="admin-grid"><section className="card admin-users"><div className="section-title"><div><span className="eyebrow">COMMUNITY</span><h2>Recent accounts</h2></div></div>{data.users.slice(0, 8).map((account) => <div className="user-row" key={account.id}><div className="avatar">{account.name.slice(0, 1).toUpperCase()}</div><div><strong>{account.name}</strong><small>{account.email}</small></div><span className="role-tag">{account.role}</span></div>)}</section><section className="card admin-promos"><div className="section-title"><div><span className="eyebrow">GROWTH</span><h2>Promo codes</h2></div></div><form className="promo-form" onSubmit={(event) => { event.preventDefault(); const body = { code: promo.code, discountPercent: Number(promo.discountPercent), expiresAt: new Date(promo.expiresAt).toISOString(), ...(promo.maxUses ? { maxUses: Number(promo.maxUses) } : {}) }; run(() => post('/api/admin/promos', body), 'Promo code created.').then((result) => { if (result?.ok) setPromo({ code: '', discountPercent: 10, expiresAt: '', maxUses: '' }) }) }}><label>Code<input required value={promo.code} onChange={(event) => setPromo({ ...promo, code: event.target.value.toUpperCase() })} maxLength="30" placeholder="WELCOME10" /></label><div className="promo-inputs"><label>Discount %<input type="number" min="1" max="100" step="1" value={promo.discountPercent} onChange={(event) => setPromo({ ...promo, discountPercent: event.target.value })} required /></label><label>Max uses<input type="number" min="1" value={promo.maxUses} onChange={(event) => setPromo({ ...promo, maxUses: event.target.value })} placeholder="No limit" /></label></div><label>Expires at<input type="datetime-local" value={promo.expiresAt} onChange={(event) => setPromo({ ...promo, expiresAt: event.target.value })} required /></label><button className="button primary full">Create code</button></form><div className="promo-list">{data.promos.map((item) => <div className="promo-row" key={item.id}><div><strong>{item.code}</strong><small>{item.discountPercent}% off · {item.usageCount}{item.maxUses ? `/${item.maxUses}` : ''} uses</small></div><StatusPill status={item.active ? 'ACTIVE' : 'INACTIVE'} />{item.active && <button className="text-button" onClick={() => run(() => patch(`/api/admin/promos/${item.id}/active`, {}), 'Promo code deactivated.')}>Deactivate</button>}</div>)}</div></section></div>
    <section className="card table-card"><div className="section-title"><div><span className="eyebrow">SERVICE ACTIVITY</span><h2>All rides</h2></div></div><RideTable rides={data.rides.slice(0, 12)} role="ADMIN" run={run} busy={false} /></section>
    <section className="card table-card"><div className="section-title"><div><span className="eyebrow">CUSTOMER CARE</span><h2>Support queue</h2></div></div>{data.tickets.length ? data.tickets.map((ticket) => <TicketItem ticket={ticket} admin run={run} key={ticket.id} />) : <EmptyState title="All clear" text="There are no support requests to review." />}</section>
  </>
}

function StatusPill({ status }) {
  const text = status.replaceAll('_', ' ').toLowerCase()
  return <span className={`status-pill status-${text.replaceAll(' ', '-')}`}>{text}</span>
}

function EmptyState({ title, text }) { return <div className="empty-state"><div className="empty-icon"><Glyph name="spark" /></div><strong>{title}</strong><p>{text}</p></div> }

function Glyph({ name }) {
  const paths = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
    help: <><circle cx="12" cy="12" r="9" /><path d="M9.6 9a2.5 2.5 0 1 1 4.2 1.8c-1.1 1-1.8 1.2-1.8 2.7M12 17h.01" /></>,
    route: <><path d="M5 19c3 0 3-6 6-6s3 6 8 6" /><path d="M5 5h.01M19 5h.01M11 13h.01" /><circle cx="5" cy="5" r="2" /><circle cx="19" cy="5" r="2" /></>,
    shield: <><path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" /><path d="m9 12 2 2 4-4" /></>,
    pin: <><path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z" /><circle cx="12" cy="10" r="2.5" /></>,
    arrow: <><path d="M5 12h14" /><path d="m13 6 6 6-6 6" /></>,
    card: <><rect x="2" y="5" width="20" height="14" rx="2" /><path d="M2 10h20M6 15h3" /></>,
    activity: <><path d="M3 12h4l3-8 4 16 3-8h4" /></>,
    logout: <><path d="M10 17l5-5-5-5M15 12H3" /><path d="M12 3h7a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-7" /></>,
    spark: <><path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z" /><path d="m19 14 .8 2.2L22 17l-2.2.8L19 20l-.8-2.2L16 17l2.2-.8L19 14Z" /></>,
  }
  return <svg className="glyph" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{paths[name] || paths.spark}</svg>
}

function GoogleMark() { return <svg className="google-mark" viewBox="0 0 48 48" aria-hidden="true"><path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5Z" /><path fill="#4285F4" d="M46.98 24.55c0-1.57-.14-3.09-.41-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.75 7.18l7.73 6C44.42 38.02 47 31.9 47 24.55Z" /><path fill="#FBBC05" d="M10.53 28.59a14.4 14.4 0 0 1 0-9.18l-7.98-6.19a23.94 23.94 0 0 0 0 21.56l7.98-6.19Z" /><path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.9-5.8l-7.73-6c-2.14 1.45-4.88 2.3-8.17 2.3-6.26 0-11.57-4.22-13.46-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48Z" /></svg> }
