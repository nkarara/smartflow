import { Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { DashboardPage } from "./pages/DashboardPage";
import { InterventionsPage } from "./pages/InterventionsPage";
import { InterventionDetailPage } from "./pages/InterventionDetailPage";
import { InterventionCreatePage } from "./pages/InterventionCreatePage";
import { AdminUsersPage } from "./pages/AdminUsersPage";
import { AdminClientsPage } from "./pages/AdminClientsPage";
import { AdminTechniciansPage } from "./pages/AdminTechniciansPage";
import { AdminCategoriesPage } from "./pages/AdminCategoriesPage";
import { AdminSettingsPage } from "./pages/AdminSettingsPage";
import { NotFoundPage } from "./pages/NotFoundPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/interventions" element={<InterventionsPage />} />
        <Route path="/interventions/new" element={<InterventionCreatePage />} />
        <Route path="/interventions/:id" element={<InterventionDetailPage />} />
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute roles={["ADMIN"]}>
              <AdminUsersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/clients"
          element={
            <ProtectedRoute roles={["ADMIN", "MANAGER"]}>
              <AdminClientsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/technicians"
          element={
            <ProtectedRoute roles={["ADMIN", "MANAGER"]}>
              <AdminTechniciansPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/categories"
          element={
            <ProtectedRoute roles={["ADMIN"]}>
              <AdminCategoriesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin/settings"
          element={
            <ProtectedRoute roles={["ADMIN"]}>
              <AdminSettingsPage />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}