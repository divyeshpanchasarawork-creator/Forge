import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Code2, RefreshCw, PenLine, BarChart3, User, Flame, LogOut, Lightbulb, Brain, Mail, ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuth } from '@/contexts/AuthContext';
import { AnimatePresence, motion } from 'framer-motion';

const navItems = [
  { to: '/app', icon: LayoutDashboard, label: 'Dashboard', shortcut: '1' },
  { to: '/app/roadmap', icon: Lightbulb, label: 'Roadmap', shortcut: '2' },
  { to: '/app/problems', icon: Code2, label: 'Practice', shortcut: '3' },
  { to: '/app/revision', icon: RefreshCw, label: 'Revision', shortcut: '4' },
  { to: '/app/journal', icon: PenLine, label: 'Journal', shortcut: '5' },
  { to: '/app/memory', icon: Brain, label: 'Memory', shortcut: '6' },
  { to: '/app/analytics', icon: BarChart3, label: 'Analytics', shortcut: '7' },
  { to: '/app/profile', icon: User, label: 'Profile', shortcut: '8' },
];

export default function Sidebar({
  collapsed,
  onToggle,
  mobileOpen,
  onMobileClose,
}: {
  collapsed: boolean;
  onToggle: () => void;
  mobileOpen: boolean;
  onMobileClose: () => void;
}) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/', { state: { signedOut: true } });
  };

  const sidebar = (
    <div className="flex h-full flex-col">
      <div className={cn(
        "flex h-14 items-center border-b border-border",
        collapsed ? "justify-center px-0" : "gap-2 px-6"
      )}>
        <Flame className="h-6 w-6 text-primary shrink-0" />
        {!collapsed && <span className="text-lg font-bold tracking-tight">Forge</span>}
      </div>
      <nav className="flex-1 space-y-1 px-2 pt-4">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/app'}
            onClick={onMobileClose}
            className={({ isActive }) =>
              cn(
                'relative flex items-center rounded-lg px-3 py-2.5 text-sm font-medium transition-colors group',
                collapsed ? 'justify-center' : 'gap-3',
                isActive
                  ? 'text-primary'
                  : 'text-muted-foreground hover:bg-secondary hover:text-foreground'
              )
            }
            title={collapsed ? item.label : undefined}
          >
            {({ isActive }) => (
              <>
                {isActive && (
                  <motion.span
                    layoutId="active-pill"
                    className="absolute inset-0 rounded-lg bg-primary/10"
                    transition={{ type: 'spring', damping: 28, stiffness: 300 }}
                  />
                )}
                <item.icon className="relative h-4 w-4 shrink-0" />
                {!collapsed && (
                  <>
                    <span className="relative flex-1">{item.label}</span>
                    <kbd className="relative rounded border border-border px-1.5 py-0.5 text-[10px] text-muted-foreground opacity-0 transition-opacity group-hover:opacity-60">
                      {item.shortcut}
                    </kbd>
                  </>
                )}
              </>
            )}
          </NavLink>
        ))}
      </nav>
      {!collapsed && (
        <div className="border-t border-border px-3 py-2">
          <a
            href="mailto:divyeshpanchasara.work@gmail.com"
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-xs text-muted-foreground transition-colors hover:text-foreground"
          >
            <Mail className="h-3 w-3 shrink-0" />
            Send feedback
          </a>
        </div>
      )}
      <div className="border-t border-border px-3 py-3">
        {collapsed ? (
          <button
            onClick={handleLogout}
            className="flex w-full items-center justify-center rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
            title="Sign Out"
          >
            <LogOut className="h-4 w-4 shrink-0" />
          </button>
        ) : (
          <button
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive"
          >
            <LogOut className="h-4 w-4 shrink-0" />
            Sign Out
          </button>
        )}
      </div>
      <div className="border-t border-border px-3 py-2">
        {!collapsed ? (
          <p className="px-3 py-1 text-[10px] text-muted-foreground/70">
            <kbd className="mr-1 rounded border border-border px-1">⌘K</kbd> Search · <kbd className="mx-1 rounded border border-border px-1">1–8</kbd> Jump
          </p>
        ) : null}
        <button
          onClick={onToggle}
          className="flex w-full items-center justify-center rounded-lg px-3 py-2 text-muted-foreground hover:bg-secondary hover:text-foreground transition-colors"
          title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
        </button>
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop sidebar */}
      <aside className={cn(
        "fixed left-0 top-0 z-40 hidden h-screen flex-col border-r border-border bg-sidebar transition-all duration-200 lg:flex",
        collapsed ? "w-16" : "w-64"
      )}>
        {sidebar}
      </aside>

      {/* Mobile sidebar overlay */}
      <AnimatePresence>
        {mobileOpen && (
          <>
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={onMobileClose}
              className="fixed inset-0 z-40 bg-black/50 lg:hidden"
            />
            <motion.aside
              initial={{ x: '-100%' }}
              animate={{ x: 0 }}
              exit={{ x: '-100%' }}
              transition={{ type: 'spring', damping: 25, stiffness: 250 }}
              className="fixed left-0 top-0 z-50 h-screen w-64 border-r border-border bg-sidebar lg:hidden"
            >
              {sidebar}
            </motion.aside>
          </>
        )}
      </AnimatePresence>
    </>
  );
}
