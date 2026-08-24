import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, ShoppingBag } from 'lucide-react'
import Header from '../components/Header'
import Footer from '../components/Footer'

function Homepage() {
  const navigate = useNavigate()
  
  // Catalog State
  const [products, setProducts] = useState([])
  const [cart, setCart] = useState(null)
  const [toast, setToast] = useState({ show: false, message: '', type: 'error' })

  // Retrieve user token from localStorage
  const token = localStorage.getItem('accessToken') || ''

  // Load data on mount and token change
  useEffect(() => {
    if (token) {
      fetchProducts()
      fetchCart()
    } else {
      navigate('/login')
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

  // API: Fetch Products
  const fetchProducts = async () => {
    try {
      const response = await fetch('/api/v1/products/all', {
        headers: getAuthHeaders()
      })
      if (response.status === 401 || response.status === 403) {
        localStorage.clear()
        navigate('/login')
        return
      }
      const data = await response.json()
      setProducts(data)
    } catch (err) {
      console.error('Error fetching products', err)
    }
  }

  // API: Fetch Cart (only to show correct item quantity badge in header)
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

  // API: Add to Cart
  const handleAddToCart = async (product) => {
    try {
      const response = await fetch('/api/v1/cart/items', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          productId: product.id,
          quantity: 1
        })
      })

      if (!response.ok) {
        const errorText = await response.text()
        throw new Error(errorText || 'Failed to add item to cart')
      }

      await fetchCart()
      showNotification('Added to cart successfully!', 'success')
      navigate('/cart') // Navigate directly to cart page
    } catch (err) {
      showNotification(getFriendlyErrorMessage(err.message), 'error')
    }
  }

  const cartTotalQty = cart?.cartItems?.reduce((sum, item) => sum + item.quantity, 0) || 0

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans">
      <Header cartCount={cartTotalQty} />

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-6 space-y-6">
        
        {/* Product Catalog Grid */}
        <div className="space-y-4 animate-fadeIn">
          <h2 className="text-lg font-bold text-slate-800 tracking-tight flex items-center gap-2">
            <ShoppingBag size={18} className="text-indigo-600" />
            <span>Product Catalog</span>
          </h2>
          
          {products.length === 0 ? (
            <div className="bg-white border border-slate-200 rounded-2xl p-12 text-center space-y-3">
              <ShoppingBag size={36} className="mx-auto text-slate-350" />
              <p className="text-slate-500 text-sm">No products found in the database.</p>
              <p className="text-slate-400 text-xs">Run your database migrations or add mock entries.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {products.map((prod) => (
                <div
                  key={prod.id}
                  className={`bg-white rounded-2xl border p-5 flex flex-col justify-between transition-all duration-200 ${
                    prod.productStatus !== 'ACTIVE'
                      ? 'border-slate-100 opacity-60'
                      : 'border-slate-200 hover:border-slate-300 hover:shadow-sm'
                  }`}
                >
                  <div className="space-y-3">
                    <div className="flex justify-between items-start">
                      <span className="bg-slate-100 text-slate-655 text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider">
                        {prod.category}
                      </span>
                      {prod.productStatus !== 'ACTIVE' && (
                        <span className="bg-red-55 text-red-600 text-[10px] px-2 py-0.5 rounded-full font-bold uppercase tracking-wider border border-red-100">
                          Inactive
                        </span>
                      )}
                    </div>

                    <h3 className="font-bold text-slate-900 text-base">{prod.product_name}</h3>
                    <p className="text-xs text-slate-500 line-clamp-2">{prod.product_description || 'No description available.'}</p>
                    <p className="text-xs text-slate-400 font-medium">SKU: {prod.sku}</p>

                    <div className="flex items-center justify-between text-xs pt-1 border-t border-slate-50">
                      <span className="text-slate-400 font-medium">In Stock:</span>
                      <span className={`font-semibold ${prod.quantity === 0 ? 'text-red-500' : 'text-slate-700'}`}>
                        {prod.quantity === 0 ? 'Out of stock' : `${prod.quantity} units`}
                      </span>
                    </div>
                  </div>

                  <div className="mt-5 flex items-center justify-between gap-4 pt-3 border-t border-slate-100">
                    <span className="text-lg font-extrabold text-slate-900">₹{prod.price.toFixed(2)}</span>
                    {prod.productStatus !== 'ACTIVE' ? (
                      <button disabled className="bg-slate-100 text-slate-400 px-4 py-2 rounded-xl text-xs font-semibold cursor-not-allowed border border-slate-200">
                        Inactive
                      </button>
                    ) : prod.quantity === 0 ? (
                      <button disabled className="bg-red-50 text-red-400 px-4 py-2 rounded-xl text-xs font-semibold cursor-not-allowed border border-red-150">
                        Sold Out
                      </button>
                    ) : (
                      <button
                        onClick={() => handleAddToCart(prod)}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-xl text-xs font-bold transition-all active:scale-95 flex items-center gap-1.5 cursor-pointer shadow-sm shadow-indigo-600/10"
                      >
                        <Plus size={14} /> Add
                      </button>
                    )}
                  </div>
                </div>
              ))}
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

      <Footer />
    </div>
  )
}

export default Homepage
