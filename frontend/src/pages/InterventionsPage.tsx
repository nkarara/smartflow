import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { useState } from "react";
import { interventionsApi } from "../api/interventions";
import { EmptyState, PriorityBadge, Spinner, StatusBadge, formatDate } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import type { Priority, Status } from "../types";

const STATUS_OPTIONS = ["", "NOUVELLE", "ASSIGNED", "ACCEPTED", "IN_PROGRESS", "BLOCKED", "RESOLVED", "CLOSED"] as const;
const PRIORITY_OPTIONS = ["", "LOW", "MEDIUM", "HIGH", "URGENT"] as const;

export function InterventionsPage() {
  const { user } = useAuth();
  const [status, setStatus] = useState<string>("");
  const [priority, setPriority] = useState<string>("");
  const { data, isLoading, isError } = useQuery({
    queryKey: ["interventions"],
    queryFn: interventionsApi.list,
  });

  const items = (data ?? []).filter(
    (i) => (status === "" || i.status === (status as Status)) && (priority === "" || i.priority === (priority as Priority)),
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Interventions</h2>
          <p className="text-sm text-slate-500">Consultez et gérez les interventions.</p>
        </div>
        {user && ["CLIENT", "MANAGER", "ADMIN"].includes(user.role) && (
          <Link
            to="/interventions/new"
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
          >
            + Nouvelle intervention
          </Link>
        )}
      </div>

      <div className="flex flex-wrap gap-3">
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
        >
          {STATUS_OPTIONS.map((s) => (
            <option key={s} value={s}>
              {s === "" ? "Tous les statuts" : s}
            </option>
          ))}
        </select>
        <select
          value={priority}
          onChange={(e) => setPriority(e.target.value)}
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
        >
          {PRIORITY_OPTIONS.map((p) => (
            <option key={p} value={p}>
              {p === "" ? "Toutes les priorités" : p}
            </option>
          ))}
        </select>
      </div>

      {isLoading && <Spinner label="Chargement des interventions..." />}
      {isError && <EmptyState message="Impossible de charger les interventions." />}
      {!isLoading && !isError && items.length === 0 && <EmptyState message="Aucune intervention trouvée." />}

      {!isLoading && !isError && items.length > 0 && (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-100 bg-slate-50 text-left text-xs uppercase text-slate-400">
              <tr>
                <th className="px-5 py-3">Titre</th>
                <th className="px-3 py-3">Client</th>
                <th className="px-3 py-3">Technicien</th>
                <th className="px-3 py-3">Catégorie</th>
                <th className="px-3 py-3">Priorité</th>
                <th className="px-3 py-3">Statut</th>
                <th className="px-3 py-3">Créée le</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id} className="border-b border-slate-50 transition hover:bg-slate-50">
                  <td className="px-5 py-3">
                    <Link to={`/interventions/${item.id}`} className="font-medium text-indigo-700 hover:underline">
                      {item.title}
                    </Link>
                    {item.location && <p className="text-xs text-slate-400">{item.location}</p>}
                  </td>
                  <td className="px-3 py-3 text-slate-600">{item.clientName}</td>
                  <td className="px-3 py-3 text-slate-600">{item.technicianName ?? "—"}</td>
                  <td className="px-3 py-3 text-slate-600">{item.categoryName}</td>
                  <td className="px-3 py-3">
                    <PriorityBadge priority={item.priority} />
                  </td>
                  <td className="px-3 py-3">
                    <StatusBadge status={item.status} />
                  </td>
                  <td className="px-3 py-3 text-xs text-slate-500">{formatDate(item.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}