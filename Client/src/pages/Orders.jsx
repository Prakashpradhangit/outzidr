import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Package,
  MapPin,
  CreditCard,
  Calendar,
  CheckCircle2
} from 'lucide-react'
import Header from '../components/Header'
import Footer from '../components/Footer'

function Orders() {
  const navigate = useNavigate()
  
  // State
  const [orders, setOrders] = useState([])
  const [cart, setCart] = useState(null)
  const [loading, setLoading] = useState(true)
  const [toast, setToast] = useState({ show: false, message: '', type: 'error' })

  // Retrieve user metadata directly from localStorage
  const userName = localStorage.getItem('userName') || ''
  const userEmail = localStorage.getItem('userEmail') || ''
  const token = localStorage.getItem('accessToken') || ''

  useEffect(() => {
    if (!token) {
      navigate('/login')
    } else {
      fetchOrders()
      fetchCart()
    }
  }, [token])

  // Helper to build auth headers
  const getAuthHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  })

  // Parse backend error messages and return a clean, friendly string
  const getFriendlyErrorMessage = (errText) => {
    if (!errText) return 'An unexpected error occurred.'
    let message = errText
    try {
      const parsed = JSON.parse(errText)
      if (parsed && parsed.message) {
        message = parsed.message
      }
    } catch (e) {
      // Not JSON
    }
    if (message.includes('No static resource') || message.includes('Resource not found')) {
      return 'Oops! The requested API endpoint could not be found on the server.'
    }
    if (message.includes('exceeds available stock')) {
      return message
    }
    if (message.includes('<html>') || message.length > 200) {
      return 'Something went wrong on the server. Please try again.'
    }
    return message
  }

  // Show beautiful in-app toast notification
  const showNotification = (message, type = 'error') => {
    setToast({ show: true, message, type })
    setTimeout(() => {
      setToast(prev => {
        if (prev.message === message) {
          return { ...prev, show: false }
        }
        return prev
      })
    }, 4000)
  }

  // Fetch Orders belonging to authenticated user
  const fetchOrders = async () => {
    setLoading(true)
    try {
      const response = await fetch('/api/v1/orders/myorders', {
        headers: getAuthHeaders()
      })
      if (response.status === 401 || response.status === 403) {
        localStorage.clear()
        navigate('/login')
        return
      }
      if (!response.ok) {
        throw new Error('Failed to fetch orders')
      }
      const data = await response.json()
      setOrders(data)
    } catch (err) {
      showNotification(getFriendlyErrorMessage(err.message), 'error')
    } finally {
      setLoading(false)
    }
  }

  // Fetch Cart to show quantity badge in header
  const fetchCart = async () => {
    try {
      const response = await fetch('/api/v1/cart', {
        headers: getAuthHeaders()
      })
      if (response.status === 401 || response.status === 403) {
        localStorage.clear()
        navigate('/login')
        return
      }
      const data = await response.json()
      setCart(data)
    } catch (err) {
      console.error('Error fetching cart', err)
    }
  }

  // API: Logout
  const handleLogout = async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        await fetch('/api/v1/auth/logout', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        })
      }
    } catch (e) {
      console.error('Logout error', e)
    } finally {
      localStorage.clear()
      navigate('/login')
    }
  }

  const cartTotalQty = cart?.cartItems?.reduce((sum, item) => sum + item.quantity, 0) || 0

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'DELIVERED':
        return 'bg-emerald-50 border border-emerald-200 text-emerald-700'
      case 'SHIPPED':
        return 'bg-blue-50 border border-blue-200 text-blue-700'
      case 'CANCELLED':
        return 'bg-rose-50 border border-rose-200 text-rose-700'
      case 'CREATED':
      default:
        return 'bg-indigo-50 border border-indigo-200 text-indigo-700'
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return ''
    try {
      const date = new Date(dateString)
      return date.toLocaleDateString('en-IN', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      })
    } catch (e) {
      return dateString
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans">
      <Header cartCount={cartTotalQty} />

      {/* Main Container */}
      <main className="flex-1 max-w-4xl w-full mx-auto p-6 space-y-6">
        <h2 className="text-lg font-bold text-slate-800 tracking-tight flex items-center gap-2">
          <Package size={20} className="text-indigo-600" />
          <span>My Orders</span>
        </h2>

        {loading ? (
          <div className="flex flex-col items-center justify-center py-20 space-y-3">
            <div className="h-8 w-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
            <p className="text-slate-500 text-xs font-semibold">Loading orders...</p>
          </div>
        ) : orders.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-2xl p-16 text-center space-y-4 shadow-sm max-w-lg mx-auto">
            <Package size={48} className="mx-auto text-slate-300 animate-pulse" />
            <h3 className="text-base font-bold text-slate-700">No orders found</h3>
            <p className="text-slate-500 text-xs">You haven't placed any orders yet.</p>
            <button
              onClick={() => navigate('/')}
              className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2.5 rounded-xl text-xs font-bold transition-all active:scale-95 cursor-pointer shadow-md shadow-indigo-600/10"
            >
              Start Shopping
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {orders.map((order) => (
              <div 
                key={order.id} 
                className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm space-y-4 hover:border-slate-300 transition-all duration-205"
              >
                {/* Order Header */}
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center border-b border-slate-100 pb-3 gap-3">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-extrabold text-sm text-slate-800">Order #OD-{order.id}</span>
                      <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider ${getStatusBadgeClass(order.orderStatus)}`}>
                        {order.orderStatus}
                      </span>
                    </div>
                    <div className="flex items-center gap-1.5 text-[11px] text-slate-400 font-medium">
                      <Calendar size={12} />
                      <span>{formatDate(order.createdAt)}</span>
                    </div>
                  </div>
                  <div className="text-right sm:text-right">
                    <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Total Amount</p>
                    <p className="text-base font-black text-indigo-600">₹{order.total?.toFixed(2)}</p>
                  </div>
                </div>

                {/* Order Details Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs text-slate-500">
                  <div className="space-y-2 border-r border-slate-100 pr-0 md:pr-4">
                    <div className="flex items-start gap-2">
                      <MapPin size={14} className="text-slate-400 shrink-0 mt-0.5" />
                      <div>
                        <p className="font-semibold text-slate-700">Shipping Address</p>
                        <p className="mt-0.5 text-slate-500 font-medium">{order.shippingAddress}</p>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <div className="flex items-start gap-2">
                      <CreditCard size={14} className="text-slate-400 shrink-0 mt-0.5" />
                      <div>
                        <p className="font-semibold text-slate-700">Payment Information</p>
                        <p className="mt-0.5 text-slate-500 font-medium">
                          Method: <span className="font-bold text-slate-700">{order.paymentMethod}</span>
                        </p>
                        <p className="mt-0.5 text-slate-500 font-medium">
                          Status: <span className="font-bold text-slate-700">{order.paymentStatus}</span>
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Subtotal Calculations */}
                <div className="border-t border-slate-100 pt-3 flex flex-wrap gap-x-6 gap-y-2 text-xs text-slate-400 font-medium">
                  <div>
                    Subtotal: <span className="text-slate-600 font-bold">₹{order.subtotal?.toFixed(2)}</span>
                  </div>
                  {order.discountAmount > 0 && (
                    <div className="text-indigo-600">
                      Discount: <span className="font-bold">-₹{order.discountAmount?.toFixed(2)}</span>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Custom Toast Notification Popup */}
      {toast.show && (
        <div className="fixed top-5 left-1/2 -translate-x-1/2 z-55 flex items-center gap-3 px-5 py-3.5 rounded-2xl shadow-xl border animate-slideDown max-w-md w-[90%] sm:w-auto bg-white border-slate-200">
          <div className={`h-2.5 w-2.5 rounded-full ${toast.type === 'error' ? 'bg-red-500 animate-pulse' : 'bg-emerald-500 animate-ping'}`} />
          {toast.type === 'success' && <div className="absolute top-5 left-[23px] h-2.5 w-2.5 rounded-full bg-emerald-500 animate-pulse" />}
          <span className="text-xs font-bold text-slate-800">{toast.message}</span>
          <button
            onClick={() => setToast(prev => ({ ...prev, show: false }))}
            className="text-slate-400 hover:text-slate-650 font-bold ml-auto pl-4 cursor-pointer text-sm"
          >
            ✕
          </button>
        </div>
      )}

      <Footer />
    </div>
  )
}

export default Orders
