import { useMutation, useQuery } from "@tanstack/react-query";
import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { aiApi } from "../api/misc";
import { interventionsApi, type CreateInterventionPayload } from "../api/interventions";
import { apiErrorMessage } from "../api/client";
import { clientsApi, categoriesApi } from "../api/admin";
import { useAuth } from "../context/AuthContext";
import { ErrorBanner } from "../components/ui";
import type { AIAnalysis, Priority } from "../types";

export function InterventionCreatePage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const canPickClient = user && ["ADMIN", "MANAGER"].includes(user.role);

  const { data: categories = [] } = useQuery({ queryKey: ["categories"], queryFn: categoriesApi.list });
  const { data: clients = [] } = useQuery({
    queryKey: ["clients"],
    queryFn: clientsApi.list,
    enabled: canPickClient === true,
  });

  const [error, setError] = useState("");
  const [ai, setAi] = useState<AIAnalysis | null>(null);
  const [form, setForm] = useState({
    clientId: "",
    title: "",
    description: "",
    categoryId: "",
    priority: "MEDIUM" as Priority,
    location: "",
    plannedDate: "",
    estimatedTimeMinutes: "",
  });

  const set = (key: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
      setForm((f) => ({ ...f, [key]: e.target.value }));

  const applyAi = (analysis: AIAnalysis) => {
    setForm((f) => ({
      ...f,
      categoryId: analysis.categoryId ? String(analysis.categoryId) : f.categoryId,
      priority: analysis.priority,
      estimatedTimeMinutes: String(analysis.estimatedTimeMinutes || f.estimatedTimeMinutes),
    }));
    setAi(analysis);
  };

  const analyze = async () => {
    setError("");
    try {
      applyAi(await aiApi.analyze(form.title, form.description));
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  };

  const createMutation = useMutation({
    mutationFn: (payload: CreateInterventionPayload) => interventionsApi.create(payload),
    onSuccess: (intervention) => navigate(`/interventions/${intervention.id}`),
    onError: (err) => setError(apiErrorMessage(err)),
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    createMutation.mutate({
      clientId: canPickClient && form.clientId ? Number(form.clientId) : null,
      title: form.title,
      description: form.description,
      categoryId: Number(form.categoryId),
      priority: form.priority,
      location: form.location || undefined,
      plannedDate: form.plannedDate ? new Date(form.plannedDate).toISOString() : undefined,
      estimatedTimeMinutes: form.estimatedTimeMinutes ? Number(form.estimatedTimeMinutes) : undefined,
    });
  };

  const inputClass = "w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500";

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-800">Nouvelle intervention</h2>
        <p className="text-sm text-slate-500">Utilisez l'assistant IA pour classer automatiquement votre demande.</p>
      </div>

      <ErrorBanner message={error} />

      <form onSubmit={handleSubmit} className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Titre</label>
          <input required value={form.title} onChange={set("title")} className={inputClass} placeholder="Ordinateur en panne" />
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Description du problème</label>
          <textarea required value={form.description} onChange={set("description")} rows={4} className={inputClass}
            placeholder="Mon ordinateur ne démarre plus depuis ce matin..." />
        </div>

        <button type="button" onClick={analyze} className="rounded-lg border border-violet-200 px-4 py-2 text-sm font-semibold text-violet-600 hover:bg-violet-50">
          🤖 Analyser avec l'IA
        </button>

        {ai && (
          <div className="rounded-xl border border-violet-200 bg-violet-50 p-4 text-sm text-slate-700">
            <p className="text-xs font-semibold text-violet-500">Analyse IA ({ai.analyzer}) :</p>
            <p className="mt-1">
              Catégorie : <span className="font-medium">{ai.category}</span> · Type : {ai.type} · Priorité :{" "}
              <span className="font-medium">{ai.priority}</span> · Temps estimé : {ai.estimatedTimeMinutes} min
            </p>
            <p className="mt-1 text-slate-500">Problème probable : {ai.probableProblem}</p>
          </div>
        )}

        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Catégorie</label>
            <select required value={form.categoryId} onChange={set("categoryId")} className={inputClass}>
              <option value="">Choisir...</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.icon ?? "📋"} {c.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Priorité</label>
            <select value={form.priority} onChange={set("priority")} className={inputClass}>
              <option value="LOW">Basse</option>
              <option value="MEDIUM">Moyenne</option>
              <option value="HIGH">Haute</option>
              <option value="URGENT">Urgente</option>
            </select>
          </div>
          {canPickClient && (
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Client</label>
              <select required value={form.clientId} onChange={set("clientId")} className={inputClass}>
                <option value="">Choisir...</option>
                {clients.map((c) => (
                  <option key={c.id} value={c.id}>{c.companyName ?? c.email}</option>
                ))}
              </select>
            </div>
          )}
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Localisation</label>
            <input value={form.location} onChange={set("location")} className={inputClass} placeholder="Paris" />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Date souhaitée</label>
            <input type="datetime-local" value={form.plannedDate} onChange={set("plannedDate")} className={inputClass} />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Temps estimé (minutes)</label>
            <input type="number" min={0} value={form.estimatedTimeMinutes} onChange={set("estimatedTimeMinutes")} className={inputClass} placeholder="60" />
          </div>
        </div>

        <button type="submit" disabled={createMutation.isPending || !form.categoryId}
          className="w-full rounded-lg bg-indigo-600 py-2.5 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50">
          {createMutation.isPending ? "Création..." : "Créer l'intervention"}
        </button>
      </form>
    </div>
  );
}