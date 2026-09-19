import axios, { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from "axios";
import type { ErrorResponse } from "../types";

// Clés de stockage local (localStorage) des jetons de session
const ACCESS_TOKEN_KEY = "smartflow.accessToken";
const REFRESH_TOKEN_KEY = "smartflow.refreshToken";

/**
 * Zone de stockage des jetons.
 * Les jetons sont gardés dans le localStorage pour survivre au rechargement des pages.
 */
export const tokenStore = {
  /** Retourne l'access token stocké (ou null si absent). */
  getAccess: (): string | null => localStorage.getItem(ACCESS_TOKEN_KEY),
  /** Retourne le refresh token stocké (ou null si absent). */
  getRefresh: (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY),
  /** Enregistre le couple access/refresh après connexion ou refresh. */
  set: (access: string, refresh: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, access);
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh);
  },
  /** Supprime les jetons (déconnexion ou session expirée). */
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};

/**
 * Instance Axios partagée par toute l'application.
 * Base URL : /api (proxifiée par Vite en dev, ou nginx en production).
 */
export const api = axios.create({ baseURL: "/api" });

// ------- Intercepteur de REQUÊTE : ajoute automatiquement le Bearer token -------
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccess();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

// Types utilitaires pour le gestionnaire de refresh
type RetriableRequest = InternalAxiosRequestConfig & { _retry?: boolean };

/** Promesse du refresh en cours : évite les appels parallèles en double. */
let refreshing: Promise<string> | null = null;

/**
 * Appelle /auth/refresh avec le refresh token stocké puis met à jour le localStorage.
 *
 * @returns le nouveau access token
 */
async function refreshTokens(): Promise<string> {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) {
    throw new Error("Aucun refresh token disponible");
  }
  const response: AxiosResponse<{ accessToken: string; refreshToken: string }> = await axios.post(
    "/api/auth/refresh",
    { refreshToken },
  );
  tokenStore.set(response.data.accessToken, response.data.refreshToken);
  return response.data.accessToken;
}

// ------- Intercepteur de RÉPONSE : rafraîchit le token après une 401 -------
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableRequest | undefined;
    const status = error.response?.status;
    const isAuthEndpoint = original?.url?.startsWith("/auth/") ?? false;

    // Si la requête est rejetée (401) et qu'elle n'est pas déjà relancée, on tente un refresh
    if (status === 401 && original && !original._retry && !isAuthEndpoint) {
      original._retry = true; // évite une boucle infinie
      try {
        // Réutilise le refresh en cours (évite de le lancer plusieurs fois en parallèle)
        refreshing = refreshing ?? refreshTokens().finally(() => {
          refreshing = null;
        });
        const newAccessToken = await refreshing;
        // Relance la requête initiale avec le nouveau jeton
        original.headers.set("Authorization", `Bearer ${newAccessToken}`);
        return api(original);
      } catch (refreshError) {
        // Refresh impossible → on déconnecte et on renvoie vers la page de connexion
        tokenStore.clear();
        window.location.href = "/login";
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  },
);

/**
 * Extrait un message d'erreur lisible depuis une erreur Axios
 * (message API, erreurs de validation par champ, ou message générique).
 */
export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ErrorResponse | undefined;
    if (data?.message) return data.message;
    if (data?.fieldErrors) return Object.values(data.fieldErrors).join(", ");
    return error.message;
  }
  return "Une erreur est survenue";
}