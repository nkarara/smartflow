import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { clientsApi } from "../api/admin";
import { apiErrorMessage } from "../api/client";
import { EmptyState, ErrorBanner, Spinner } from "../components/ui";

export function AdminClientsPage() {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["clients"], queryFn: clientsApi.list });
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    email: "",
    password: "",
    firstName: "",
    lastName: "",
    phone: "",
    companyName: "",
    address: "",
    city: "",
    siret: "",
  });

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["clients"] });

  const createMutation = useMutation({
    mutationFn: clientsApi.create,
    onSuccess: () => {
      refresh();
      setShowForm(false);
      setError("");
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const remove = (id: number) => {
    if (window.confirm("Supprimer ce client ?")) {
      clientsApi.remove(id).then(refresh);
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
          <h2 className="text-xl font-bold text-slate-800">Gestion des clients</h2>
          <p className="text-sm text-slate-500">Comptes clients et sociétés.</p>
        </div>
        <button
          onClick={() => setShowForm((v) => !v)}
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
        >
          + Ajouter un client
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
            <input required placeholder="Société" value={form.companyName} onChange={set("companyName")} className={inputClass} />
            <input placeholder="Téléphone" value={form.phone} onChange={set("phone")} className={inputClass} />
            <input placeholder="Adresse" value={form.address} onChange={set("address")} className={inputClass} />
            <input placeholder="Ville" value={form.city} onChange={set("city")} className={inputClass} />
            <input placeholder="SIRET" value={form.siret} onChange={set("siret")} className={inputClass} />
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
      {data && data.length === 0 && <EmptyState message="Aucun client." />}
      {data && data.length > 0 && (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table className="w-full text-sm">
            <thead className="border-b border-slate-100 bg-slate-50 text-left text-xs uppercase text-slate-400">
              <tr>
                <th className="px-5 py-3">Société</th>
                <th className="px-3 py-3">Contact</th>
                <th className="px-3 py-3">Email</th>
                <th className="px-3 py-3">Ville</th>
                <th className="px-3 py-3">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.map((c) => (
                <tr key={c.id} className="border-b border-slate-50">
                  <td className="px-5 py-3 font-medium text-slate-700">{c.companyName ?? "—"}</td>
                  <td className="px-3 py-3 text-slate-600">
                    {c.firstName} {c.lastName}
                    <p className="text-xs text-slate-400">{c.phone ?? ""}</p>
                  </td>
                  <td className="px-3 py-3 text-slate-600">{c.email}</td>
                  <td className="px-3 py-3 text-slate-600">{c.city ?? "—"}</td>
                  <td className="px-3 py-3">
                    <button onClick={() => remove(c.id)} className="rounded border border-red-200 px-2 py-1 text-xs text-red-600 hover:bg-red-50">
                      Supprimer
                    </button>
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