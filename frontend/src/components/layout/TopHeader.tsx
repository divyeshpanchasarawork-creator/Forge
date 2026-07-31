import { useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { useTheme } from '@/contexts/ThemeContext';
import { Sun, Moon, User, Menu, Search } from 'lucide-react';

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

export default function TopHeader({ sidebarCollapsed, onMenuClick, onOpenSearch }: { sidebarCollapsed: boolean; onMenuClick: () => void; onOpenSearch: () => void }) {
  const location = useLocation();
  const { user } = useAuth();
  const { theme, toggle: toggleTheme } = useTheme();
  const title = pageTitles[location.pathname] || 'Forge';

  return (
    <header
      className={`fixed top-0 right-0 z-30 flex h-14 items-center justify-between border-b border-border bg-background/80 backdrop-blur-sm px-4 transition-all lg:px-6 ${
        sidebarCollapsed ? 'lg:left-16' : 'lg:left-64'
      }`}
    >
      <div className="flex items-center gap-3">
        <button
          onClick={onMenuClick}
          className="flex h-9 w-9 items-center justify-center rounded-lg text-muted-foreground hover:bg-secondary hover:text-foreground transition-colors lg:hidden"
          aria-label="Open menu"
        >
          <Menu className="h-5 w-5" />
        </button>
        <h1 className="text-lg font-semibold">{title}</h1>
      </div>
      <div className="flex items-center gap-3">
        <button
          onClick={onOpenSearch}
          className="hidden items-center gap-2 rounded-lg border border-border bg-secondary/40 px-3 py-1.5 text-sm text-muted-foreground transition-colors hover:border-primary/20 hover:text-foreground md:inline-flex"
          title="Search (⌘K)"
        >
          <Search className="h-3.5 w-3.5" />
          <span>Search</span>
          <kbd className="rounded border border-border px-1 py-0.5 text-[10px]">⌘K</kbd>
        </button>
        <button
          onClick={onOpenSearch}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground hover:bg-secondary hover:text-foreground transition-colors md:hidden"
          aria-label="Search"
        >
          <Search className="h-4 w-4" />
        </button>
        <button
          onClick={toggleTheme}
          className="flex h-8 w-8 items-center justify-center rounded-lg text-muted-foreground hover:bg-secondary hover:text-foreground transition-all active:scale-95"
          title={theme === 'dark' ? 'Light mode' : 'Dark mode'}
        >
          {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
        </button>
        {user && (
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <User className="h-4 w-4" />
            <span className="hidden sm:inline">{user.displayName || user.username}</span>
          </div>
        )}
      </div>
    </header>
  );
}
