import { lazy, Suspense, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider, useAuth } from '@/contexts/AuthContext';
import { ThemeProvider } from '@/contexts/ThemeContext';
import ColdStartGate from '@/components/layout/ColdStartGate';
import AppLayout from '@/components/layout/AppLayout';
import ErrorBoundary from '@/components/ui/ErrorBoundary';
import AppBootScreen from '@/components/ui/AppBootScreen';
import { Logo } from '@/components/brand/Logo';

const LandingPage = lazy(() => import('@/pages/LandingPage'));
const OnboardingPage = lazy(() => import('@/pages/OnboardingPage'));
const DashboardPage = lazy(() => import('@/pages/DashboardPage'));
const RoadmapPage = lazy(() => import('@/pages/RoadmapPage'));
const PracticePage = lazy(() => import('@/pages/PracticePage'));
const RevisionPage = lazy(() => import('@/pages/RevisionPage'));
const JournalPage = lazy(() => import('@/pages/JournalPage'));
const AnalyticsPage = lazy(() => import('@/pages/AnalyticsPage'));
const MemoryPage = lazy(() => import('@/pages/MemoryPage'));
const ProfilePage = lazy(() => import('@/pages/ProfilePage'));

function PreloadCorePages() {
  useEffect(() => {
    const timer = setTimeout(() => {
      import('@/pages/DashboardPage');
      import('@/pages/PracticePage');
      import('@/pages/RevisionPage');
    }, 1500);
    return () => clearTimeout(timer);
  }, []);
  return null;
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000,
      gcTime: 30 * 60 * 1000,
      retry: 1,
      refetchOnWindowFocus: true,
    },
  },
});

function RouteLoader() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-background" role="status" aria-label="Loading">
      <Logo size="md" className="animate-pulse" />
    </div>
  );
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, loading, loggingOut } = useAuth();
  if (loading) return <AppBootScreen />;
  if (!isAuthenticated && !loggingOut) return <Navigate to="/" replace />;
  return <>{children}</>;
}

function PublicOnlyRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return <AppBootScreen />;
  if (isAuthenticated) return <Navigate to="/app" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider>
          <AuthProvider>
            <BrowserRouter>
              <PreloadCorePages />
              <ColdStartGate>
                <Suspense fallback={<RouteLoader />}>
                  <Routes>
                    <Route path="/login" element={<Navigate to="/" replace />} />
                    <Route path="/register" element={<Navigate to="/" replace />} />
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
                </Suspense>
              </ColdStartGate>
            </BrowserRouter>
          </AuthProvider>
        </ThemeProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
