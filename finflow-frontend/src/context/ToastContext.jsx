import { createContext, useCallback, useContext, useState } from 'react'
import { CheckCircle2, CircleAlert, X } from 'lucide-react'

const ToastContext = createContext(null)

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const notify = useCallback((message, type = 'success') => {
    const id = crypto.randomUUID()
    setToasts((current) => [...current, { id, message, type }])
    window.setTimeout(() => setToasts((current) => current.filter((toast) => toast.id !== id)), 5000)
  }, [])
  return <ToastContext.Provider value={notify}>{children}
    <div className="fixed right-4 top-4 z-[60] space-y-2" aria-live="polite">
      {toasts.map((toast) => <div key={toast.id} className={`flex max-w-sm items-start gap-3 rounded-xl border p-4 shadow-lg ${toast.type === 'success' ? 'border-emerald-200 bg-emerald-50 text-emerald-900' : 'border-rose-200 bg-rose-50 text-rose-900'}`}>
        {toast.type === 'success' ? <CheckCircle2 size={19} /> : <CircleAlert size={19} />}<p className="flex-1 text-sm font-medium">{toast.message}</p>
        <button onClick={() => setToasts((current) => current.filter((item) => item.id !== toast.id))} aria-label="Dismiss notification"><X size={16} /></button>
      </div>)}
    </div>
  </ToastContext.Provider>
}

export const useToast = () => useContext(ToastContext)
