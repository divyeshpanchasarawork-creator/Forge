import { lazy, Suspense, useEffect, useRef, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopHeader from './TopHeader';
import ErrorBoundary from '@/components/ui/ErrorBoundary';
import { PageSkeleton } from '@/components/ui/LoadingSkeleton';
import { NAV_SHORTCUTS } from '@/lib/nav';

const CommandPalette = lazy(() => import('./CommandPalette'));

const pageLabels: Record<string, string> = {
  '/app': 'Dashboard',
  '/app/roadmap': 'Roadmap',
  '/app/problems': 'Practice',
  '/app/revision': 'Revision',
  '/app/journal': 'Journal',
  '/app/memory': 'Memory',
  '/app/analytics': 'Analytics',
  '/app/profile': 'Profile',
};

export default function AppLayout() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
    const saved = localStorage.getItem('forge-sidebar-collapsed');
    return saved === 'true';
  });
  const [mobileOpen, setMobileOpen] = useState(false);
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [paletteMounted, setPaletteMounted] = useState(false);
  const [recent, setRecent] = useState<{ path: string; label: string }[]>(() => {
    try {
      const parsed = JSON.parse(localStorage.getItem('forge-recent-pages') || '[]');
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  });
  const location = useLocation();
  const navigate = useNavigate();
  const rootRef = useRef<HTMLDivElement>(null);
  const pathRef = useRef(location.pathname);

  useEffect(() => {
    pathRef.current = location.pathname;
  }, [location.pathname]);

  useEffect(() => {
    window.focus();
    rootRef.current?.focus();
  }, []);

  useEffect(() => {
    const active = document.activeElement as HTMLElement | null;
    if (active && active !== document.body && active !== rootRef.current) {
      active.blur();
    }
    rootRef.current?.focus({ preventScroll: true });
  }, [location.pathname]);

  useEffect(() => {
    const label = pageLabels[location.pathname];
    if (!label) return;
    setRecent((prev) => {
      const next = [{ path: location.pathname, label }, ...prev.filter((r) => r.path !== location.pathname)].slice(0, 5);
      try {
        localStorage.setItem('forge-recent-pages', JSON.stringify(next));
      } catch {
        // ignore storage errors (private mode / quota)
      }
      return next;
    });
  }, [location.pathname]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setPaletteMounted(true);
        setPaletteOpen((o) => !o);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (paletteOpen) return;
      if (e.metaKey || e.ctrlKey || e.altKey || e.isComposing) return;
      const target = e.target as HTMLElement | null;
      const editable =
        target?.tagName === 'INPUT' ||
        target?.tagName === 'TEXTAREA' ||
        target?.tagName === 'SELECT' ||
        target?.isContentEditable;
      if (editable) return;
      const key = e.code?.startsWith('Digit')
        ? e.code.slice(5)
        : e.code?.startsWith('Numpad')
          ? e.code.slice(6)
          : e.key;
      const to = NAV_SHORTCUTS[key];
      if (!to) return;
      if (to !== pathRef.current) navigate(to);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [navigate, paletteOpen]);

  const toggleSidebar = () => {
    setSidebarCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem('forge-sidebar-collapsed', String(next));
      return next;
    });
  };

  const openPalette = () => {
    setPaletteMounted(true);
    setPaletteOpen(true);
  };

  return (
    <div ref={rootRef} tabIndex={-1} className="min-h-screen outline-none">
      <div className="pointer-events-none fixed inset-0 -z-10 bg-dots" aria-hidden="true" />
      <span className="sr-only" role="status" aria-live="polite">
        {pageLabels[location.pathname]}
      </span>
      <TopHeader sidebarCollapsed={sidebarCollapsed} onMenuClick={() => setMobileOpen(true)} onOpenSearch={openPalette} />
      {paletteMounted && (
        <Suspense fallback={null}>
          <CommandPalette open={paletteOpen} onClose={() => setPaletteOpen(false)} recent={recent} />
        </Suspense>
      )}
      <Sidebar
        collapsed={sidebarCollapsed}
        onToggle={toggleSidebar}
        mobileOpen={mobileOpen}
        onMobileClose={() => setMobileOpen(false)}
      />
      <main
        className={`min-h-screen pt-16 transition-all ${
          sidebarCollapsed ? 'lg:ml-20' : 'lg:ml-72'
        }`}
      >
        <div className="p-4 lg:p-8">
          <Suspense fallback={<PageSkeleton />}>
            <ErrorBoundary>
              <Outlet />
            </ErrorBoundary>
          </Suspense>
        </div>
      </main>
    </div>
  );
}
