import { useState } from 'react'
import Modal from './Modal'
import { api, errorMessage } from '../api/client'
import { useToast } from '../context/ToastContext'
import { money } from '../utils/format'

export default function OperationModal({ mode, accounts, defaultAccountId, onClose, onSuccess }) {
  const [form, setForm] = useState({ accountId: defaultAccountId || accounts[0]?.id || '', amount: '', description: '' })
  const [confirming, setConfirming] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [key] = useState(() => crypto.randomUUID())
  const notify = useToast()
  const account = accounts.find((item) => String(item.id) === String(form.accountId))
  const verb = mode === 'deposit' ? 'Deposit' : 'Withdraw'

  const validate = () => {
    if (!form.accountId) return 'Choose an account.'
    if (!/^\d+(\.\d{1,4})?$/.test(form.amount) || Number(form.amount) <= 0) return 'Enter a positive amount with up to four decimal places.'
    return ''
  }
  const startConfirmation = (event) => { event.preventDefault(); const issue = validate(); setError(issue); if (!issue) setConfirming(true) }
  const submit = async () => {
    setSaving(true); setError('')
    try {
      const payload = { amount: form.amount, description: form.description.trim() || null }
      if (mode === 'deposit') await api.deposit(form.accountId, payload, key)
      else await api.withdraw(form.accountId, payload, key)
      notify(`${verb} confirmed.`)
      await onSuccess?.()
      onClose()
    } catch (requestError) { setError(errorMessage(requestError, `${verb} could not be completed.`)); setConfirming(false) }
    finally { setSaving(false) }
  }
  return <Modal title={confirming ? `Confirm ${verb.toLowerCase()}` : `${verb} money`} onClose={saving ? () => {} : onClose}>
    {confirming ? <div className="space-y-5"><p className="text-sm text-slate-600">Please verify this operation. Submitting will update your account balance.</p><dl className="rounded-xl bg-slate-50 p-4 text-sm"><div className="flex justify-between gap-4"><dt>Account</dt><dd className="font-semibold">{account?.name}</dd></div><div className="mt-2 flex justify-between gap-4"><dt>Amount</dt><dd className="font-semibold">{money(form.amount, account?.currency)}</dd></div>{form.description && <div className="mt-2 flex justify-between gap-4"><dt>Description</dt><dd className="text-right">{form.description}</dd></div>}</dl>{error && <p className="field-error">{error}</p>}<div className="flex justify-end gap-3"><button className="btn-secondary" disabled={saving} onClick={() => setConfirming(false)}>Back</button><button className={mode === 'withdraw' ? 'btn-danger' : 'btn-primary'} disabled={saving} onClick={submit}>{saving ? 'Submitting…' : `Confirm ${verb}`}</button></div></div> : <form onSubmit={startConfirmation} className="space-y-4"><div><label className="field-label" htmlFor="operation-account">Account</label><select id="operation-account" className="field" value={form.accountId} onChange={(e) => setForm({ ...form, accountId: e.target.value })}>{accounts.map((item) => <option value={item.id} key={item.id}>{item.name} · {money(item.balance, item.currency)}</option>)}</select></div><div><label className="field-label" htmlFor="operation-amount">Amount</label><input id="operation-amount" className="field" inputMode="decimal" placeholder="0.00" value={form.amount} onChange={(e) => setForm({ ...form, amount: e.target.value })} /></div><div><label className="field-label" htmlFor="operation-description">Description <span className="text-slate-400">optional</span></label><input id="operation-description" className="field" maxLength="255" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>{error && <p className="field-error">{error}</p>}<div className="flex justify-end gap-3"><button type="button" className="btn-secondary" onClick={onClose}>Cancel</button><button className={mode === 'withdraw' ? 'btn-danger' : 'btn-primary'}>{verb}</button></div></form>}
  </Modal>
}
