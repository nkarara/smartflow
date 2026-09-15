import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { techniciansApi } from "../api/admin";
import { apiErrorMessage } from "../api/client";
import { EmptyState, ErrorBanner, Spinner } from "../components/ui";

export function AdminTechniciansPage() {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["technicians"], queryFn: techniciansApi.list });
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    email: "",
    password: "",
    firstName: "",
    lastName: "",
    phone: "",
    specialty: "",
    location: "",
    skills: "",
  });

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["technicians"] });

  const createMutation = useMutation({
    mutationFn: techniciansApi.create,
    onSuccess: () => {
      refresh();
      setShowForm(false);
      setError("");
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const toggleAvailable = (id: number, available: boolean) =>
    techniciansApi.update(id, { available: !available }).then(refresh);

  const remove = (id: number) => {
    if (window.confirm("Supprimer ce technicien ?")) {
      techniciansApi.remove(id).then(refresh);
    }
  };

  const handleCreate = (e: FormEvent) => {
    e.preventDefault();
    createMutation.mutate({
      ...form,
      skills: form.skills.split(",").map((s) => s.trim()).filter(Boolean),
    });
  };

  const inputClass = "w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500";

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Gestion des techniciens</h2>
          <p className="text-sm text-slate-500">Compétences, disponibilités et localisations.</p>
        </div>
        <button
          onClick={() => setShowForm((v) => !v)}
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
        >
          + Ajouter un technicien
        </button>
      </div>

      <ErrorBanner message={error} />

      {showForm && (
        <form onSubmit={handleCreate} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="grid gap-3 md:grid-cols-3">
            <input required type="email" placeholder="Email" value={form.email} onChange={set("email")} className={inputClass} />
            <input required type="password" minLength={8} placeholder="Mot de passe (8+)" value={form.password} onChange={set("password")} className={inputClass} />
            <input required placeholder="Prénom" value={form.firstName} onChange={set("firstName")} className={inputClass} />
            <input required placeholder="Nom" value={form.lastName} onChange={set("lastName")} className={inputClass} />
            <input placeholder="Téléphone" value={form.phone} onChange={set("phone")} className={inputClass} />
            <input placeholder="Spécialité" value={form.specialty} onChange={set("specialty")} className={inputClass} />
            <input placeholder="Localisation" value={form.location} onChange={set("location")} className={inputClass} />
            <input placeholder="Compétences (virgules)" value={form.skills} onChange={set("skills")} className={inputClass} />
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
      {data && data.length === 0 && <EmptyState message="Aucun technicien." />}
      {data && data.length > 0 && (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-100 bg-slate-50 text-left text-xs uppercase text-slate-400">
              <tr>
                <th className="px-5 py-3">Technicien</th>
                <th className="px-3 py-3">Spécialité</th>
                <th className="px-3 py-3">Localisation</th>
                <th className="px-3 py-3">Compétences</th>
                <th className="px-3 py-3">Disponible</th>
                <th className="px-3 py-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.map((t) => (
                <tr key={t.id} className="border-b border-slate-50">
                  <td className="px-5 py-3 font-medium text-slate-700">
                    {t.firstName} {t.lastName}
                    <p className="text-xs text-slate-400">{t.email}</p>
                  </td>
                  <td className="px-3 py-3 text-slate-600">{t.specialty ?? "—"}</td>
                  <td className="px-3 py-3 text-slate-600">{t.location ?? "—"}</td>
                  <td className="px-3 py-3">
                    <div className="flex flex-wrap gap-1">
                      {t.skills.map((s) => (
                        <span key={s} className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">{s}</span>
                      ))}
                    </div>
                  </td>
                  <td className="px-3 py-3">
                    <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${t.available ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}>
                      {t.available ? "Disponible" : "Indisponible"}
                    </span>
                  </td>
                  <td className="px-3 py-3">
                    <div className="flex gap-2">
                      <button onClick={() => toggleAvailable(t.id, t.available)} className="rounded border border-slate-200 px-2 py-1 text-xs text-slate-600 hover:bg-slate-50">
                        {t.available ? "Marquer indisponible" : "Marquer disponible"}
                      </button>
                      <button onClick={() => remove(t.id)} className="rounded border border-red-200 px-2 py-1 text-xs text-red-600 hover:bg-red-50">
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