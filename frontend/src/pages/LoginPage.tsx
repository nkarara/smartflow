import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authApi } from "../api/auth";
import { apiErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { ErrorBanner } from "../components/ui";

export function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const response = await authApi.login(email, password);
      login(response.accessToken, response.refreshToken, response.user);
      navigate("/");
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-indigo-700">SmartFlow</h1>
          <p className="mt-1 text-sm text-slate-500">Connexion à votre espace</p>
        </div>
        <ErrorBanner message={error} />
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500"
              placeholder="vous@entreprise.fr"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Mot de passe</label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500"
              placeholder="••••••••"
            />
          </div>
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-indigo-600 py-2.5 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:opacity-50"
          >
            {submitting ? "Connexion..." : "Se connecter"}
          </button>
        </form>
        <p className="mt-6 text-center text-sm text-slate-500">
          Pas de compte ?{" "}
          <Link to="/register" className="font-medium text-indigo-600 hover:underline">
            Créer un compte
          </Link>
        </p>
        <div className="mt-6 rounded-lg bg-slate-50 p-3 text-xs text-slate-500">
          <p className="mb-1 font-semibold text-slate-600">Comptes de démonstration :</p>
          <p>admin@smartflow.fr / Admin@123 · manager@smartflow.fr / Manager@123</p>
          <p>tech@smartflow.fr / Tech@123 · client@smartflow.fr / Client@123</p>
        </div>
      </div>
    </div>
  );
}