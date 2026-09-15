import { api } from "./client";
import type { CategoryDto, ClientDto, TechnicianDto, UserDto } from "../types";

export const usersApi = {
  list: () => api.get<UserDto[]>("/users").then((r) => r.data),
  create: (payload: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phone?: string;
    role: string;
    companyName?: string;
    location?: string;
    specialty?: string;
    skills?: string[];
  }) => api.post<UserDto>("/users", payload).then((r) => r.data),
  update: (id: number, payload: { firstName?: string; lastName?: string; phone?: string; enabled?: boolean }) =>
    api.put<UserDto>(`/users/${id}`, payload).then((r) => r.data),
  remove: (id: number) => api.delete(`/users/${id}`).then(() => undefined),
};

export const clientsApi = {
  list: () => api.get<ClientDto[]>("/clients").then((r) => r.data),
  create: (payload: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phone?: string;
    companyName: string;
    address?: string;
    city?: string;
    siret?: string;
  }) => api.post<ClientDto>("/clients", payload).then((r) => r.data),
  update: (id: number, payload: Partial<ClientDto>) =>
    api.put<ClientDto>(`/clients/${id}`, payload).then((r) => r.data),
  remove: (id: number) => api.delete(`/clients/${id}`).then(() => undefined),
};

export const techniciansApi = {
  list: () => api.get<TechnicianDto[]>("/technicians").then((r) => r.data),
  create: (payload: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phone?: string;
    specialty?: string;
    location?: string;
    available?: boolean;
    skills?: string[];
  }) => api.post<TechnicianDto>("/technicians", payload).then((r) => r.data),
  update: (id: number, payload: Partial<TechnicianDto>) =>
    api.put<TechnicianDto>(`/technicians/${id}`, payload).then((r) => r.data),
  remove: (id: number) => api.delete(`/technicians/${id}`).then(() => undefined),
};

export const categoriesApi = {
  list: () => api.get<CategoryDto[]>("/categories").then((r) => r.data),
  create: (payload: { name: string; description?: string; color?: string; icon?: string }) =>
    api.post<CategoryDto>("/categories", payload).then((r) => r.data),
  update: (id: number, payload: { name: string; description?: string; color?: string; icon?: string }) =>
    api.put<CategoryDto>(`/categories/${id}`, payload).then((r) => r.data),
  remove: (id: number) => api.delete(`/categories/${id}`).then(() => undefined),
};