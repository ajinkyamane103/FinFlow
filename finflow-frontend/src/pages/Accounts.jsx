import { useCallback, useEffect, useState } from 'react'
import { ArrowDownToLine, ArrowUpFromLine, Plus } from 'lucide-react'
import AccountCard from '../components/AccountCard'
import CreateAccountModal from '../components/CreateAccountModal'
import OperationModal from '../components/OperationModal'
import { EmptyState, ErrorState, LoadingState } from '../components/StatusState'
import { api, errorMessage } from '../api/client'

export default function Accounts() { const [accounts, setAccounts] = useState(null); const [error, setError] = useState(''); const [modal, setModal] = useState(null)
  const load = useCallback(async () => { setError(''); try { setAccounts((current) => current ? current : null); const { data } = await api.accounts(); setAccounts(data) } catch (requestError) { setError(errorMessage(requestError)) } }, [])
  useEffect(() => { load() }, [load])
  if (!accounts && !error) return <LoadingState label="Loading your accounts…" />
  if (!accounts) return <ErrorState message={error} retry={load} />
  return <><div className="mb-7 flex flex-col justify-between gap-4 sm:flex-row sm:items-end"><div><p className="text-sm font-semibold text-emerald-700">Accounts</p><h1 className="mt-1 text-3xl font-bold tracking-tight">Where your money lives</h1><p className="mt-2 text-sm text-slate-600">Create and track the accounts you own.</p></div><button className="btn-primary" onClick={() => setModal('create')}><Plus size={18} />New account</button></div>{accounts.length ? <><div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">{accounts.map((account) => <AccountCard key={account.id} account={account} />)}</div><section className="mt-8"><h2 className="text-xl font-bold">Quick account actions</h2><div className="mt-4 grid gap-3 sm:grid-cols-2"><button className="btn-secondary justify-start" onClick={() => setModal('deposit')}><ArrowDownToLine size={18} className="text-emerald-700" />Deposit into an account</button><button className="btn-secondary justify-start" onClick={() => setModal('withdraw')}><ArrowUpFromLine size={18} />Withdraw from an account</button></div></section></> : <EmptyState title="No accounts yet" detail="Create a savings or checking account to begin tracking balances." action={<button className="btn-primary" onClick={() => setModal('create')}>Create account</button>} />}{modal === 'create' && <CreateAccountModal onClose={() => setModal(null)} onSuccess={load} />}{(modal === 'deposit' || modal === 'withdraw') && <OperationModal mode={modal} accounts={accounts} onClose={() => setModal(null)} onSuccess={load} />}</> }
