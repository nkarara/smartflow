import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { usersApi } from "../api/admin";
import { apiErrorMessage } from "../api/client";
import { EmptyState, ErrorBanner, Spinner } from "../components/ui";
import type { Role } from "../types";

const ROLES: Role[] = ["ADMIN", "MANAGER", "TECHNICIAN", "CLIENT"];

export function AdminUsersPage() {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["users"], queryFn: usersApi.list });
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    email: "",
    password: "",
    firstName: "",
    lastName: "",
    phone: "",
    role: "TECHNICIAN" as Role,
    specialty: "",
    location: "",
    companyName: "",
  });

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["users"] });

  const createMutation = useMutation({
    mutationFn: usersApi.create,
    onSuccess: () => {
      refresh();
      setShowForm(false);
      setError("");
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const toggleEnabled = (id: number, enabled: boolean) => usersApi.update(id, { enabled: !enabled }).then(refresh);

  const remove = (id: number) => {
    if (window.confirm("Supprimer cet utilisateur ?")) {
      usersApi.remove(id).then(refresh);
    }
  };

  const handleCreate = (e: FormEvent) => {
    e.preventDefault();
    createMutation.mutate(form);
  };

  const inputClass = "w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500";

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Gestion des utilisateurs</h2>
          <p className="text-sm text-slate-500">Comptes, rôles et activations.</p>
        </div>
        <button
          onClick={() => setShowForm((v) => !v)}
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
        >
          + Créer un utilisateur
        </button>
      </div>

      <ErrorBanner message={error} />

      {showForm && (
        <form onSubmit={handleCreate} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="grid gap-3 md:grid-cols-3">
            <input required type="email" placeholder="Email" value={form.email} onChange={set("email")} className={inputClass} />
            <input required type="password" minLength={8} placeholder="Mot de passe (8+)" value={form.password} onChange={set("password")} className={inputClass} />
            <select value={form.role} onChange={set("role")} className={inputClass}>
              {ROLES.map((r) => (
                <option key={r} value={r}>{r}</option>
              ))}
            </select>
            <input required placeholder="Prénom" value={form.firstName} onChange={set("firstName")} className={inputClass} />
            <input required placeholder="Nom" value={form.lastName} onChange={set("lastName")} className={inputClass} />
            <input placeholder="Téléphone" value={form.phone} onChange={set("phone")} className={inputClass} />
            {form.role === "TECHNICIAN" && (
              <>
                <input placeholder="Spécialité" value={form.specialty} onChange={set("specialty")} className={inputClass} />
                <input placeholder="Localisation" value={form.location} onChange={set("location")} className={inputClass} />
              </>
            )}
            {form.role === "CLIENT" && (
              <input placeholder="Société" value={form.companyName} onChange={set("companyName")} className={inputClass} />
            )}
          </div>
          <div className="mt-4 flex gap-2">
            <button type="submit" disabled={createMutation.isPending} className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50">
              Créer
            </button>
            <button type="button" onClick={() => setShowForm(false)} className="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50">
              Annuler
            </button>
          </div>
        </form>
      )}

      {isLoading && <Spinner label="Chargement..." />}
      {data && data.length === 0 && <EmptyState message="Aucun utilisateur." />}
      {data && data.length > 0 && (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-100 bg-slate-50 text-left text-xs uppercase text-slate-400">
              <tr>
                <th className="px-5 py-3">Utilisateur</th>
                <th className="px-3 py-3">Email</th>
                <th className="px-3 py-3">Rôle</th>
                <th className="px-3 py-3">Statut</th>
                <th className="px-3 py-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.map((u) => (
                <tr key={u.id} className="border-b border-slate-50">
                  <td className="px-5 py-3 font-medium text-slate-700">
                    {u.firstName} {u.lastName}
                    <p className="text-xs text-slate-400">{u.phone ?? ""}</p>
                  </td>
                  <td className="px-3 py-3 text-slate-600">{u.email}</td>
                  <td className="px-3 py-3">
                    <span className="rounded-full bg-indigo-50 px-2 py-0.5 text-xs font-semibold text-indigo-700">{u.role}</span>
                  </td>
                  <td className="px-3 py-3">
                    <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${u.enabled ? "bg-emerald-100 text-emerald-700" : "bg-red-100 text-red-700"}`}>
                      {u.enabled ? "Actif" : "Désactivé"}
                    </span>
                  </td>
                  <td className="px-3 py-3">
                    <div className="flex gap-2">
                      <button onClick={() => toggleEnabled(u.id, u.enabled)} className="rounded border border-slate-200 px-2 py-1 text-xs text-slate-600 hover:bg-slate-50">
                        {u.enabled ? "Désactiver" : "Activer"}
                      </button>
                      <button onClick={() => remove(u.id)} className="rounded border border-red-200 px-2 py-1 text-xs text-red-600 hover:bg-red-50">
                        Supprimer
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}