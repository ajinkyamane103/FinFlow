import { ArrowUpRight, Landmark } from 'lucide-react'
import { Link } from 'react-router-dom'
import { money } from '../utils/format'

export default function AccountCard({ account }) { return <article className="card p-5"><div className="flex items-start justify-between"><div className="grid h-10 w-10 place-items-center rounded-xl bg-emerald-50 text-emerald-700"><Landmark size={20} /></div><span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">{account.accountType}</span></div><p className="mt-6 text-sm text-slate-500">{account.name}</p><p className="mt-1 text-2xl font-bold tracking-tight">{money(account.balance, account.currency)}</p><div className="mt-5 border-t pt-4"><Link className="inline-flex items-center gap-1 text-sm font-semibold text-emerald-700 hover:text-emerald-800" to={`/transactions?account=${account.id}`}>View activity <ArrowUpRight size={16} /></Link></div></article> }
