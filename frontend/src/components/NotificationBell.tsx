import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { notificationsApi } from "../api/misc";
import { formatDate } from "./ui";
import { useAuth } from "../context/AuthContext";

/** Cloche de notifications avec compte non-lu et menu déroulant. */
export function NotificationBell() {
  const { user } = useAuth();
  const [open, setOpen] = useState(false);
  const queryClient = useQueryClient();

  const { data: unread = 0 } = useQuery({
    queryKey: ["notifications", "unread"],
    queryFn: () => notificationsApi.unreadCount(),
    refetchInterval: 15_000,
    enabled: !!user,
  });

  const { data: items = [] } = useQuery({
    queryKey: ["notifications"],
    queryFn: () => notificationsApi.list(false),
    enabled: open,
  });

  if (!user) return null;

  const markAllRead = async () => {
    await notificationsApi.markAllRead();
    queryClient.invalidateQueries({ queryKey: ["notifications"] });
  };

  return (
    <div className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className="relative rounded-lg p-2 text-slate-600 transition hover:bg-slate-100"
        aria-label="Notifications"
      >
        🔔
        {unread > 0 && (
          <span className="absolute -right-0.5 -top-0.5 rounded-full bg-rose-600 px-1.5 text-xs font-bold text-white">
            {unread > 9 ? "9+" : unread}
          </span>
        )}
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
          <div className="absolute right-0 z-20 mt-2 w-96 max-w-[90vw] overflow-hidden rounded-xl border border-slate-200 bg-white shadow-lg">
            <div className="flex items-center justify-between border-b border-slate-100 px-4 py-2.5">
              <h3 className="text-sm font-semibold text-slate-800">Notifications</h3>
              <button onClick={markAllRead} className="text-xs font-medium text-indigo-600 hover:underline">
                Tout marquer lu
              </button>
            </div>
            <div className="max-h-96 overflow-y-auto">
              {items.length === 0 && <p className="px-4 py-8 text-center text-sm text-slate-500">Aucune notification</p>}
              {items.slice(0, 20).map((n) => (
                <Link
                  key={n.id}
                  to={n.relatedInterventionId ? `/interventions/${n.relatedInterventionId}` : "#"}
                  onClick={() => {
                    if (!n.read) notificationsApi.markRead(n.id).then(() => queryClient.invalidateQueries({ queryKey: ["notifications"] }));
                    setOpen(false);
                  }}
                  className={`block border-b border-slate-50 px-4 py-3 transition hover:bg-slate-50 ${n.read ? "" : "bg-indigo-50/50"}`}
                >
                  <p className="text-sm text-slate-800">{n.message}</p>
                  <p className="mt-0.5 text-xs text-slate-400">{formatDate(n.createdAt)}</p>
                </Link>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}