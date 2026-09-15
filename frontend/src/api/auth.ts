import { api } from "./client";
import type { AuthResponse, UserDto } from "../types";

export const authApi = {
  login: (email: string, password: string) =>
    api.post<AuthResponse>("/auth/login", { email, password }).then((r) => r.data),
  register: (payload: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phone?: string;
    role: "CLIENT" | "TECHNICIAN";
    companyName?: string;
    location?: string;
    skills?: string[];
  }) => api.post<AuthResponse>("/auth/register", payload).then((r) => r.data),
  logout: (refreshToken: string) => api.post("/auth/logout", { refreshToken }).then(() => undefined),
  me: () => api.get<UserDto>("/auth/me").then((r) => r.data),
};