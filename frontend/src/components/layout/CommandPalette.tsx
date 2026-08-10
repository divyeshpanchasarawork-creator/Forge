import { useEffect, useMemo, useRef, useState, type ComponentType, type ReactNode } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  LayoutDashboard, Lightbulb, Code2, RefreshCw, PenLine, Brain, BarChart3,
  User, Search, Sun, Moon, NotebookPen, LogOut, CornerDownLeft, History,
  Hash, Loader2, ExternalLink,
} from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useTheme } from '@/contexts/ThemeContext';
import { cn } from '@/lib/utils';
import { practiceApi, searchApi, type SearchProblem } from '@/api';
import type { ProblemAttempt } from '@/types';

type CommandIcon = ComponentType<{ className?: string }>;

interface NavCommand {
  id: string;
  label: string;
  icon: CommandIcon;
  hint?: string;
  to: string;
}

interface PaletteItem {
  id: string;
  kind: 'problem' | 'topic' | 'page' | 'action' | 'recent' | 'attempt';
  group: string;
  title: string;
  subtitle?: string;
  icon?: CommandIcon;
  hint?: string;
  difficulty?: string;
  outcome?: ProblemAttempt['outcome'];
  run: () => void;
}

const navCommands: NavCommand[] = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard, hint: '1', to: '/app' },
  { id: 'roadmap', label: 'Roadmap', icon: Lightbulb, hint: '2', to: '/app/roadmap' },
  { id: 'practice', label: 'Practice', icon: Code2, hint: '3', to: '/app/problems' },
  { id: 'revision', label: 'Revision', icon: RefreshCw, hint: '4', to: '/app/revision' },
  { id: 'journal', label: 'Journal', icon: PenLine, hint: '5', to: '/app/journal' },
  { id: 'memory', label: 'Memory', icon: Brain, hint: '6', to: '/app/memory' },
  { id: 'analytics', label: 'Analytics', icon: BarChart3, hint: '7', to: '/app/analytics' },
  { id: 'profile', label: 'Profile', icon: User, hint: '8', to: '/app/profile' },
];

const TOPICS = [
  'array', 'string', 'hash-table', 'two-pointers', 'sliding-window', 'stack', 'queue',
  'linked-list', 'trees', 'bst', 'graphs', 'dfs', 'bfs', 'binary-search', 'heap',
  'trie', 'greedy', 'dynamic-programming', 'backtracking', 'bit-manipulation', 'math',
  'prefix-sum', 'monotonic-stack', 'monotonic-queue', 'segment-tree', 'fenwick-tree',
  'union-find', 'design', 'sql', 'system-design-prep',
];

const TOPIC_TITLES: Record<string, string> = Object.fromEntries(TOPICS.map((slug) => [slug, titleize(slug)]));

const DIFF_COLOR: Record<string, string> = {
  Easy: 'var(--color-success)',
  Medium: 'var(--color-warning)',
  Hard: 'var(--color-destructive)',
};

const OUTCOME_STYLE: Record<ProblemAttempt['outcome'], { dot: string; label: string }> = {
  SOLVED: { dot: 'bg-success', label: 'Solved' },
  PARTIAL: { dot: 'bg-warning', label: 'Partial' },
  FAILED: { dot: 'bg-destructive', label: 'Failed' },
  SKIPPED: { dot: 'bg-muted-foreground', label: 'Skipped' },
};

function titleize(slug: string): string {
  return slug
    .split('-')
    .map((w) => (w === 'sql' ? 'SQL' : w.charAt(0).toUpperCase() + w.slice(1)))
    .join(' ');
}

function timeAgo(iso: string): string {
  const s = Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 1000));
  if (s < 60) return 'just now';
  const m = Math.floor(s / 60);
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  const d = Math.floor(h / 24);
  return d === 1 ? 'yesterday' : `${d}d ago`;
}

function Highlight({ text, query }: { text: string; query: string }): ReactNode {
  const q = query.trim().toLowerCase();
  if (!q) return text;
  const idx = text.toLowerCase().indexOf(q);
  if (idx === -1) return text;
  return (
    <>
      {text.slice(0, idx)}
      <mark className="rounded-[4px] bg-primary/25 px-0.5 text-foreground">
        {text.slice(idx, idx + q.length)}
      </mark>
      {text.slice(idx + q.length)}
    </>
  );
}

function openLeetCode(slug: string) {
  window.open(`https://leetcode.com/problems/${slug}/`, '_blank', 'noopener,noreferrer');
}

