import { useEffect, useRef, useState } from 'react';
import { useLocation, useNavigate, NavLink } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/Button';
import ThemeToggle from '@/components/ui/ThemeToggle';
import { User, LogOut, Mail, Menu, Search, ChevronDown } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { cn } from '@/lib/utils';

const pageTitles: Record<string, string> = {
  '/app': 'Dashboard',
  '/app/roadmap': 'Roadmap',
  '/app/problems': 'Practice',
  '/app/revision': 'Revision',
  '/app/journal': 'Journal',
  '/app/memory': 'Memory',
  '/app/analytics': 'Analytics',
  '/app/profile': 'Profile',
};

const menuItemClasses =
  'flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground';

export default function TopHeader({ sidebarCollapsed, onMenuClick, onOpenSearch }: { sidebarCollapsed: boolean; onMenuClick: () => void; onOpenSearch: () => void }) {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);
  const title = pageTitles[location.pathname] || 'Forge';

  useEffect(() => {
    if (!menuOpen) return;
    const onClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, [menuOpen]);

  const handleLogout = () => {
    setMenuOpen(false);
    navigate('/', { state: { signedOut: true } });
    logout();
  };

  return (
    <header
      className={cn(
        'fixed top-0 right-0 z-30 flex h-16 items-center gap-4 bg-background/80 px-4 backdrop-blur-sm transition-all lg:px-6',
        sidebarCollapsed ? 'lg:left-20' : 'lg:left-72'
      )}
    >
      <div className="flex min-w-0 flex-1 items-center gap-3">
        <Button variant="ghost" onClick={onMenuClick} className="h-9 w-9 p-0 lg:hidden" aria-label="Open menu">
          <Menu className="h-5 w-5" />
        </Button>
        <h1 className="truncate text-lg font-semibold lg:hidden">{title}</h1>
        <button
          onClick={onOpenSearch}
          title="Search (⌘K)"
          className="hidden h-9 w-full max-w-md items-center gap-2.5 rounded-xl border border-border bg-secondary/40 px-3 text-sm text-muted-foreground transition-colors hover:bg-secondary md:flex"
        >
          <Search className="h-4 w-4 shrink-0" />
          <span className="flex-1 truncate text-left">Search pages and actions…</span>
          <kbd className="flex h-6 shrink-0 items-center rounded-md bg-muted px-2 text-xs text-muted-foreground">⌘K</kbd>
        </button>
      </div>

      <div className="flex shrink-0 items-center gap-2">
        <Button variant="ghost" onClick={onOpenSearch} className="h-9 w-9 p-0 md:hidden" aria-label="Search">
          <Search className="h-5 w-5" />
        </Button>
        <ThemeToggle size="md" />
        <div className="relative" ref={menuRef}>
          <button
            type="button"
            onClick={() => setMenuOpen((o) => !o)}
            aria-label="Account menu"
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            className="flex h-9 items-center gap-2 rounded-lg px-1.5 transition-colors hover:bg-secondary"
          >
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-primary to-purple-500 text-xs font-bold text-white shadow-glow">
              {(user?.displayName || user?.username || 'F').charAt(0).toUpperCase()}
            </div>
            {user && (
              <span className="hidden max-w-[120px] truncate text-sm font-medium md:inline">
                {user.displayName || user.username}
              </span>
            )}
            <ChevronDown className={cn('h-3.5 w-3.5 text-muted-foreground md:inline', menuOpen && 'rotate-180')} />
          </button>
          <AnimatePresence>
            {menuOpen && (
              <motion.div
                initial={{ opacity: 0, y: 6, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 6, scale: 0.97 }}
                transition={{ duration: 0.12 }}
                className="absolute right-0 top-full z-50 mt-2 w-56 rounded-xl border border-border bg-card p-1.5 shadow-soft"
              >
                {user && (
                  <div className="px-2.5 py-2">
                    <p className="truncate text-sm font-semibold">{user.displayName || user.username}</p>
                    {user.email && <p className="truncate text-xs text-muted-foreground">{user.email}</p>}
                  </div>
                )}
                <div className="mx-1 my-1 h-px bg-border" />
                <NavLink to="/app/profile" onClick={() => setMenuOpen(false)} className={menuItemClasses}>
                  <User className="h-4 w-4 shrink-0" />
                  Profile
                </NavLink>
                <a
                  href="mailto:divyeshpanchasara.work@gmail.com"
                  className={menuItemClasses}
                  onClick={() => setMenuOpen(false)}
                >
                  <Mail className="h-4 w-4 shrink-0" />
                  Send feedback
                </a>
                <button
                  type="button"
                  onClick={handleLogout}
                  className={cn(menuItemClasses, 'hover:bg-destructive/10 hover:text-destructive')}
                >
                  <LogOut className="h-4 w-4 shrink-0" />
                  Sign out
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </header>
  );
}
