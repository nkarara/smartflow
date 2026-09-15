import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { aiApi } from "../api/misc";
import { interventionsApi } from "../api/interventions";
import { apiErrorMessage } from "../api/client";
import { techniciansApi } from "../api/admin";
import { useAuth } from "../context/AuthContext";
import { ErrorBanner, Spinner, StatusBadge, PriorityBadge, formatDate, formatMinutes } from "../components/ui";
import type { Status } from "../types";

const STATUS_ORDER: Status[] = ["NOUVELLE", "ASSIGNED", "ACCEPTED", "IN_PROGRESS", "BLOCKED", "RESOLVED", "CLOSED"];

function StatusTimeline({ status }: { status: Status }) {
  const index = STATUS_ORDER.indexOf(status);
  return (
    <div className="flex items-center gap-1">
      {STATUS_ORDER.map((s, i) => (
        <div key={s} className="flex items-center gap-1">
          <span
            className={`h-3 w-3 rounded-full ${i <= index ? "bg-indigo-600" : "bg-slate-200"} ${s === status ? "ring-2 ring-indigo-300" : ""}`}
            title={s}
          />
          {i < STATUS_ORDER.length - 1 && <span className={`h-0.5 w-4 ${i < index ? "bg-indigo-600" : "bg-slate-200"}`} />}
        </div>
      ))}
    </div>
  );
}

