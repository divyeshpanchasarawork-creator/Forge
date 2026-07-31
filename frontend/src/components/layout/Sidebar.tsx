import { NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Code2, RefreshCw, PenLine, BarChart3, User, LogOut, Lightbulb, Brain, Mail, ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuth } from '@/contexts/AuthContext';
import { Button, buttonVariants } from '@/components/ui/Button';
import { Logo } from '@/components/brand/Logo';
import { AnimatePresence, motion } from 'framer-motion';

const navSections: { label: string; items: { to: string; icon: typeof LayoutDashboard; label: string; shortcut: string }[] }[] = [
  {
    label: 'Navigate',
    items: [
      { to: '/app', icon: LayoutDashboard, label: 'Dashboard', shortcut: '1' },
      { to: '/app/roadmap', icon: Lightbulb, label: 'Roadmap', shortcut: '2' },
      { to: '/app/problems', icon: Code2, label: 'Practice', shortcut: '3' },
      { to: '/app/revision', icon: RefreshCw, label: 'Revision', shortcut: '4' },
    ],
  },
  {
    label: 'Insights',
    items: [
      { to: '/app/journal', icon: PenLine, label: 'Journal', shortcut: '5' },
      { to: '/app/memory', icon: Brain, label: 'Memory', shortcut: '6' },
      { to: '/app/analytics', icon: BarChart3, label: 'Analytics', shortcut: '7' },
    ],
  },
  {
    label: 'Account',
    items: [{ to: '/app/profile', icon: User, label: 'Profile', shortcut: '8' }],
  },
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
  const { logout, user } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    navigate('/', { state: { signedOut: true } });
    logout();
  };

  const sidebar = (
    <div className="flex h-full flex-col">
      {/* Brand header */}
      <div className={cn(
        "flex h-14 shrink-0 items-center border-b border-border",
        collapsed ? "justify-center" : "px-5"
      )}>
        {collapsed ? (
          <Logo size="md" variant="gradient" />
        ) : (
          <Logo size="md" variant="gradient" withText />
        )}
      </div>

      {/* Nav */}
      <nav className={cn('flex-1 space-y-1 px-2 pt-4', collapsed ? 'overflow-visible' : 'overflow-y-auto')}>
        {navSections.map((section) => (
          <div key={section.label}>
            {!collapsed && (
              <p className="px-3 pb-1 pt-4 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground/60">
                {section.label}
              </p>
            )}
            <div className="space-y-1">
              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === '/app'}
                  onClick={onMobileClose}
                  className={({ isActive }) =>
                    cn(
                      'group relative flex items-center rounded-lg text-sm font-medium transition-colors',
                      collapsed ? 'justify-center py-2.5' : 'gap-3 px-3 py-2',
                      isActive
                        ? 'text-primary'
                        : 'text-muted-foreground hover:bg-secondary/60 hover:text-foreground'
                    )
                  }
                >
                  {({ isActive }) => (
                    <>
                      {isActive && (
                        <motion.span
                          layoutId="active-pill"
                          className="absolute inset-0 rounded-lg bg-gradient-to-r from-primary/15 via-primary/10 to-transparent"
                          transition={{ type: 'spring', damping: 28, stiffness: 300 }}
                        />
                      )}
                      {isActive && !collapsed && (
                        <span className="absolute left-0 top-1/2 h-5 w-1 -translate-y-1/2 rounded-r-full bg-primary" />
                      )}
                      <span
                        className={cn(
                          'relative flex shrink-0 items-center justify-center transition-transform duration-200',
                          isActive
                            ? 'rounded-lg bg-primary/15'
                            : 'group-hover:scale-110',
                          collapsed ? 'h-8 w-8' : 'h-7 w-7'
                        )}
                      >
                        <item.icon
                          className={cn(
                            'h-4 w-4',
                            isActive
                              ? 'text-primary'
                              : 'text-muted-foreground group-hover:text-foreground'
                          )}
                        />
                      </span>
                      {!collapsed && (
                        <>
                          <span className={cn('relative flex-1 truncate', isActive && 'font-semibold')}>
                            {item.label}
                          </span>
                          <kbd className="relative rounded border border-border px-1.5 py-0.5 text-[10px] text-muted-foreground opacity-0 transition-opacity group-hover:opacity-60">
                            {item.shortcut}
                          </kbd>
                        </>
                      )}
                      {collapsed && (
                        <span className="pointer-events-none absolute left-full z-50 ml-2 whitespace-nowrap rounded-lg border border-border bg-card px-2.5 py-1.5 text-xs font-medium text-foreground opacity-0 shadow-lg transition-opacity duration-150 group-hover:opacity-100">
                          {item.label}
                        </span>
                      )}
                    </>
                  )}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </nav>

      {/* Feedback */}
      {!collapsed && (
        <div className="border-t border-border px-3 py-2">
          <a
            href="mailto:divyeshpanchasara.work@gmail.com"
            className={buttonVariants({ variant: 'ghost', size: 'sm', className: 'w-full justify-start px-3 text-xs' })}
          >
            <Mail className="h-3 w-3 shrink-0" />
            Send feedback
          </a>
        </div>
      )}

      {/* User card */}
      {user && !collapsed && (
        <div className="border-t border-border px-3 py-3">
          <NavLink
            to="/app/profile"
            onClick={onMobileClose}
            className="group flex items-center gap-3 rounded-xl bg-secondary/40 px-3 py-2.5 transition-colors hover:bg-secondary"
          >
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-primary to-purple-500 text-xs font-bold text-white shadow-glow">
              {(user.displayName || user.username).charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium leading-tight text-foreground">
                {user.displayName || user.username}
              </p>
              <p className="truncate text-xs text-muted-foreground">{user.email || 'Forge user'}</p>
            </div>
          </NavLink>
        </div>
      )}

      {/* Logout */}
      <div className="border-t border-border px-3 py-3">
        {collapsed ? (
          <Button
            variant="destructive"
            size="sm"
            onClick={handleLogout}
            className="w-full"
            title="Sign Out"
          >
            <LogOut className="h-4 w-4 shrink-0" />
          </Button>
        ) : (
          <Button
            variant="destructive"
            onClick={handleLogout}
            className="w-full justify-start px-3"
          >
            <LogOut className="h-4 w-4 shrink-0" />
            Sign Out
          </Button>
        )}
      </div>

      {/* Footer: hint + collapse */}
      <div className="border-t border-border px-3 py-2">
        {!collapsed ? (
          <p className="px-3 py-1 text-[10px] text-muted-foreground/70">
            <kbd className="mr-1 rounded border border-border px-1">⌘K</kbd> Search · <kbd className="mx-1 rounded border border-border px-1">1–8</kbd> Jump
          </p>
        ) : null}
        <Button
          variant="ghost"
          size="sm"
          onClick={onToggle}
          className="w-full"
          title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? <ChevronRight className="h-4 w-4" /> : <ChevronLeft className="h-4 w-4" />}
        </Button>
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
