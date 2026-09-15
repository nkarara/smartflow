import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  PieChart,
  Pie,
  Cell,
  Legend,
} from "recharts";
import { dashboardApi } from "../api/misc";
import { interventionsApi } from "../api/interventions";
import { useAuth } from "../context/AuthContext";
import { EmptyState, Spinner, StatCard, StatusBadge, formatMinutes } from "../components/ui";
import type { NameCount } from "../types";

const CHART_COLORS = ["#6366f1", "#3b82f6", "#f97316", "#ef4444", "#10b981", "#8b5cf6"];

export function DashboardPage() {
  const { user } = useAuth();
  if (!user) return null;
  if (user.role === "CLIENT") return <ClientDashboard />;
  if (user.role === "TECHNICIAN") return <TechnicianDashboard />;
  return <AdminDashboard />;
}

function StatGrid({ stats }: { stats: { label: string; value: string | number; icon: string }[] }) {
  return (
    <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
      {stats.map((s) => (
        <StatCard key={s.label} {...s} />
      ))}
    </div>
  );
}

function byMonthChart(byMonth: { month: string; count: number }[]) {
  return byMonth.map((m) => ({ ...m, label: m.month.slice(0, 7) }));
}

function AdminDashboard() {
  const isAdmin = useAuth().user?.role === "ADMIN";
  const { data, isLoading } = useQuery({
    queryKey: ["dashboard", isAdmin ? "ADMIN" : "MANAGER"],
    queryFn: () => (isAdmin ? dashboardApi.admin() : dashboardApi.manager()),
  });

  if (isLoading) return <Spinner label="Chargement des statistiques..." />;
  if (!data) return <EmptyState message="Aucune statistique disponible" />;

  const admin = isAdmin ? (data as import("../types").AdminStats) : null;
  const manager = isAdmin ? null : (data as import("../types").ManagerStats);
  const byMonth = admin?.byMonth ?? [];
  const byPriority: NameCount[] = manager?.byPriority ?? admin?.byPriority ?? [];
  const techPerf = manager?.technicianPerformance ?? admin?.technicianPerformance ?? [];
  const totals = (isAdmin ? admin! : manager!) as typeof data;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-800">Tableau de bord {isAdmin ? "administrateur" : "manager"}</h2>
        <p className="text-sm text-slate-500">Vue d'ensemble des interventions et de l'activité des équipes.</p>
      </div>

      <StatGrid
        stats={[
          { label: "Interventions", value: totals.totalInterventions, icon: "🔧" },
          { label: "En cours", value: totals.inProgress, icon: "⚙️" },
          { label: "Urgentes", value: totals.urgent, icon: "🚨" },
          { label: "Clôturées", value: totals.closed, icon: "✅" },
        ]}
      />

      {admin && (
        <StatGrid
          stats={[
            { label: "Taux de résolution", value: `${admin.resolutionRate.toFixed(1)} %`, icon: "📈" },
            { label: "Durée moyenne", value: formatMinutes(Math.round((admin.avgResolutionHours ?? 0) * 60)), icon: "⏱️" },
            { label: "Utilisateurs", value: admin.totalUsers, icon: "👥" },
            { label: "Techniciens", value: admin.totalTechnicians, icon: "👷" },
          ]}
        />
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 className="mb-4 text-sm font-semibold text-slate-700">Interventions par mois</h3>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={byMonthChart(byMonth)}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="label" tick={{ fontSize: 12 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
              <Tooltip />
              <Bar dataKey="count" name="Interventions" fill="#6366f1" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <h3 className="mb-4 text-sm font-semibold text-slate-700">Interventions par priorité</h3>
          <ResponsiveContainer width="100%" height={240}>
            <PieChart>
              <Pie data={byPriority} dataKey="count" nameKey="name" cx="50%" cy="50%" outerRadius={80} label>
                {byPriority.map((_, index) => (
                  <Cell key={index} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                ))}
              </Pie>
              <Legend />
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="mb-4 text-sm font-semibold text-slate-700">Performance des techniciens</h3>
        {techPerf.length === 0 ? (
          <p className="text-sm text-slate-400">Aucune donnée de performance.</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 text-left text-xs uppercase text-slate-400">
                <th className="py-2 pr-4">Technicien</th>
                <th className="py-2 pr-4">Terminées</th>
                <th className="py-2 pr-4">En cours</th>
                <th className="py-2">Temps moyen</th>
              </tr>
            </thead>
            <tbody>
              {techPerf.map((t) => (
                <tr key={t.technicianName} className="border-b border-slate-50">
                  <td className="py-2 pr-4 font-medium text-slate-700">{t.technicianName}</td>
                  <td className="py-2 pr-4 text-slate-600">{t.completed}</td>
                  <td className="py-2 pr-4 text-slate-600">{t.active}</td>
                  <td className="py-2 text-slate-600">{formatMinutes(Math.round((t.avgHours ?? 0) * 60))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

function ClientDashboard() {
  const { data, isLoading } = useQuery({ queryKey: ["interventions"], queryFn: interventionsApi.list });
  if (isLoading) return <Spinner label="Chargement..." />;
  const items = data ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-slate-800">Mes demandes</h2>
          <p className="text-sm text-slate-500">Suivez l'avancement de vos interventions.</p>
        </div>
        <Link
          to="/interventions/new"
          className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
        >
          + Nouvelle demande
        </Link>
      </div>

      <StatGrid
        stats={[
          { label: "Total", value: items.length, icon: "📋" },
          {
            label: "En cours",
            value: items.filter((i) => ["ASSIGNED", "ACCEPTED", "IN_PROGRESS"].includes(i.status)).length,
            icon: "⚙️",
          },
          {
            label: "Résolues",
            value: items.filter((i) => ["RESOLVED", "CLOSED"].includes(i.status)).length,
            icon: "✅",
          },
          { label: "Non affectées", value: items.filter((i) => i.status === "NOUVELLE").length, icon: "⏳" },
        ]}
      />

      {items.length === 0 ? (
        <EmptyState message="Aucune intervention pour le moment. Créez votre première demande !" />
      ) : (
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          {items.map((item) => (
            <Link
              key={item.id}
              to={`/interventions/${item.id}`}
              className="flex items-center justify-between gap-4 border-b border-slate-50 px-5 py-4 transition hover:bg-slate-50"
            >
              <div>
                <p className="font-medium text-slate-800">{item.title}</p>
                <p className="text-xs text-slate-500">
                  {item.categoryName} · {item.location ?? "Localisation non précisée"}
                </p>
              </div>
              <StatusBadge status={item.status} />
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}

function TechnicianDashboard() {
  const { data, isLoading } = useQuery({
    queryKey: ["dashboard", "technician"],
    queryFn: dashboardApi.technician,
  });
  if (isLoading) return <Spinner label="Chargement..." />;
  if (!data) return <EmptyState message="Aucune statistique" />;

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-800">Mon tableau de bord</h2>
        <p className="text-sm text-slate-500">Votre activité et vos interventions.</p>
      </div>

      <StatGrid
        stats={[
          { label: "Mes interventions", value: data.myInterventions, icon: "🔧" },
          { label: "Aujourd'hui", value: data.today, icon: "📅" },
          { label: "En cours", value: data.inProgress, icon: "⚙️" },
          { label: "Terminées", value: data.completed, icon: "🏁" },
        ]}
      />

      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="mb-1 text-sm font-semibold text-slate-700">Temps moyen d'intervention</h3>
        <p className="text-3xl font-bold text-indigo-600">
          {data.avgInterventionHours === null ? "—" : `${data.avgInterventionHours} h`}
        </p>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="mb-4 text-sm font-semibold text-slate-700">Interventions par mois</h3>
        <ResponsiveContainer width="100%" height={240}>
          <BarChart data={byMonthChart(data.byMonth)}>
            <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
            <XAxis dataKey="label" tick={{ fontSize: 12 }} />
            <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
            <Tooltip />
            <Bar dataKey="count" name="Interventions" fill="#10b981" radius={[6, 6, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}