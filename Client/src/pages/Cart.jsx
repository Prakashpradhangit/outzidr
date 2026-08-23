import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  User,
  ShoppingCart,
  LogOut,
  Plus,
  Minus,
  Trash2,
  Tag,
  CheckCircle2,
  MapPin,
  CreditCard,
  Package
} from 'lucide-react'

function Cart() {
  const navigate = useNavigate()

  // Cart & Order States
  const [cart, setCart] = useState(null)
  const [couponCode, setCouponCode] = useState('')
  const [couponMsg, setCouponMsg] = useState({ text: '', isError: false })
  const [shippingAddress, setShippingAddress] = useState('')
  const [paymentMethod, setPaymentMethod] = useState('COD')
  const [orderSuccess, setOrderSuccess] = useState(null)
  const [toast, setToast] = useState({ show: false, message: '', type: 'error' })

  // Retrieve credentials directly from localStorage
  const userName = localStorage.getItem('userName') || ''
  const userEmail = localStorage.getItem('userEmail') || ''
  const token = localStorage.getItem('accessToken') || ''

  // Redirect to login if token is missing
  useEffect(() => {
    if (!token) {
      navigate('/login')
    } else {
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

  // API: Fetch Cart
  const fetchCart = async () => {
    try {
      const response = await fetch('/api/v1/cart', {
        headers: getAuthHeaders()
      })
      if (response.status === 401 || response.status === 403) {
        handleLogout()
        return
      }
      const data = await response.json()
      setCart(data)
    } catch (err) {
      console.error('Error fetching cart', err)
    }
  }

  // API: Update Cart Item Quantity
  const handleUpdateQuantity = async (productId, newQty) => {
    if (newQty <= 0) {
      handleRemoveItem(productId)
      return
    }

    try {
      const response = await fetch(`/api/v1/cart/items/${productId}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          product_id: productId,
          quantity: newQty
        })
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || 'Failed to update quantity')
      }

      await fetchCart()
      showNotification('Quantity updated.', 'success')
    } catch (err) {
      showNotification(getFriendlyErrorMessage(err.message), 'error')
    }
  }

  // API: Remove Item from Cart
  const handleRemoveItem = async (productId) => {
    try {
      const response = await fetch('/api/v1/cart/items', {
        method: 'DELETE',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          product_id: productId
        })
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || 'Failed to remove item')
      }

      await fetchCart()
      showNotification('Item removed from cart.', 'success')
    } catch (err) {
      showNotification(getFriendlyErrorMessage(err.message), 'error')
    }
  }

  // API: Apply Coupon
  const handleApplyCoupon = async (e) => {
    e.preventDefault()
    setCouponMsg({ text: '', isError: false })
    
    if (!couponCode.trim()) return

    try {
      const response = await fetch('/api/v1/cart/coupon', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ code: couponCode.trim() })
      })

      const message = await response.text()

      if (!response.ok) {
        throw new Error(message || 'Failed to apply coupon')
      }

      setCouponMsg({ text: 'Coupon applied successfully!', isError: false })
      showNotification('Coupon applied successfully!', 'success')
      await fetchCart()
    } catch (err) {
      setCouponMsg({ text: getFriendlyErrorMessage(err.message), isError: true })
    }
  }

  // API: Remove Coupon
  const handleRemoveCoupon = async () => {
    try {
      const response = await fetch('/api/v1/cart/coupon', {
        method: 'DELETE',
        headers: getAuthHeaders()
      })

      if (!response.ok) {
        throw new Error('Failed to remove coupon')
      }

      setCouponCode('')
      setCouponMsg({ text: '', isError: false })
      await fetchCart()
      showNotification('Coupon removed.', 'success')
    } catch (err) {
      showNotification(getFriendlyErrorMessage(err.message), 'error')
    }
  }

  // API: Checkout / Order Placement
  const handlePlaceOrder = async (e) => {
    e.preventDefault()
    if (!shippingAddress.trim()) {
      showNotification('Shipping address is required', 'error')
      return
    }

    try {
      const response = await fetch('/api/v1/orders', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          paymentMethod: paymentMethod,
          shippingAddress: shippingAddress
        })
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || 'Order placement failed')
      }

      const orderData = await response.json()
      setOrderSuccess(orderData)
      setShippingAddress('')
      await fetchCart()
      showNotification('Order placed successfully!', 'success')
    } catch (err) {
      showNotification(getFriendlyErrorMessage(err.message), 'error')
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

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans">
      {/* App Header */}
      <header className="bg-white border-b border-slate-200 px-6 py-4 sticky top-0 z-40 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span 
            onClick={() => navigate('/')} 
            className="text-xl font-black tracking-widest text-slate-900 cursor-pointer hover:opacity-80"
          >
            OUTZDIR
          </span>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 border-r border-slate-200 pr-4">
            <span className="p-2 bg-slate-100 rounded-full text-slate-500">
              <User size={16} />
            </span>
            <div className="text-left">
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Customer</p>
              <p className="text-xs font-semibold text-slate-700">{userName || userEmail}</p>
            </div>
          </div>

          {/* Orders Icon Button */}
          <button
            onClick={() => navigate('/orders')}
            className="p-2.5 hover:bg-slate-100 text-slate-700 rounded-xl transition-all active:scale-95 cursor-pointer"
            title="My Orders"
          >
            <Package size={20} />
          </button>

          {/* Cart Icon Button (already on Cart page, but clickable to refresh) */}
          <button
            onClick={fetchCart}
            className="p-2.5 bg-indigo-50 text-indigo-600 rounded-xl relative transition-all active:scale-95 cursor-pointer"
          >
            <ShoppingCart size={20} />
            {cartTotalQty > 0 && (
              <span className="absolute -top-1 -right-1 bg-indigo-600 text-white text-[10px] font-bold h-5 w-5 rounded-full flex items-center justify-center border-2 border-white shadow-sm animate-pulse">
                {cartTotalQty}
              </span>
            )}
          </button>

          {/* Logout Button */}
          <button
            onClick={handleLogout}
            className="p-2.5 hover:bg-rose-50 text-slate-500 hover:text-rose-600 rounded-xl transition-all active:scale-95 cursor-pointer"
            title="Log Out"
          >
            <LogOut size={20} />
          </button>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-6 space-y-6">
        
        {/* Order Success Receipt */}
        {orderSuccess && (
          <div className="bg-emerald-50 border border-emerald-250 rounded-2xl p-6 max-w-xl mx-auto space-y-4 shadow-sm animate-fadeIn">
            <div className="flex items-center gap-2 text-emerald-800">
              <CheckCircle2 size={20} />
              <h3 className="font-extrabold text-sm uppercase tracking-wider">Order Created Successfully!</h3>
            </div>

            <div className="text-xs text-slate-605 space-y-2">
              <div className="flex justify-between border-b border-slate-105 pb-1.5">
                <span>Order Reference:</span>
                <span className="font-bold text-slate-900">#OD-{orderSuccess.id}</span>
              </div>
              <div className="flex justify-between border-b border-slate-105 pb-1.5">
                <span>Shipping Address:</span>
                <span className="text-slate-800 text-right max-w-[200px] font-medium">{orderSuccess.shippingAddress}</span>
              </div>
              <div className="flex justify-between border-b border-slate-105 pb-1.5">
                <span>Payment Method:</span>
                <span className="font-semibold text-slate-800">{orderSuccess.paymentMethod}</span>
              </div>
              <div className="flex justify-between border-b border-slate-105 pb-1.5">
                <span>Applied Coupon:</span>
                <span className="text-indigo-600 font-bold">{orderSuccess.appliedDiscount?.code || 'None'}</span>
              </div>
              <div className="flex justify-between pt-1 text-sm font-bold text-emerald-800">
                <span>Total Amount Paid (COD):</span>
                <span>₹{orderSuccess.subtotal?.toFixed(2)}</span>
              </div>
            </div>

            <button
              onClick={() => setOrderSuccess(null)}
              className="w-full bg-white hover:bg-slate-50 text-slate-700 py-2 rounded-xl text-xs font-semibold border border-slate-205 transition-all active:scale-95 cursor-pointer"
            >
              Close Receipt
            </button>
          </div>
        )}

        {/* Dedicated Proper Cart Page Content */}
        <div className="space-y-6 animate-fadeIn">
          <h2 className="text-lg font-bold text-slate-800 tracking-tight flex items-center gap-2">
            <ShoppingCart size={18} className="text-indigo-600" />
            <span>Your Shopping Cart</span>
          </h2>

          {!cart || !cart.cartItems || cart.cartItems.length === 0 ? (
            <div className="bg-white border border-slate-200 rounded-2xl p-16 text-center space-y-4 max-w-lg mx-auto shadow-sm">
              <ShoppingCart size={48} className="mx-auto text-slate-300 animate-bounce" />
              <h3 className="text-base font-bold text-slate-700">Your cart is empty</h3>
              <p className="text-slate-500 text-xs">Looks like you haven't added anything to your cart yet.</p>
              <button
                onClick={() => navigate('/')}
                className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2.5 rounded-xl text-xs font-bold transition-all active:scale-95 cursor-pointer shadow-md shadow-indigo-600/10"
              >
                Explore Products
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-start">
              
              {/* Left Side: Cart Items List */}
              <div className="lg:col-span-2 space-y-4">
                <div className="bg-white rounded-2xl border border-slate-200 divide-y divide-slate-100 overflow-hidden shadow-sm">
                  {cart.cartItems.map((item) => (
                    <div key={item.product.id} className="p-6 flex flex-col sm:flex-row justify-between sm:items-center gap-4 transition-all hover:bg-slate-50/30">
                      <div className="flex-1 min-w-0">
                        <span className="bg-slate-100 text-slate-500 text-[9px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider">
                          {item.product.category}
                        </span>
                        <h3 className="text-sm font-bold text-slate-900 mt-1 truncate">{item.product.product_name}</h3>
                        <p className="text-xs text-slate-400 mt-0.5">₹{item.unitPrice.toFixed(2)} each</p>
                        <button
                          onClick={() => handleRemoveItem(item.product.id)}
                          className="text-xs text-red-500 hover:text-red-650 mt-3 flex items-center gap-1 cursor-pointer font-semibold transition-colors"
                        >
                          <Trash2 size={12} /> Remove Item
                        </button>
                      </div>

                      <div className="flex sm:flex-col items-center sm:items-end justify-between sm:justify-center gap-3">
                        <div className="flex items-center bg-slate-50 border border-slate-200 rounded-xl overflow-hidden shadow-sm">
                          <button
                            onClick={() => handleUpdateQuantity(item.product.id, item.quantity - 1)}
                            className="px-3 py-1.5 text-slate-500 hover:bg-slate-100 active:bg-slate-200 cursor-pointer transition-colors"
                          >
                            <Minus size={12} />
                          </button>
                          <span className="px-3 text-xs font-bold text-slate-700 min-w-[24px] text-center">
                            {item.quantity}
                          </span>
                          <button
                            onClick={() => handleUpdateQuantity(item.product.id, item.quantity + 1)}
                            className="px-3 py-1.5 text-slate-500 hover:bg-slate-105 active:bg-slate-200 cursor-pointer transition-colors"
                          >
                            <Plus size={12} />
                          </button>
                        </div>
                        <span className="text-base font-extrabold text-slate-900">
                          ₹{(item.unitPrice * item.quantity).toFixed(2)}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
                
                <button
                  onClick={() => navigate('/')}
                  className="text-xs text-indigo-600 hover:text-indigo-700 font-bold hover:underline cursor-pointer flex items-center gap-1"
                >
                  &larr; Back to Shop
                </button>
              </div>

              {/* Right Side: Order Summary & Checkout Card */}
              <div className="space-y-6">
                
                {/* Summary Card */}
                <div className="bg-white rounded-2xl border border-slate-200 p-6 space-y-6 shadow-sm">
                  <h3 className="font-bold text-slate-800 text-sm uppercase tracking-wider border-b border-slate-100 pb-3">
                    Order Summary
                  </h3>
                  
                  {/* Cart Calculations */}
                  <div className="space-y-3 text-xs text-slate-500">
                    <div className="flex justify-between">
                      <span>Total:</span>
                      <span className="font-semibold text-slate-800">₹{cart.total?.toFixed(2)}</span>
                    </div>
                    {cart.discountAmount > 0 && (
                      <div className="flex justify-between text-indigo-600 font-medium">
                        <span>Discount:</span>
                        <span>-₹{cart.discountAmount?.toFixed(2)}</span>
                      </div>
                    )}
                    <div className="flex justify-between text-sm font-black text-slate-850 pt-3 border-t border-slate-100">
                      <span>Subtotal:</span>
                      <span className="text-lg text-indigo-600 font-black">₹{cart.subtotal?.toFixed(2)}</span>
                    </div>
                  </div>

                  {/* Coupon Section */}
                  <div className="border-t border-slate-100 pt-4 space-y-3">
                    <div className="flex items-center justify-between text-xs font-semibold text-slate-500">
                      <span className="flex items-center gap-1"><Tag size={12} /> Coupon Code:</span>
                      {cart.appliedDiscount ? (
                        <div className="flex items-center gap-1.5 bg-indigo-50 border border-indigo-150 text-indigo-700 text-[10px] px-2 py-0.5 rounded-md">
                          <span className="font-bold">{cart.appliedDiscount.code}</span>
                          <button onClick={handleRemoveCoupon} className="hover:text-red-500 cursor-pointer font-bold">
                            ✕
                          </button>
                        </div>
                      ) : (
                        <span className="text-slate-400 font-medium">None</span>
                      )}
                    </div>

                    {!cart.appliedDiscount ? (
                      <form onSubmit={handleApplyCoupon} className="flex gap-2">
                        <input
                          type="text"
                          placeholder="e.g. PERCENT20"
                          value={couponCode}
                          onChange={(e) => setCouponCode(e.target.value)}
                          className="bg-slate-50 border border-slate-200 focus:bg-white focus:border-indigo-600 rounded-lg px-3 py-2 text-xs text-slate-900 placeholder:text-slate-400 outline-none flex-1 transition-all"
                        />
                        <button
                          type="submit"
                          className="bg-slate-800 hover:bg-slate-700 text-white px-4 py-2 rounded-lg text-xs font-semibold transition-all cursor-pointer"
                        >
                          Apply
                        </button>
                      </form>
                    ) : null}

                    {couponMsg.text && (
                      <p className={`text-[10px] ${couponMsg.isError ? 'text-red-505' : 'text-emerald-600'} font-medium`}>
                        {couponMsg.isError ? '❌ ' : '✅ '}{couponMsg.text}
                      </p>
                    )}
                  </div>
                </div>

                {/* Checkout Form Card */}
                <div className="bg-slate-50 border border-slate-200 rounded-2xl p-6 space-y-4 shadow-sm">
                  <h3 className="font-bold text-slate-800 text-xs uppercase tracking-wider flex items-center gap-1">
                    Shipping Details
                  </h3>
                  <form onSubmit={handlePlaceOrder} className="space-y-4">
                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1">
                        <MapPin size={10} /> Delivery Address
                      </label>
                      <input
                        type="text"
                        required
                        placeholder="Enter your complete address"
                        value={shippingAddress}
                        onChange={(e) => setShippingAddress(e.target.value)}
                        className="w-full bg-white border border-slate-200 focus:border-indigo-600 rounded-xl px-3.5 py-2.5 text-xs text-slate-900 outline-none transition-all shadow-2xs"
                      />
                    </div>

                    <div className="space-y-1.5">
                      <label className="text-[10px] font-bold text-slate-500 uppercase tracking-wider flex items-center gap-1">
                        <CreditCard size={10} /> Payment Option
                      </label>
                      <select
                        value={paymentMethod}
                        onChange={(e) => setPaymentMethod(e.target.value)}
                        className="w-full bg-white border border-slate-200 rounded-xl px-3.5 py-2.5 text-xs text-slate-750 outline-none cursor-pointer"
                      >
                        <option value="COD">Cash On Delivery (COD)</option>
                      </select>
                    </div>

                    <button
                      type="submit"
                      className="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-3.5 rounded-xl text-xs font-bold transition-all shadow-md hover:shadow-indigo-600/10 active:scale-98 cursor-pointer mt-2"
                    >
                      Place COD Order
                    </button>
                  </form>
                </div>

              </div>
            </div>
          )}
        </div>
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

      {/* Page Footer */}
      <footer className="py-8 text-center text-[11px] text-slate-400 bg-white border-t border-slate-200 mt-12">
        <p>&copy; {new Date().getFullYear()} Outzdir E-Commerce. All rights reserved.</p>
        <p className="mt-1">Powered by React, Vite, Tailwind CSS v4 & Lucide Icons</p>
      </footer>
    </div>
  )
}

export default Cart