export function InterventionDetailPage() {
  const { id } = useParams();
  const interventionId = Number(id);
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [actionError, setActionError] = useState("");
  const [comment, setComment] = useState("");
  const [report, setReport] = useState("");
  const [actualTime, setActualTime] = useState("");
  const [ratingScore, setRatingScore] = useState("5");
  const [ratingComment, setRatingComment] = useState("");
  const [summary, setSummary] = useState("");
  const [assignTech, setAssignTech] = useState("");

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["interventions"] });

  const { data: intervention, isLoading, isError } = useQuery({
    queryKey: ["interventions", interventionId],
    queryFn: () => interventionsApi.get(interventionId),
  });
  const { data: history = [] } = useQuery({
    queryKey: ["interventions", interventionId, "history"],
    queryFn: () => interventionsApi.history(interventionId),
  });
  const { data: comments = [] } = useQuery({
    queryKey: ["interventions", interventionId, "comments"],
    queryFn: () => interventionsApi.comments(interventionId),
  });
  const { data: attachments = [] } = useQuery({
    queryKey: ["interventions", interventionId, "attachments"],
    queryFn: () => interventionsApi.attachments(interventionId),
  });
  const { data: technicians = [] } = useQuery({
    queryKey: ["technicians"],
    queryFn: techniciansApi.list,
    enabled: !!user && ["ADMIN", "MANAGER"].includes(user.role),
  });
  const { data: suggestions = [] } = useQuery({
    queryKey: ["interventions", interventionId, "suggestions"],
    queryFn: () => interventionsApi.suggestions(interventionId),
    enabled: !!user && ["ADMIN", "MANAGER"].includes(user.role),
  });
  const { data: existingRating } = useQuery({
    queryKey: ["interventions", interventionId, "rating"],
    queryFn: () => interventionsApi.rating(interventionId),
    retry: false,
  });

  const statusMutation = useMutation({
    mutationFn: ({ status, comment }: { status: Status; comment?: string }) =>
      interventionsApi.changeStatus(interventionId, status, comment),
    onSuccess: () => {
      invalidate();
      setActionError("");
    },
    onError: (err) => setActionError(apiErrorMessage(err)),
  });

  const assignMutation = useMutation({
    mutationFn: (technicianId: number) => interventionsApi.assign(interventionId, technicianId),
    onSuccess: invalidate,
    onError: (err) => setActionError(apiErrorMessage(err)),
  });

  const accountMutation = useMutation({
    mutationFn: () => interventionsApi.submitAccount(interventionId, Number(actualTime) || 0, report),
    onSuccess: () => {
      invalidate();
      setReport("");
      setActualTime("");
    },
    onError: (err) => setActionError(apiErrorMessage(err)),
  });

  const commentMutation = useMutation({
    mutationFn: (content: string) => interventionsApi.addComment(interventionId, content),
    onSuccess: () => {
      setComment("");
      queryClient.invalidateQueries({ queryKey: ["interventions", interventionId, "comments"] });
    },
    onError: (err) => setActionError(apiErrorMessage(err)),
  });

  const ratingMutation = useMutation({
    mutationFn: () => interventionsApi.rate(interventionId, Number(ratingScore), ratingComment || undefined),
    onSuccess: () => {
      invalidate();
      setRatingComment("");
    },
    onError: (err) => setActionError(apiErrorMessage(err)),
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => interventionsApi.uploadAttachment(interventionId, file),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["interventions", interventionId, "attachments"] }),
    onError: (err) => setActionError(apiErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: () => interventionsApi.remove(interventionId),
    onSuccess: () => (window.location.href = "/interventions"),
    onError: (err) => setActionError(apiErrorMessage(err)),
  });

  if (isLoading) return <Spinner label="Chargement de l'intervention..." />;
  if (isError || !intervention) {
    return (
      <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-700">
        Intervention introuvable ou accès refusé.
        <Link to="/interventions" className="underline">Retour à la liste</Link>
      </div>
    );
  }

  const role = user?.role;
  const canManage = role === "ADMIN" || role === "MANAGER";
  const isAdmin = role === "ADMIN";
  const s = intervention.status;
  const R: { status: Status; label: string; className: string }[] = [];
  const push = (status: Status, label: string, className: string) => R.push({ status, label, className });
  if (role === "TECHNICIAN") {
    if (s === "ASSIGNED") {
      push("ACCEPTED", "Accepter", "bg-emerald-600 text-white");
      push("NOUVELLE", "Refuser", "border border-red-200 text-red-600");
    }
    if (s === "ACCEPTED") push("IN_PROGRESS", "Démarrer", "bg-indigo-600 text-white");
  }
  if (canManage) {
    if (s === "ASSIGNED") push("ACCEPTED", "Accepter", "bg-emerald-600 text-white");
    if (s === "ACCEPTED") push("IN_PROGRESS", "Démarrer", "bg-indigo-600 text-white");
    if (s === "RESOLVED") push("CLOSED", "Clôturer", "bg-slate-800 text-white");
  }
  if (s === "IN_PROGRESS" && (canManage || role === "TECHNICIAN")) {
    push("BLOCKED", "Bloquer", "border border-amber-300 text-amber-700");
    push("RESOLVED", "Résoudre", "bg-emerald-600 text-white");
  }
  if (s === "BLOCKED") {
    push("IN_PROGRESS", "Reprendre", "bg-indigo-600 text-white");
    push("RESOLVED", "Résoudre", "bg-emerald-600 text-white");
  }
  const canAssign = canManage && (s === "NOUVELLE" || s === "ASSIGNED");
  const canLinkAccount = role === "TECHNICIAN" || canManage;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Link to="/interventions" className="text-sm text-indigo-600 hover:underline">← Interventions</Link>
          <h2 className="mt-1 text-xl font-bold text-slate-800">{intervention.title}</h2>
          <p className="text-sm text-slate-500">{intervention.categoryName} · {intervention.location ?? "Sans localisation"}</p>
        </div>
        <div className="flex items-center gap-2">
          <PriorityBadge priority={intervention.priority} />
          <StatusBadge status={intervention.status} />
          {isAdmin && (
            <button
              onClick={() => window.confirm("Supprimer définitivement ?") && deleteMutation.mutate()}
              className="rounded-lg border border-red-200 px-3 py-1.5 text-sm text-red-600 hover:bg-red-50"
            >
              Supprimer
            </button>
          )}
        </div>
      </div>

      <ErrorBanner message={actionError} />

      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="mb-3 text-sm font-semibold text-slate-700">Description</h3>
        <p className="whitespace-pre-wrap text-slate-700">{intervention.description}</p>
        <dl className="mt-4 grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
          <dt className="text-slate-400">Client</dt><dd className="text-slate-700">{intervention.clientName}</dd>
          <dt className="text-slate-400">Technicien</dt><dd className="text-slate-700">{intervention.technicianName ?? "Non affecté"}</dd>
          <dt className="text-slate-400">Temps estimé</dt><dd className="text-slate-700">{formatMinutes(intervention.estimatedTimeMinutes)}</dd>
          <dt className="text-slate-400">Temps réel</dt><dd className="text-slate-700">{formatMinutes(intervention.actualTimeMinutes)}</dd>
          <dt className="text-slate-400">Clôturée le</dt><dd className="text-slate-700">{formatDate(intervention.closedAt)}</dd>
        </dl>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="mb-3 text-sm font-semibold text-slate-700">Statut & actions</h3>
        <StatusTimeline status={intervention.status} />
        <div className="mt-4 flex flex-wrap gap-2">
          {R.map((a) => (
            <button key={a.status} onClick={() => statusMutation.mutate({ status: a.status })} disabled={statusMutation.isPending}
              className={`rounded-lg px-3 py-2 text-sm font-semibold transition disabled:opacity-50 ${a.className}`}>
              {a.label}
            </button>
          ))}
          {R.length === 0 && <p className="text-sm text-slate-400">Aucune action disponible.</p>}
        </div>

        {canAssign && (
          <div className="mt-5 border-t border-slate-100 pt-4">
            <h4 className="text-sm font-semibold text-slate-700">Affecter un technicien</h4>
            <div className="mt-2 space-y-2">
              <select value={assignTech} onChange={(e) => setAssignTech(e.target.value)}
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm">
                <option value="">Choisir un technicien...</option>
                {technicians.map((t) => (
                  <option key={t.id} value={t.id}>{t.firstName} {t.lastName} — {t.location ?? "?"} {t.available ? "" : "(indisponible)"}</option>
                ))}
              </select>
              <button onClick={() => assignTech && assignMutation.mutate(Number(assignTech))} disabled={!assignTech || assignMutation.isPending}
                className="w-full rounded-lg bg-indigo-600 px-3 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50">
                Affecter
              </button>
            </div>
            {suggestions.length > 0 && (
              <div className="mt-3 rounded-lg bg-slate-50 p-3">
                <p className="text-xs font-semibold text-slate-500">🤖 Suggestions automatiques :</p>
                <ul className="mt-1 space-y-1 text-xs text-slate-600">
                  {suggestions.slice(0, 3).map((sg) => (
                    <li key={sg.technicianId}>{sg.technicianName} (score {sg.score})</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}
      </div>

      {canLinkAccount && (
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 className="mb-3 text-sm font-semibold text-slate-700">Compte rendu & temps passé</h3>
          {intervention.report && (
            <div className="mb-4 rounded-lg bg-slate-50 p-3 text-sm text-slate-600">
              <p className="text-xs font-semibold text-slate-400">Compte rendu actuel :</p>
              <p className="whitespace-pre-wrap">{intervention.report}</p>
            </div>
          )}
          <textarea value={report} onChange={(e) => setReport(e.target.value)} rows={4}
            placeholder="Décrivez les actions réalisées..." className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <div className="mt-2 flex flex-wrap items-center gap-3">
            <input type="number" min={0} placeholder="Temps (minutes)" value={actualTime} onChange={(e) => setActualTime(e.target.value)}
              className="w-40 rounded-lg border border-slate-300 px-3 py-2 text-sm" />
            <button onClick={() => accountMutation.mutate()} disabled={accountMutation.isPending}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50">
              Enregistrer le compte rendu
            </button>
            <button
              onClick={() =>
                aiApi.summarize(intervention.report ?? report, intervention.title)
                  .then((r) => setSummary(r.summary))
                  .catch(() => setActionError("Impossible de générer le résumé"))}
              className="rounded-lg border border-violet-200 px-4 py-2 text-sm font-semibold text-violet-600 hover:bg-violet-50"
            >
              ✨ Résumé IA
            </button>
          </div>
          {summary && (
            <div className="mt-3 rounded-lg bg-violet-50 p-3 text-sm text-slate-700">
              <p className="text-xs font-semibold text-violet-500">🤖 Résumé généré :</p>
              <p>{summary}</p>
            </div>
          )}
        </div>
      )}

      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="mb-3 text-sm font-semibold text-slate-700">Commentaires</h3>
        <div className="space-y-3">
          {comments.length === 0 && <p className="text-sm text-slate-400">Aucun commentaire.</p>}
          {comments.map((c) => (
            <div key={c.id} className="rounded-lg bg-slate-50 p-3">
              <p className="text-xs text-slate-400">{c.authorName} — {formatDate(c.createdAt)}</p>
              <p className="mt-1 text-sm text-slate-700">{c.content}</p>
            </div>
          ))}
        </div>
        <div className="mt-4 flex gap-2">
          <input value={comment} onChange={(e) => setComment(e.target.value)}
            placeholder="Ajouter un commentaire..." className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm" />
          <button onClick={() => comment.trim() && commentMutation.mutate(comment.trim())} disabled={commentMutation.isPending}
            className="rounded-lg bg-slate-800 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-700 disabled:opacity-50">
            Envoyer
          </button>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="mb-3 text-sm font-semibold text-slate-700">Pièces jointes</h3>
        <ul className="mb-4 space-y-1 text-sm">
          {attachments.length === 0 && <li className="text-slate-400">Aucune pièce jointe.</li>}
          {attachments.map((a) => (
            <li key={a.id} className="flex items-center justify-between text-slate-600">
              <span>📎 {a.fileName} <span className="text-xs text-slate-400">({Math.round(a.size / 1024)} Ko) par {a.uploadedByName}</span></span>
              <a href={`/api${a.downloadUrl}`} download={a.fileName} className="text-indigo-600 hover:underline">Télécharger</a>
            </li>
          ))}
        </ul>
        <label className="inline-block cursor-pointer rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-50">
          📤 Ajouter un fichier
          <input type="file" className="hidden" onChange={(e) => e.target.files?.[0] && uploadMutation.mutate(e.target.files[0])} />
        </label>
      </div>

      {role === "CLIENT" && (s === "RESOLVED" || s === "CLOSED") && (
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 className="mb-3 text-sm font-semibold text-slate-700">Évaluer l'intervention</h3>
          {existingRating ? (
            <p className="text-sm text-slate-600">
              Votre note : <span className="font-semibold">{existingRating.score}/5</span>{" "}
              {existingRating.comment ? `— ${existingRating.comment}` : ""}
            </p>
          ) : (
            <div className="flex flex-wrap items-center gap-3">
              <select value={ratingScore} onChange={(e) => setRatingScore(e.target.value)}
                className="rounded-lg border border-slate-300 px-3 py-2 text-sm">
                {[5, 4, 3, 2, 1].map((n) => (
                  <option key={n} value={n}>{n} / 5</option>
                ))}
              </select>
              <input value={ratingComment} onChange={(e) => setRatingComment(e.target.value)}
                placeholder="Un commentaire ?" className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm" />
              <button onClick={() => ratingMutation.mutate()} disabled={ratingMutation.isPending}
                className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50">
                Noter
              </button>
            </div>
          )}
        </div>
      )}

      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="mb-3 text-sm font-semibold text-slate-700">Historique</h3>
        <ol className="mt-2 space-y-3 border-l border-slate-200 pl-4 text-sm">
          {history.length === 0 && <li className="text-slate-400">Aucune entrée d'historique.</li>}
          {history.map((h) => (
            <li key={h.id} className="text-slate-600">
              <p>
                <span className="font-medium text-slate-700">{h.fromStatus ? `${h.fromStatus} → ` : ""}{h.toStatus}</span>
                <span className="text-slate-400"> · {h.changedByName} · {formatDate(h.changedAt)}</span>
              </p>
              {h.comment && <p className="text-xs text-slate-500">{h.comment}</p>}
            </li>
          ))}
        </ol>
      </div>
    </div>
  );
}