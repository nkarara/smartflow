import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-slate-100">
      <p className="text-6xl">🧭</p>
      <h1 className="text-2xl font-bold text-slate-800">Page introuvable</h1>
      <Link to="/" className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700">
        Retour à l'accueil
      </Link>
    </div>
  );
}