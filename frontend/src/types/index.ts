export type Role = "ADMIN" | "MANAGER" | "TECHNICIAN" | "CLIENT";
export type Status =
  | "NOUVELLE"
  | "ASSIGNED"
  | "ACCEPTED"
  | "IN_PROGRESS"
  | "BLOCKED"
  | "RESOLVED"
  | "CLOSED";
export type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type NotificationType =
  | "NEW_INTERVENTION"
  | "INTERVENTION_ASSIGNED"
  | "INTERVENTION_ACCEPTED"
  | "STATUS_CHANGED"
  | "NEW_COMMENT"
  | "NEW_ATTACHMENT"
  | "NEW_RATING"
  | "INTERVENTION_RESOLVED"
  | "INTERVENTION_CLOSED";

export interface AuthUser {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

export interface UserDto {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  role: Role;
  enabled: boolean;
  createdAt: string;
  clientId: number | null;
  technicianId: number | null;
}

export interface ClientDto {
  id: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  companyName: string | null;
  address: string | null;
  city: string | null;
  siret: string | null;
}

export interface TechnicianDto {
  id: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  specialty: string | null;
  location: string | null;
  available: boolean;
  skills: string[];
}

export interface CategoryDto {
  id: number;
  name: string;
  description: string | null;
  color: string | null;
  icon: string | null;
}

export interface AppSetting {
  key: string;
  value: string;
  description: string | null;
  category: string | null;
}

export interface InterventionDto {
  id: number;
  title: string;
  description: string;
  status: Status;
  priority: Priority;
  categoryId: number;
  categoryName: string;
  clientId: number;
  clientName: string;
  technicianId: number | null;
  technicianName: string | null;
  location: string | null;
  createdAt: string;
  plannedDate: string | null;
  estimatedTimeMinutes: number | null;
  actualTimeMinutes: number | null;
  report: string | null;
  closedAt: string | null;
}

export interface CommentDto {
  id: number;
  authorId: number;
  authorName: string;
  content: string;
  createdAt: string;
}

export interface AttachmentDto {
  id: number;
  interventionId: number;
  fileName: string;
  contentType: string | null;
  size: number;
  uploadedById: number;
  uploadedByName: string;
  uploadedAt: string;
  downloadUrl: string;
}

export interface NotificationDto {
  id: number;
  userId: number;
  type: NotificationType;
  message: string;
  relatedInterventionId: number | null;
  read: boolean;
  createdAt: string;
}

export interface RatingDto {
  id: number;
  interventionId: number;
  score: number;
  comment: string | null;
  createdAt: string;
}

export interface SuggestionDto {
  technicianId: number;
  technicianName: string;
  score: number;
  available: boolean;
  activeInterventions: number;
  reasons: string[];
}

export interface HistoryDto {
  id: number;
  changedById: number | null;
  changedByName: string;
  fromStatus: Status | null;
  toStatus: Status;
  comment: string | null;
  changedAt: string;
}

export interface MonthCount {
  month: string;
  count: number;
}

export interface NameCount {
  name: string;
  count: number;
}

export interface TechnicianPerf {
  technicianName: string;
  completed: number;
  active: number;
  avgHours: number | null;
}

export interface AdminStats {
  totalUsers: number;
  totalClients: number;
  totalTechnicians: number;
  totalInterventions: number;
  inProgress: number;
  urgent: number;
  closed: number;
  resolutionRate: number;
  avgResolutionHours: number | null;
  byMonth: MonthCount[];
  byCategory: NameCount[];
  byPriority: NameCount[];
  technicianPerformance: TechnicianPerf[];
}

export interface ManagerStats {
  totalInterventions: number;
  inProgress: number;
  urgent: number;
  closed: number;
  resolutionRate: number;
  byPriority: NameCount[];
  technicianPerformance: TechnicianPerf[];
}

export interface TechnicianStats {
  myInterventions: number;
  today: number;
  inProgress: number;
  completed: number;
  avgInterventionHours: number | null;
  byMonth: MonthCount[];
}

export interface AIAnalysis {
  category: string;
  categoryId: number | null;
  type: string;
  priority: Priority;
  probableProblem: string;
  estimatedTimeMinutes: number;
  analyzer: string;
}

export interface AISummary {
  summary: string;
  analyzer: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}