import { useEffect, useRef, useState, type ComponentType } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import {
  LayoutDashboard, Lightbulb, Code2, RefreshCw, PenLine, Brain, BarChart3,
  User, Search, Sun, Moon, NotebookPen, LogOut, CornerDownLeft, History,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useTheme } from '@/contexts/ThemeContext';
import { cn } from '@/lib/utils';

type CommandIcon = ComponentType<{ className?: string }>;

interface NavCommand {
  id: string;
  label: string;
  group: string;
  icon: CommandIcon;
  hint?: string;
  to: string;
}

interface CommandItem {
  id: string;
  label: string;
  group: string;
  icon: CommandIcon;
  hint?: string;
  run: () => void;
}

const navCommands: NavCommand[] = [
  { id: 'dashboard', label: 'Dashboard', group: 'Navigate', icon: LayoutDashboard, hint: '1', to: '/app' },
  { id: 'roadmap', label: 'Roadmap', group: 'Navigate', icon: Lightbulb, hint: '2', to: '/app/roadmap' },
  { id: 'practice', label: 'Practice', group: 'Navigate', icon: Code2, hint: '3', to: '/app/problems' },
  { id: 'revision', label: 'Revision', group: 'Navigate', icon: RefreshCw, hint: '4', to: '/app/revision' },
  { id: 'journal', label: 'Journal', group: 'Navigate', icon: PenLine, hint: '5', to: '/app/journal' },
  { id: 'memory', label: 'Memory', group: 'Navigate', icon: Brain, hint: '6', to: '/app/memory' },
  { id: 'analytics', label: 'Analytics', group: 'Navigate', icon: BarChart3, hint: '7', to: '/app/analytics' },
  { id: 'profile', label: 'Profile', group: 'Navigate', icon: User, hint: '8', to: '/app/profile' },
];

