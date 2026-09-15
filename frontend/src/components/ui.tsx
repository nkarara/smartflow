import type { Priority, Status } from "../types";

export const STATUS_LABELS: Record<Status, { label: string; className: string }> = {
  NOUVELLE: { label: "Nouvelle", className: "bg-sky-100 text-sky-700" },
  ASSIGNED: { label: "Assignée", className: "bg-indigo-100 text-indigo-700" },
  ACCEPTED: { label: "Acceptée", className: "bg-violet-100 text-violet-700" },
  IN_PROGRESS: { label: "En cours", className: "bg-amber-100 text-amber-700" },
  BLOCKED: { label: "Bloquée", className: "bg-red-100 text-red-700" },
  RESOLVED: { label: "Résolue", className: "bg-emerald-100 text-emerald-700" },
  CLOSED: { label: "Clôturée", className: "bg-slate-200 text-slate-600" },
};

export const PRIORITY_LABELS: Record<Priority, { label: string; className: string }> = {
  LOW: { label: "Basse", className: "bg-slate-100 text-slate-600" },
  MEDIUM: { label: "Moyenne", className: "bg-blue-100 text-blue-700" },
  HIGH: { label: "Haute", className: "bg-orange-100 text-orange-700" },
  URGENT: { label: "Urgente", className: "bg-rose-100 text-rose-700" },
};

export function StatusBadge({ status }: { status: Status }) {
  const s = STATUS_LABELS[status];
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${s.className}`}>
      {s.label}
    </span>
  );
}

export function PriorityBadge({ priority }: { priority: Priority }) {
  const p = PRIORITY_LABELS[priority];
  return (
    <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${p.className}`}>
      {p.label}
    </span>
  );
}

export function Spinner({ label }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-10 text-slate-500">
      <div className="h-6 w-6 animate-spin rounded-full border-2 border-slate-300 border-t-indigo-600" />
      {label && <span>{label}</span>}
    </div>
  );
}

export function EmptyState({ message }: { message: string }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 bg-white py-12 text-center text-slate-500">
      {message}
    </div>
  );
}

export function ErrorBanner({ message }: { message: string }) {
  if (!message) return null;
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{message}</div>
  );
}

export function StatCard({ label, value, icon }: { label: string; value: string | number; icon?: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-slate-500">{label}</p>
          <p className="mt-1 text-2xl font-bold text-slate-900">{value}</p>
        </div>
        {icon && <span className="text-3xl">{icon}</span>}
      </div>
    </div>
  );
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("fr-FR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatMinutes(minutes: number | null | undefined): string {
  if (minutes === null || minutes === undefined) return "—";
  if (minutes < 60) return `${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m === 0 ? `${h} h` : `${h} h ${m.toString().padStart(2, "0")}`;
}