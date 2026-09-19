import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api, tokenStore } from "../api/client";
import type { AuthUser, Role } from "../types";

/**
 * Interface du contexte d'authentification partagé par toute l'application.
 *
 * @property user    utilisateur connecté (ou null)
 * @property loading vrai pendant la restauration de la session au démarrage
 * @property login   enregistre les jetons et mémorise l'utilisateur
 * @property logout  révoque la session côté serveur et vide l'état local
 */
interface AuthContextValue {
  user: AuthUser | null;
  loading: boolean;
  login: (access: string, refresh: string, user: AuthUser) => void;
  logout: () => void;
}

/** Contexte React + valeur par défaut (permet l'appel de useAuth() sans provider). */
const AuthContext = createContext<AuthContextValue>({
  user: null,
  loading: true,
  login: () => undefined,
  logout: () => undefined,
});

/**
 * Fournisseur global d'authentification.
 * Au démarrage, restaure la session depuis le localStorage en interrogeant /auth/me.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  // dès le premier rendu : si un token existe, on vérifie qu'il est encore valide
  useEffect(() => {
    const token = tokenStore.getAccess();
    if (!token) {
      setLoading(false); // pas de session → rien à restaurer
      return;
    }
    // Appel /auth/me : renvoie 401 automatiquement si le token est expiré
    // (l'intercepteur Axios tente alors un refresh avec le refresh token)
    api
      .get<{ id: number; email: string; firstName: string; lastName: string; role: Role }>("/auth/me")
      .then((response) => {
        const d = response.data;
        setUser({ id: d.id, email: d.email, firstName: d.firstName, lastName: d.lastName, role: d.role });
      })
      .catch(() => {
        // Session réellement invalide → on nettoie le stockage local
        tokenStore.clear();
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  /**
   * Connexion : mémorise les jetons puis l'utilisateur courant.
   */
  const login = (access: string, refresh: string, authUser: AuthUser) => {
    tokenStore.set(access, refresh);
    setUser(authUser);
  };

  /**
   * Déconnexion : révoque le refresh token côté serveur puis vide l'état local.
   */
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
/** Accès au contexte d'authentification depuis n'importe quel composant. */
export function useAuth() {
  return useContext(AuthContext);
}