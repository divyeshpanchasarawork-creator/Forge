import { useEffect, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import TopHeader from './TopHeader';
import CommandPalette from './CommandPalette';
import { AnimatePresence, motion } from 'framer-motion';

export default function AppLayout() {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(() => {
    const saved = localStorage.getItem('forge-sidebar-collapsed');
    return saved === 'true';
  });
  const [mobileOpen, setMobileOpen] = useState(false);
  const [paletteOpen, setPaletteOpen] = useState(false);
  const [recent, setRecent] = useState<{ path: string; label: string }[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('forge-recent-pages') || '[]');
    } catch {
      return [];
    }
  });
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
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
    const label = pageLabels[location.pathname];
    if (!label) return;
    setRecent((prev) => {
      const next = [{ path: location.pathname, label }, ...prev.filter((r) => r.path !== location.pathname)].slice(0, 5);
      localStorage.setItem('forge-recent-pages', JSON.stringify(next));
      return next;
    });
  }, [location.pathname]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const tag = (e.target as HTMLElement)?.tagName;
      if (tag === 'INPUT' || tag === 'TEXTAREA' || e.metaKey || e.ctrlKey || e.altKey) return;
      const routeByNum: Record<string, string> = {
        '1': '/app',
        '2': '/app/roadmap',
        '3': '/app/problems',
        '4': '/app/revision',
        '5': '/app/journal',
        '6': '/app/memory',
        '7': '/app/analytics',
        '8': '/app/profile',
      };
      const to = routeByNum[e.key];
      if (to && to !== location.pathname) navigate(to);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [location.pathname, navigate]);

  const toggleSidebar = () => {
    setSidebarCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem('forge-sidebar-collapsed', String(next));
      return next;
    });
  };

  return (
    <div className="min-h-screen bg-background">
      <TopHeader sidebarCollapsed={sidebarCollapsed} onMenuClick={() => setMobileOpen(true)} onOpenSearch={() => setPaletteOpen(true)} />
      <CommandPalette open={paletteOpen} onOpen={() => setPaletteOpen(true)} onClose={() => setPaletteOpen(false)} recent={recent} />
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
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.18, ease: 'easeOut' }}
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </div>
      </main>
    </div>
  );
}
