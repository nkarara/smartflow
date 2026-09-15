import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { NotificationBell } from "./NotificationBell";
import type { Role } from "../types";

const NAV_ITEMS: { to: string; label: string; icon: string; roles?: Role[] }[] = [
  { to: "/", label: "Tableau de bord", icon: "📊" },
  { to: "/interventions", label: "Interventions", icon: "🔧" },
  { to: "/interventions/new", label: "Nouvelle demande", icon: "➕", roles: ["CLIENT", "MANAGER", "ADMIN"] },
  { to: "/admin/users", label: "Utilisateurs", icon: "👥", roles: ["ADMIN"] },
  { to: "/admin/clients", label: "Clients", icon: "🏢", roles: ["ADMIN", "MANAGER"] },
  { to: "/admin/technicians", label: "Techniciens", icon: "👷", roles: ["ADMIN", "MANAGER"] },
  { to: "/admin/categories", label: "Catégories", icon: "🗂️", roles: ["ADMIN"] },
];

export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const items = NAV_ITEMS.filter((item) => !item.roles || (user && item.roles.includes(user.role)));

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-64 flex-col border-r border-slate-200 bg-white md:flex">
        <div className="border-b border-slate-100 px-6 py-5">
          <h1 className="text-xl font-bold text-indigo-700">SmartFlow</h1>
          <p className="text-xs text-slate-400">Gestion des interventions</p>
        </div>
        <nav className="flex-1 space-y-1 px-3 py-4">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/"}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition ${
                  isActive ? "bg-indigo-50 text-indigo-700" : "text-slate-600 hover:bg-slate-50"
                }`
              }
            >
              <span>{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-100 px-5 py-4 text-xs text-slate-400">SmartFlow v0.1.0</div>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-6">
          <div className="hidden text-sm text-slate-500 md:block">
            {user ? `${user.firstName} ${user.lastName}` : ""}
            <span className="ml-2 rounded-full bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600">
              {user?.role}
            </span>
          </div>
          <div className="flex items-center gap-3 md:ml-auto">
            <NotificationBell />
            <button
              onClick={handleLogout}
              className="rounded-lg border border-slate-200 px-3 py-1.5 text-sm text-slate-600 transition hover:bg-slate-50"
            >
              Déconnexion
            </button>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}