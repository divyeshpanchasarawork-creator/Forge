import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Code2, RefreshCw, PenLine, BarChart3, User, Flame, LogOut, Lightbulb, Brain, Menu, X, Mail, ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuth } from '@/contexts/AuthContext';
import { useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/roadmap', icon: Lightbulb, label: 'Roadmap' },
  { to: '/problems', icon: Code2, label: 'Practice' },
  { to: '/revision', icon: RefreshCw, label: 'Revision' },
  { to: '/journal', icon: PenLine, label: 'Journal' },
  { to: '/memory', icon: Brain, label: 'Memory' },
  { to: '/analytics', icon: BarChart3, label: 'Analytics' },
  { to: '/profile', icon: User, label: 'Profile' },
];

export default function Sidebar({ collapsed, onToggle }: { collapsed: boolean; onToggle: () => void }) {
  const { logout } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
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
            end={item.to === '/'}
            onClick={() => setMobileOpen(false)}
            className={({ isActive }) =>
              cn(
                'flex items-center rounded-lg px-3 py-2.5 text-sm font-medium transition-colors group',
                collapsed ? 'justify-center' : 'gap-3',
                isActive
                  ? 'bg-primary/10 text-primary'
                  : 'text-muted-foreground hover:bg-secondary hover:text-foreground'
              )
            }
            title={collapsed ? item.label : undefined}
          >
            <item.icon className="h-4 w-4 shrink-0" />
            {!collapsed && item.label}
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
      {/* Mobile hamburger */}
      <button
        onClick={() => setMobileOpen(!mobileOpen)}
        className="fixed left-4 top-4 z-50 flex h-10 w-10 items-center justify-center rounded-lg bg-card border border-border shadow-lg lg:hidden"
        aria-label="Toggle menu"
      >
        {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
      </button>

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
              onClick={() => setMobileOpen(false)}
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
