import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { settingsApi } from "../api/admin";
import { apiErrorMessage } from "../api/client";
import { EmptyState, ErrorBanner, Spinner } from "../components/ui";

/**
 * Page administrateur « Paramètres de l'application ».
 * Liste les paramètres (clé/valeur) et permet d'en modifier la valeur.
 * L'accès est réservé au rôle ADMIN (ProtectedRoute + @PreAuthorize côté API).
 */
export function AdminSettingsPage() {
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({ queryKey: ["settings"], queryFn: settingsApi.list });
  const [values, setValues] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState<Record<string, boolean>>({});
  const [savedKey, setSavedKey] = useState<string | null>(null);
  const [error, setError] = useState("");

  // Indexe les valeurs par clé pour les inputs contrôlés
  const resolved = data ?? [];

  const setValue = (key: string, value: string) => setValues((v) => ({ ...v, [key]: value }));
  const inputValue = (key: string, fallback: string) => values[key] ?? fallback;

  const save = async (key: string) => {
    const value = (values[key] ?? "").trim();
    if (value === "") {
      setError(`La valeur du paramètre « ${key} » ne peut pas être vide.`);
      return;
    }
    setError("");
    setSaving((s) => ({ ...s, [key]: true }));
    try {
      await settingsApi.update(key, value);
      setSavedKey(key);
      queryClient.invalidateQueries({ queryKey: ["settings"] });
      setTimeout(() => setSavedKey(null), 2000);
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setSaving((s) => ({ ...s, [key]: false }));
    }
  };

  // Regroupe les paramètres par catégorie pour une lecture claire
  const byCategory: Record<string, typeof resolved> = {};
  resolved.forEach((s) => {
    const cat = s.category ?? "Autres";
    (byCategory[cat] = byCategory[cat] ?? []).push(s);
  });

  const inputClass =
    "flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500";

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-800">Paramètres de l'application</h2>
        <p className="text-sm text-slate-500">Modifiez les valeurs de configuration (admin uniquement).</p>
      </div>

      <ErrorBanner message={error} />

      {isLoading && <Spinner label="Chargement des paramètres..." />}
      {!isLoading && resolved.length === 0 && <EmptyState message="Aucun paramètre configuré." />}

      {!isLoading && resolved.length > 0 &&
        Object.entries(byCategory).map(([category, settings]) => (
          <div key={category} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="mb-4 text-sm font-semibold text-slate-700">{category}</h3>
            <div className="space-y-4">
              {settings.map((setting) => (
                <div key={setting.key} className="flex items-center gap-3">
                  <div className="w-56 shrink-0">
                    <p className="text-sm font-medium text-slate-700">{setting.key}</p>
                    <p className="text-xs text-slate-400">{setting.description ?? ""}</p>
                  </div>
                  <input
                    value={inputValue(setting.key, setting.value)}
                    onChange={(e) => setValue(setting.key, e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") save(setting.key);
                    }}
                    className={inputClass}
                    aria-label={setting.key}
                  />
                  <button
                    onClick={() => save(setting.key)}
                    disabled={saving[setting.key] === true}
                    className="shrink-0 rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
                  >
                    {saving[setting.key] ? "…" : "Enregistrer"}
                  </button>
                  {savedKey === setting.key && (
                    <span className="shrink-0 text-xs font-medium text-emerald-600">✓ Enregistré</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        ))}
    </div>
  );
}