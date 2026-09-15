import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { categoriesApi } from "../api/admin";
import { apiErrorMessage } from "../api/client";
import { EmptyState, ErrorBanner, Spinner } from "../components/ui";

export function AdminCategoriesPage() {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["categories"], queryFn: categoriesApi.list });
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState({ name: "", description: "", color: "#6366f1", icon: "📋" });

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["categories"] });

  const createMutation = useMutation({
    mutationFn: categoriesApi.create,
    onSuccess: () => {
      refresh();
      setShowForm(false);
      setError("");
    },
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const remove = (id: number) => {
    if (window.confirm("Supprimer cette catégorie ?")) {
      categoriesApi.remove(id).then(refresh).catch(() => window.alert("Impossible : des interventions y sont rattachées."));
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
          <h2 className="text-xl font-bold text-slate-800">Catégories</h2>
          <p className="text-sm text-slate-500">Les catégories servent à classer les interventions.</p>
        </div>
        <button
          onClick={() => setShowForm((v) => !v)}
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
        >
          + Nouvelle catégorie
        </button>
      </div>

      <ErrorBanner message={error} />

      {showForm && (
        <form onSubmit={handleCreate} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="grid gap-3 md:grid-cols-2">
            <input required placeholder="Nom" value={form.name} onChange={set("name")} className={inputClass} />
            <input placeholder="Icône emoji" value={form.icon} onChange={set("icon")} className={inputClass} />
            <input type="color" value={form.color} onChange={set("color")} className={inputClass} />
            <textarea placeholder="Description" value={form.description} onChange={set("description")} className={inputClass} rows={2} />
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
      {data && data.length === 0 && <EmptyState message="Aucune catégorie." />}
      {data && data.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {data.map((c) => (
            <div key={c.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center justify-between">
                <span className="text-3xl">{c.icon ?? "📋"}</span>
                <button onClick={() => remove(c.id)} className="rounded border border-red-200 px-2 py-1 text-xs text-red-600 hover:bg-red-50">
                  Supprimer
                </button>
              </div>
              <p className="mt-3 font-semibold text-slate-800">{c.name}</p>
              <p className="text-sm text-slate-500">{c.description ?? ""}</p>
              <div className="mt-3 h-1 w-10 rounded-full" style={{ backgroundColor: c.color ?? "#6366f1" }} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}