export default function CommandPalette({ open, onClose, recent }: { open: boolean; onClose: () => void; recent: { path: string; label: string }[] }) {
  const [query, setQuery] = useState('');
  const [debounced, setDebounced] = useState('');
  const [active, setActive] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const { logout } = useAuth();
  const { theme, toggle: toggleTheme } = useTheme();
  const location = useLocation();

  const { data: attempts } = useQuery({
    queryKey: ['palette-attempts'],
    queryFn: () => practiceApi.getAttempts(6).then((r) => r.data.data),
    enabled: open,
    staleTime: 60_000,
  });

  const { data: problems, isFetching: searching } = useQuery<SearchProblem[]>({
    queryKey: ['problem-search', debounced],
    queryFn: () => searchApi.problems(debounced).then((r) => r.data.data ?? []),
    enabled: open && debounced.length > 0,
    staleTime: 60_000,
    placeholderData: (prev) => prev,
  });

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && open) {
        onClose();
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  useEffect(() => {
    if (open) {
      setQuery('');
      setDebounced('');
      setActive(0);
      const t = requestAnimationFrame(() => inputRef.current?.focus());
      return () => {
        cancelAnimationFrame(t);
        inputRef.current?.blur();
      };
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const t = setTimeout(() => setDebounced(query.trim()), 250);
    return () => clearTimeout(t);
  }, [query, open]);

  const q = query.trim();

  const baseItems: PaletteItem[] = useMemo(
    () => [
      ...recent
        .filter((r) => r.path !== location.pathname)
        .map((r): PaletteItem => ({
          id: `recent-${r.path}`,
          kind: 'recent',
          group: 'Recent',
          title: r.label,
          icon: History,
          run: () => navigate(r.path),
        })),
      ...navCommands.map((c): PaletteItem => ({
        id: c.id,
        kind: 'page',
        group: 'Navigate',
        title: c.label,
        icon: c.icon,
        hint: c.hint,
        run: () => navigate(c.to),
      })),
      {
        id: 'act-theme',
        kind: 'action',
        group: 'Actions',
        title: theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode',
        icon: theme === 'dark' ? Sun : Moon,
        run: toggleTheme,
      },
      {
        id: 'act-journal',
        kind: 'action',
        group: 'Actions',
        title: 'Write a journal entry',
        icon: NotebookPen,
        run: () => navigate('/app/journal'),
      },
      {
        id: 'act-logout',
        kind: 'action',
        group: 'Actions',
        title: 'Sign out',
        icon: LogOut,
        run: () => {
          navigate('/', { state: { signedOut: true } });
          logout();
        },
      },
      ...TOPICS.map((slug): PaletteItem => ({
        id: `topic-${slug}`,
        kind: 'topic',
        group: 'Topics',
        title: TOPIC_TITLES[slug],
        icon: Hash,
        run: () => navigate('/app/problems'),
      })),
      ...(attempts ?? []).map((a): PaletteItem => ({
        id: `attempt-${a.id}`,
        kind: 'attempt',
        group: 'Recent attempts',
        title: a.problemTitle,
        subtitle: timeAgo(a.attemptedAt),
        difficulty: a.difficulty,
        outcome: a.outcome,
        run: () => openLeetCode(a.problemSlug),
      })),
    ],
    [recent, location.pathname, theme, attempts, navigate, logout, toggleTheme]
  );

  const problemItems: PaletteItem[] = (problems ?? []).map((p) => ({
    id: `problem-${p.titleSlug}`,
    kind: 'problem',
    group: 'Problems',
    title: p.title,
    subtitle: (p.tags ?? []).slice(0, 3).join(' · '),
    difficulty: p.difficulty,
    run: () => openLeetCode(p.titleSlug),
  }));

  const localMatches: PaletteItem[] = q
    ? baseItems.filter((it) =>
        `${it.title} ${it.subtitle ?? ''} ${it.group}`.toLowerCase().includes(q.toLowerCase()),
      )
    : baseItems;

  const items: PaletteItem[] = q ? [...problemItems, ...localMatches] : baseItems;

  useEffect(() => {
    setActive(0);
  }, [q, debounced, problems]);

  useEffect(() => {
    listRef.current?.querySelector('[data-active="true"]')?.scrollIntoView({ block: 'nearest' });
  }, [active]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (items.length === 0) return;
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

  const twoLine = (item: PaletteItem) => item.kind === 'problem' || item.kind === 'attempt';

  return (
    <div
      className={cn(
        'fixed inset-0 z-50 flex items-end justify-center bg-black/50 transition-opacity duration-150 md:items-center md:p-4',
        open ? 'opacity-100' : 'pointer-events-none opacity-0'
      )}
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Command palette"
        inert={!open}
        className={cn(
          'flex h-full w-full flex-col overflow-hidden rounded-t-2xl border border-border bg-card p-3 transition-all duration-150 md:h-[72vh] md:max-h-[760px] md:max-w-[920px] md:w-[min(92vw,760px)] md:rounded-2xl md:p-4 xl:w-[min(92vw,820px)] 2xl:w-[min(88vw,880px)]',
          open ? 'translate-y-0 scale-100 opacity-100' : 'pointer-events-none translate-y-4 scale-[0.98] opacity-0'
        )}
        onClick={(e) => e.stopPropagation()}
      >
            <div className="mb-3 flex h-14 shrink-0 items-center gap-3 border-b border-border px-2">
              {searching ? (
                <Loader2 className="h-[18px] w-[18px] shrink-0 animate-spin text-primary" />
              ) : (
                <Search className="h-[18px] w-[18px] shrink-0 text-muted-foreground/70" />
              )}
              <input
                ref={inputRef}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Search…"
                className="command-palette-input h-full w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
              />
              <kbd className="shrink-0 rounded-md border border-border bg-secondary/60 px-1.5 py-0.5 font-mono text-micro text-muted-foreground">esc</kbd>
            </div>

            <div ref={listRef} className="flex-1 overflow-y-auto px-2 pb-4">
              {searching && debounced && localMatches.length === 0 && (
                <div className="space-y-1">
                  <p className="px-2 pt-3 pb-1.5 text-micro font-medium uppercase tracking-[0.2em] text-muted-foreground/40">Problems</p>
                  {[0, 1, 2].map((i) => (
                    <div key={i} className="flex h-12 w-full items-center gap-3 rounded-lg px-3">
                      <div className="h-8 w-8 shrink-0 animate-pulse rounded-md bg-secondary" />
                      <div className="flex-1 space-y-2">
                        <div className="h-3 w-2/3 animate-pulse rounded bg-secondary" />
                        <div className="h-2.5 w-1/3 animate-pulse rounded bg-secondary/70" />
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {!searching && items.length === 0 && (
                <div className="px-3 py-10 text-center">
                  <p className="text-sm font-medium text-muted-foreground">No results for "{query}"</p>
                  <p className="mt-1 text-xs text-muted-foreground/60">
                    Try a problem, topic, or page like "Two Sum", "Graphs", or "Journal".
                  </p>
                </div>
              )}

              {!searching &&
                items.map((item, i) => {
                  const showGroup = i === 0 || items[i - 1].group !== item.group;
                  const isActive = i === active;
                  const diff = item.difficulty ? DIFF_COLOR[item.difficulty] : undefined;
                  const outcome = item.outcome ? OUTCOME_STYLE[item.outcome] : undefined;
                  return (
                    <div key={item.id}>
                      {showGroup && (
                        <p className="px-2 pt-4 pb-1.5 text-micro font-medium uppercase tracking-[0.2em] text-muted-foreground/40 first:pt-0">
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
                          'relative flex w-full items-center gap-3 rounded-lg px-3 text-left text-sm transition-colors duration-150',
                          'before:absolute before:left-0 before:top-1/2 before:h-5 before:w-[2px] before:-translate-y-1/2 before:rounded-full before:bg-primary before:opacity-0',
                          twoLine(item) ? 'min-h-[52px] py-1.5' : 'h-12 md:h-11',
                          isActive
                            ? 'bg-primary/10 text-foreground before:opacity-100'
                            : 'text-muted-foreground hover:bg-secondary/50'
                        )}
                      >
                        {twoLine(item) ? (
                          <>
                            <div className="flex min-w-0 flex-1 flex-col gap-0.5">
                              <span className="truncate font-medium text-foreground">
                                <Highlight text={item.title} query={q} />
                              </span>
                              <span className="truncate text-xs text-muted-foreground">
                                {item.subtitle}
                              </span>
                            </div>
                            {outcome && (
                              <span className="flex shrink-0 items-center gap-1.5 text-xs">
                                <span className={cn('h-1.5 w-1.5 rounded-full', outcome.dot)} />
                                {outcome.label}
                              </span>
                            )}
                            {diff && (
                              <span className="flex shrink-0 items-center gap-1.5 text-xs">
                                <span className="h-1.5 w-1.5 rounded-full" style={{ backgroundColor: diff }} />
                                {item.difficulty}
                              </span>
                            )}
                            <ExternalLink className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />
                          </>
                        ) : (
                          <>
                            {item.icon && <item.icon className="h-[18px] w-[18px] shrink-0 text-muted-foreground/70" />}
                            <span className="flex-1 truncate">
                              <Highlight text={item.title} query={q} />
                            </span>
                            {item.hint && (
                              <kbd className="rounded-md border border-border px-1.5 py-0.5 font-mono text-micro text-muted-foreground">
                                {item.hint}
                              </kbd>
                            )}
                          </>
                        )}
                        {isActive && <CornerDownLeft className="h-3.5 w-3.5 shrink-0 text-muted-foreground" />}
                      </button>
                    </div>
                  );
                })}
            </div>

            <div className="mt-3 flex min-h-10 shrink-0 items-center justify-between border-t border-border px-2 pt-3 pb-[calc(0.25rem+env(safe-area-inset-bottom))] text-caption text-muted-foreground/60">
              <span>↑↓ Navigate</span>
              <span className="flex items-center gap-3">
                <span>↵ Select</span>
                <span>Esc Close</span>
              </span>
            </div>
          </div>
        </div>
  );
}
