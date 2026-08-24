import { useNavigate, useLocation } from "react-router-dom";
import { User, ShoppingCart, LogOut, Truck } from "lucide-react";

function Header({ cartCount = 0 }) {
  const navigate = useNavigate();
  const location = useLocation();

  const userName = localStorage.getItem("userName") || "";
  const userEmail = localStorage.getItem("userEmail") || "";

  const handleLogout = async () => {
    try {
      const refreshToken = localStorage.getItem("refreshToken");
      const token = localStorage.getItem("accessToken");
      if (refreshToken && token) {
        await fetch("/api/v1/auth/logout", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({ refreshToken }),
        });
      }
    } catch (e) {
      console.error("Logout error", e);
    } finally {
      localStorage.clear();
      navigate("/login");
    }
  };

  const isOrdersActive = location.pathname === "/orders";
  const isCartActive = location.pathname === "/cart";

  return (
    <header className="bg-white border-b border-slate-200 px-6 py-4 sticky top-0 z-40 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <span
          onClick={() => navigate("/")}
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
            <p className="text-xs font-semibold text-slate-700">
              {userName || userEmail}
            </p>
          </div>
        </div>

        {/* Cart Icon Button */}
        <button
          onClick={() => navigate("/cart")}
          className={`p-2.5 rounded-xl relative transition-all active:scale-95 cursor-pointer ${
            isCartActive
              ? "bg-indigo-50 text-indigo-600"
              : "hover:bg-slate-100 text-slate-700"
          }`}
          title="My Cart"
        >
          <ShoppingCart size={20} />
          {cartCount > 0 && (
            <span className="absolute -top-1 -right-1 bg-indigo-600 text-white text-[10px] font-bold h-5 w-5 rounded-full flex items-center justify-center border-2 border-white shadow-sm animate-pulse">
              {cartCount}
            </span>
          )}
        </button>

        {/* Orders Icon Button */}
        <button
          onClick={() => navigate("/orders")}
          className={`p-2.5 rounded-xl relative transition-all active:scale-95 cursor-pointer ${
            isOrdersActive
              ? "bg-indigo-50 text-indigo-600"
              : "hover:bg-slate-100 text-slate-700"
          }`}
          title="My Orders"
        >
          <Truck size={20} />
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
  );
}

export default Header;
