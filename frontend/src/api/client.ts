import axios, { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from "axios";
import type { ErrorResponse } from "../types";

const ACCESS_TOKEN_KEY = "smartflow.accessToken";
const REFRESH_TOKEN_KEY = "smartflow.refreshToken";

export const tokenStore = {
  getAccess: (): string | null => localStorage.getItem(ACCESS_TOKEN_KEY),
  getRefresh: (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY),
  set: (access: string, refresh: string) => {
    localStorage.setItem(ACCESS_TOKEN_KEY, access);
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh);
  },
  clear: () => {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};

export const api = axios.create({ baseURL: "/api" });

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.getAccess();
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

type RetriableRequest = InternalAxiosRequestConfig & { _retry?: boolean };

let refreshing: Promise<string> | null = null;

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

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableRequest | undefined;
    const status = error.response?.status;
    const isAuthEndpoint = original?.url?.startsWith("/auth/") ?? false;

    if (status === 401 && original && !original._retry && !isAuthEndpoint) {
      original._retry = true;
      try {
        refreshing = refreshing ?? refreshTokens().finally(() => {
          refreshing = null;
        });
        const newAccessToken = await refreshing;
        original.headers.set("Authorization", `Bearer ${newAccessToken}`);
        return api(original);
      } catch (refreshError) {
        tokenStore.clear();
        window.location.href = "/login";
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  },
);

/** Extrait un message d'erreur lisible depuis une erreur Axios. */
export function apiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as ErrorResponse | undefined;
    if (data?.message) return data.message;
    if (data?.fieldErrors) return Object.values(data.fieldErrors).join(", ");
    return error.message;
  }
  return "Une erreur est survenue";
}