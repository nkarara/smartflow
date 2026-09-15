import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authApi } from "../api/auth";
import { apiErrorMessage } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { ErrorBanner } from "../components/ui";

export function RegisterPage() {
  const [role, setRole] = useState<"CLIENT" | "TECHNICIAN">("CLIENT");
  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    email: "",
    password: "",
    phone: "",
    companyName: "",
    location: "",
    skills: "",
  });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f) => ({ ...f, [key]: e.target.value }));

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const response = await authApi.register({
        email: form.email,
        password: form.password,
        firstName: form.firstName,
        lastName: form.lastName,
        phone: form.phone || undefined,
        role,
        companyName: role === "CLIENT" ? form.companyName : undefined,
        location: form.location || undefined,
        skills: role === "TECHNICIAN" ? form.skills.split(",").map((s) => s.trim()).filter(Boolean) : undefined,
      });
      login(response.accessToken, response.refreshToken, response.user);
      navigate("/");
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const inputClass =
    "w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-indigo-500";

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <div className="w-full max-w-lg rounded-2xl border border-slate-200 bg-white p-8 shadow-sm">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-indigo-700">Créer un compte</h1>
          <p className="mt-1 text-sm text-slate-500">Rejoignez SmartFlow</p>
        </div>
        <ErrorBanner message={error} />

        <div className="mb-4 grid grid-cols-2 gap-2 rounded-lg bg-slate-100 p-1">
          {(["CLIENT", "TECHNICIAN"] as const).map((r) => (
            <button
              key={r}
              type="button"
              onClick={() => setRole(r)}
              className={`rounded-md py-2 text-sm font-medium transition ${
                role === r ? "bg-white text-indigo-700 shadow-sm" : "text-slate-500"
              }`}
            >
              {r === "CLIENT" ? "Je suis un client" : "Je suis un technicien"}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Prénom</label>
              <input required value={form.firstName} onChange={set("firstName")} className={inputClass} />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Nom</label>
              <input required value={form.lastName} onChange={set("lastName")} className={inputClass} />
            </div>
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Email</label>
            <input type="email" required value={form.email} onChange={set("email")} className={inputClass} />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-slate-700">Mot de passe (min. 8 caractères)</label>
            <input type="password" required minLength={8} value={form.password} onChange={set("password")} className={inputClass} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Téléphone</label>
              <input value={form.phone} onChange={set("phone")} className={inputClass} />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Ville / Localisation</label>
              <input value={form.location} onChange={set("location")} className={inputClass} />
            </div>
          </div>
          {role === "CLIENT" ? (
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">Société</label>
              <input value={form.companyName} onChange={set("companyName")} className={inputClass} />
            </div>
          ) : (
            <div>
              <label className="mb-1 block text-sm font-medium text-slate-700">
                Compétences (séparées par des virgules)
              </label>
              <input value={form.skills} onChange={set("skills")} className={inputClass} placeholder="Réseau, Matériel, Impression" />
            </div>
          )}
          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-indigo-600 py-2.5 text-sm font-semibold text-white transition hover:bg-indigo-700 disabled:opacity-50"
          >
            {submitting ? "Création..." : "Créer mon compte"}
          </button>
        </form>
        <p className="mt-6 text-center text-sm text-slate-500">
          Déjà inscrit ?{" "}
          <Link to="/login" className="font-medium text-indigo-600 hover:underline">
            Se connecter
          </Link>
        </p>
      </div>
    </div>
  );
}