import { api } from "./client";
import type {
  AttachmentDto,
  CommentDto,
  HistoryDto,
  InterventionDto,
  Priority,
  RatingDto,
  Status,
  SuggestionDto,
} from "../types";

export interface CreateInterventionPayload {
  clientId?: number | null;
  title: string;
  description: string;
  categoryId: number;
  priority: Priority;
  location?: string;
  plannedDate?: string;
  estimatedTimeMinutes?: number;
}

export const interventionsApi = {
  list: () => api.get<InterventionDto[]>("/interventions").then((r) => r.data),
  get: (id: number) => api.get<InterventionDto>(`/interventions/${id}`).then((r) => r.data),
  create: (payload: CreateInterventionPayload) =>
    api.post<InterventionDto>("/interventions", payload).then((r) => r.data),
  update: (id: number, payload: CreateInterventionPayload) =>
    api.put<InterventionDto>(`/interventions/${id}`, payload).then((r) => r.data),
  remove: (id: number) => api.delete(`/interventions/${id}`).then(() => undefined),
  assign: (id: number, technicianId: number) =>
    api.put<InterventionDto>(`/interventions/${id}/assign`, { technicianId }).then((r) => r.data),
  suggestions: (id: number) =>
    api.get<SuggestionDto[]>(`/interventions/${id}/assign/suggestions`).then((r) => r.data),
  changeStatus: (id: number, newStatus: Status, comment?: string) =>
    api.put<InterventionDto>(`/interventions/${id}/status`, { newStatus, comment }).then((r) => r.data),
  submitAccount: (id: number, actualTimeMinutes: number, report?: string) =>
    api.put<InterventionDto>(`/interventions/${id}/account`, { actualTimeMinutes, report }).then((r) => r.data),
  history: (id: number) => api.get<HistoryDto[]>(`/interventions/${id}/history`).then((r) => r.data),
  comments: (id: number) => api.get<CommentDto[]>(`/interventions/${id}/comments`).then((r) => r.data),
  addComment: (id: number, content: string) =>
    api.post<CommentDto>(`/interventions/${id}/comments`, { content }).then((r) => r.data),
  attachments: (id: number) => api.get<AttachmentDto[]>(`/interventions/${id}/attachments`).then((r) => r.data),
  uploadAttachment: (id: number, file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return api
      .post<AttachmentDto>(`/interventions/${id}/attachments`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },
  rating: (id: number) => api.get<RatingDto>(`/interventions/${id}/rating`).then((r) => r.data),
  rate: (id: number, score: number, comment?: string) =>
    api.post<RatingDto>(`/interventions/${id}/rating`, { score, comment }).then((r) => r.data),
};