export default function CommandPalette({ open, onOpen, onClose, recent }: { open: boolean; onOpen: () => void; onClose: () => void; recent: { path: string; label: string }[] }) {
  const [query, setQuery] = useState('');
  const [active, setActive] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { theme, toggle: toggleTheme } = useTheme();
  const location = useLocation();

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        if (open) onClose();
        else onOpen();
      } else if (e.key === 'Escape' && open) {
        onClose();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose, onOpen]);

  useEffect(() => {
    if (open) {
      setQuery('');
      setActive(0);
      const t = requestAnimationFrame(() => inputRef.current?.focus());
      return () => cancelAnimationFrame(t);
    }
  }, [open]);

  const actions: CommandItem[] = [
    {
      id: 'act-theme',
      label: theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode',
      group: 'Actions',
      icon: theme === 'dark' ? Sun : Moon,
      run: toggleTheme,
    },
    {
      id: 'act-journal',
      label: 'Write a journal entry',
      group: 'Actions',
      icon: NotebookPen,
      run: () => navigate('/app/journal'),
    },
    {
      id: 'act-logout',
      label: 'Sign out',
      group: 'Actions',
      icon: LogOut,
      run: () => {
        navigate('/', { state: { signedOut: true } });
        logout();
      },
    },
  ];

  const q = query.trim().toLowerCase();
  const items: CommandItem[] = [
    ...(q
      ? []
      : recent
          .filter((r) => r.path !== location.pathname)
          .map((r) => ({
            id: `recent-${r.path}`,
            label: r.label,
            group: 'Recent',
            icon: History,
            run: () => navigate(r.path),
          }))),
    ...navCommands
      .filter((c) => !q || c.label.toLowerCase().includes(q))
      .map((c) => ({ ...c, run: () => navigate(c.to) })),
    ...actions.filter((a) => !q || a.label.toLowerCase().includes(q)),
  ];

  useEffect(() => {
    setActive(0);
  }, [q]);

  useEffect(() => {
    listRef.current?.querySelector('[data-active="true"]')?.scrollIntoView({ block: 'nearest' });
  }, [active]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((a) => Math.min(a + 1, items.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((a) => Math.max(a - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      items[active]?.run();
      onClose();
    } else if (e.key === 'Escape') {
      e.preventDefault();
      onClose();
    }
  };

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 md:items-center md:p-4"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
        >
          <motion.div
            role="dialog"
            aria-modal="true"
            aria-label="Command palette"
            className="flex h-full w-full flex-col overflow-hidden rounded-t-[24px] border border-border bg-card/95 p-4 shadow-2xl backdrop-blur-xl md:h-[72vh] md:max-h-[760px] md:max-w-[920px] md:w-[min(92vw,760px)] md:rounded-[22px] md:p-6 xl:w-[min(92vw,820px)] 2xl:w-[min(88vw,880px)]"
            initial={{ opacity: 0, y: 24, scale: 0.98 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 24, scale: 0.98 }}
            transition={{ duration: 0.12 }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="mb-4 flex h-[52px] shrink-0 items-center gap-3 rounded-2xl border border-border bg-secondary/40 px-4 transition-colors focus-within:border-primary/40 focus-within:ring-2 focus-within:ring-primary/20 md:mb-6 md:h-14">
              <Search className="h-4 w-4 shrink-0 text-muted-foreground" />
              <input
                ref={inputRef}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Search pages and actions…"
                className="h-full w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
              />
              <kbd className="shrink-0 rounded-lg border border-border px-1.5 py-0.5 text-[10px] text-muted-foreground">esc</kbd>
            </div>
            <div ref={listRef} className="flex-1 overflow-y-auto px-2 pb-5">
              {items.length === 0 && (
                <div className="px-3 py-10 text-center">
                  <p className="text-sm font-medium text-muted-foreground">No results for "{query}"</p>
                  <p className="mt-1 text-xs text-muted-foreground/60">
                    Try a page name like "practice" or an action like "theme".
                  </p>
                </div>
              )}
              {items.map((item, i) => {
                const showGroup = i === 0 || items[i - 1].group !== item.group;
                const isActive = i === active;
                return (
                  <div key={item.id}>
                    {showGroup && (
                      <p className="px-2 pt-6 pb-3 text-[10px] font-medium uppercase tracking-[0.16em] text-muted-foreground/50 first:pt-0">
                        {item.group}
                      </p>
                    )}
                    <button
                      data-active={isActive}
                      onMouseEnter={() => setActive(i)}
                      onClick={() => {
                        item.run();
                        onClose();
                      }}
                      className={cn(
                        'flex h-16 w-full items-center gap-4 rounded-xl px-4 text-left text-sm transition-colors md:h-14',
                        isActive ? 'bg-primary/10 text-foreground' : 'text-muted-foreground'
                      )}
                    >
                      <item.icon className="h-4 w-4 shrink-0" />
                      <span className="flex-1 truncate">{item.label}</span>
                      {item.hint && (
                        <kbd className="rounded-[10px] border border-border px-1.5 py-0.5 text-[10px] text-muted-foreground">
                          {item.hint}
                        </kbd>
                      )}
                      {isActive && <CornerDownLeft className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />}
                    </button>
                  </div>
                );
              })}
            </div>
            <div className="mt-6 flex min-h-16 shrink-0 items-center justify-between border-t border-border px-2 pt-4 pb-[calc(0.5rem+env(safe-area-inset-bottom))] text-[11px] text-muted-foreground">
              <span className="flex items-center gap-1.5">
                <kbd className="rounded-[10px] border border-border px-1.5 py-0.5">↑</kbd>
                <kbd className="rounded-[10px] border border-border px-1.5 py-0.5">↓</kbd>
                to navigate
              </span>
              <span className="flex items-center gap-1.5">
                <kbd className="rounded-[10px] border border-border px-1.5 py-0.5">↵</kbd> select
                <kbd className="ml-1.5 rounded-[10px] border border-border px-1.5 py-0.5">esc</kbd> close
              </span>
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
