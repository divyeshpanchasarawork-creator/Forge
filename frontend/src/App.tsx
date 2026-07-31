import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MotionConfig } from 'framer-motion';
import { AuthProvider, useAuth } from '@/contexts/AuthContext';
import { ThemeProvider } from '@/contexts/ThemeContext';
import ColdStartGate from '@/components/layout/ColdStartGate';
import LandingPage from '@/pages/LandingPage';
import LoginPage from '@/pages/LoginPage';
import RegisterPage from '@/pages/RegisterPage';
import OnboardingPage from '@/pages/OnboardingPage';
import DashboardPage from '@/pages/DashboardPage';
import RoadmapPage from '@/pages/RoadmapPage';
import PracticePage from '@/pages/PracticePage';
import RevisionPage from '@/pages/RevisionPage';
import JournalPage from '@/pages/JournalPage';
import AnalyticsPage from '@/pages/AnalyticsPage';
import MemoryPage from '@/pages/MemoryPage';
import ProfilePage from '@/pages/ProfilePage';
import AppLayout from '@/components/layout/AppLayout';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, loading, loggingOut } = useAuth();
  if (loading) return <div className="flex h-screen items-center justify-center bg-background text-foreground">Loading...</div>;
  if (!isAuthenticated && !loggingOut) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function PublicOnlyRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <div className="flex h-screen items-center justify-center bg-background text-foreground">Loading...</div>;
  if (isAuthenticated) return <Navigate to="/app" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <MotionConfig reducedMotion="user">
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <AuthProvider>
          <BrowserRouter>
            <ColdStartGate>
            <Routes>
              <Route path="/login" element={<PublicOnlyRoute><LoginPage /></PublicOnlyRoute>} />
              <Route path="/register" element={<PublicOnlyRoute><RegisterPage /></PublicOnlyRoute>} />
              <Route path="/" element={<PublicOnlyRoute><LandingPage /></PublicOnlyRoute>} />
              <Route path="/onboarding" element={<ProtectedRoute><OnboardingPage /></ProtectedRoute>} />
              <Route path="/app" element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
                <Route index element={<DashboardPage />} />
                <Route path="roadmap" element={<RoadmapPage />} />
                <Route path="problems" element={<PracticePage />} />
                <Route path="revision" element={<RevisionPage />} />
                <Route path="journal" element={<JournalPage />} />
                <Route path="analytics" element={<AnalyticsPage />} />
                <Route path="memory" element={<MemoryPage />} />
                <Route path="profile" element={<ProfilePage />} />
              </Route>
            </Routes>
            </ColdStartGate>
          </BrowserRouter>
        </AuthProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </MotionConfig>
  );
}
