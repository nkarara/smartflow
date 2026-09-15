import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api, tokenStore } from "../api/client";
import type { AuthUser, Role } from "../types";

interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (access: string, refresh: string, user: AuthUser) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue>({
  user: null,
  loading: true,
  login: () => undefined,
  logout: () => undefined,
});

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = tokenStore.getAccess();
    if (!token) {
      setLoading(false);
      return;
    }
    api
      .get<{ id: number; email: string; firstName: string; lastName: string; role: Role }>("/auth/me")
      .then((response) => {
        const d = response.data;
        setUser({ id: d.id, email: d.email, firstName: d.firstName, lastName: d.lastName, role: d.role });
      })
      .catch(() => {
        tokenStore.clear();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  const login = (access: string, refresh: string, authUser: AuthUser) => {
    tokenStore.set(access, refresh);
    setUser(authUser);
  };

  const logout = () => {
    const refreshToken = tokenStore.getRefresh();
    if (refreshToken) {
      api.post("/auth/logout", { refreshToken }).catch(() => undefined);
    }
    tokenStore.clear();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>{children}</AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  return useContext(AuthContext);
}