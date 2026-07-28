import { NavLink } from 'react-router-dom';
import { LayoutDashboard, BookOpen, Code2, RefreshCw, PenLine, BarChart3, User, Flame } from 'lucide-react';
import { cn } from '@/lib/utils';

const navItems = [
  { to: '/', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/topics', icon: BookOpen, label: 'Topics' },
  { to: '/problems', icon: Code2, label: 'Problems' },
  { to: '/revision', icon: RefreshCw, label: 'Revision' },
  { to: '/journal', icon: PenLine, label: 'Journal' },
  { to: '/analytics', icon: BarChart3, label: 'Analytics' },
  { to: '/profile', icon: User, label: 'Profile' },
];

export default function Sidebar() {
  return (
    <aside className="fixed left-0 top-0 z-40 h-screen w-64 border-r border-border bg-sidebar">
      <div className="flex h-16 items-center gap-2 px-6">
        <Flame className="h-7 w-7 text-primary" />
        <span className="text-xl font-bold tracking-tight">Forge</span>
      </div>
      <nav className="mt-4 space-y-1 px-3">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary/10 text-primary'
                  : 'text-muted-foreground hover:bg-secondary hover:text-foreground'
              )
            }
          >
            <item.icon className="h-4 w-4" />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  );
}
