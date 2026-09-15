import { api } from "./client";
import type { AIAnalysis, AISummary, AdminStats, ManagerStats, NotificationDto, TechnicianStats } from "../types";

export const notificationsApi = {
  list: (unreadOnly = false) =>
    api.get<NotificationDto[]>("/notifications", { params: { unreadOnly } }).then((r) => r.data),
  unreadCount: () => api.get<{ count: number }>("/notifications/unread-count").then((r) => r.data.count),
  markRead: (id: number) => api.put(`/notifications/${id}/read`).then(() => undefined),
  markAllRead: () => api.put("/notifications/read-all").then(() => undefined),
};

export const dashboardApi = {
  admin: () => api.get<AdminStats>("/dashboard/admin").then((r) => r.data),
  manager: () => api.get<ManagerStats>("/dashboard/manager").then((r) => r.data),
  technician: () => api.get<TechnicianStats>("/dashboard/technician").then((r) => r.data),
};

export const aiApi = {
  analyze: (title: string, description: string) =>
    api.post<AIAnalysis>("/ai/analyze", { title, description }).then((r) => r.data),
  summarize: (reportText: string, title?: string, actions?: string) =>
    api.post<AISummary>("/ai/summarize", { reportText, title, actions }).then((r) => r.data),
};