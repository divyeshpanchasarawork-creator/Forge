import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Code2, RefreshCw, Sparkles } from 'lucide-react';
import { motion } from 'framer-motion';
import { Logo } from '@/components/brand/Logo';
import ThemeToggle from '@/components/ui/ThemeToggle';

interface AuthShellProps {
  title: string;
  subtitle: string;
  children: ReactNode;
}

const highlights = [
  { icon: Code2, title: 'LeetCode Sync', desc: 'Auto-import solved problems and streaks' },
  { icon: RefreshCw, title: 'Spaced Repetition', desc: 'SM-2 scheduling tuned to your pace' },
  { icon: Sparkles, title: 'Smart Insights', desc: 'Recommendations from your real patterns' },
];

export default function AuthShell({ title, subtitle, children }: AuthShellProps) {
  return (
    <div className="relative flex min-h-screen flex-col bg-background">
      {/* Animated background gradient */}
      <div className="pointer-events-none fixed inset-0 -z-10">
        <div className="absolute left-1/4 top-0 h-[600px] w-[600px] animate-pulse rounded-full bg-primary/8 blur-[160px]" />
        <div className="absolute right-1/4 bottom-0 h-[500px] w-[500px] animate-pulse rounded-full bg-purple-500/5 blur-[140px]" style={{ animationDelay: '2s' }} />
        <div className="absolute left-1/2 top-1/3 h-[400px] w-[400px] animate-pulse rounded-full bg-blue-500/4 blur-[120px]" style={{ animationDelay: '4s' }} />
      </div>

      {/* Header */}
      <header className="fixed top-0 right-0 left-0 z-20 bg-background/80 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
          <Link to="/" className="flex items-center gap-2.5">
            <Logo size="sm" variant="flame" withText />
          </Link>
          <ThemeToggle size="md" />
        </div>
      </header>

      {/* Body */}
      <div className="relative flex flex-1 flex-col pt-16 lg:flex-row">
        {/* Left branding panel (desktop) */}
        <div className="relative hidden w-1/2 overflow-hidden lg:block">
          <div className="absolute inset-0 bg-gradient-to-br from-primary via-purple-600 to-blue-600" />
          <div
            className="absolute inset-0 opacity-[0.12]"
            style={{
              backgroundImage:
                'linear-gradient(rgba(255,255,255,0.5) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.5) 1px, transparent 1px)',
              backgroundSize: '40px 40px',
            }}
          />
          <div className="pointer-events-none absolute -left-20 top-1/3 h-96 w-96 rounded-full bg-white/10 blur-3xl" />
          <div className="pointer-events-none absolute -right-16 top-16 h-64 w-64 rounded-full bg-white/10 blur-3xl" />
          <div className="relative flex h-full flex-col justify-between p-12">
            <div>
              <h2 className="max-w-md text-3xl font-bold leading-tight text-white">
                Forge your engineering craft, one problem at a time.
              </h2>
              <p className="mt-4 max-w-md text-sm leading-relaxed text-white/80">
                Track topics, solve problems, build consistency — all in one personal learning engine.
              </p>
              <div className="mt-8 flex flex-col gap-3">
                {highlights.map((h) => (
                  <div key={h.title} className="flex items-center gap-3 rounded-xl bg-white/10 px-4 py-3 backdrop-blur-sm">
                    <h.icon className="h-4 w-4 shrink-0 text-white" />
                    <div>
                      <p className="text-sm font-semibold text-white">{h.title}</p>
                      <p className="text-xs text-white/70">{h.desc}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <p className="text-xs text-white/60">Personal engineering companion</p>
          </div>
        </div>

        {/* Right — form */}
        <div className="relative flex flex-1 items-center justify-center px-4 py-16">
          <div className="w-full max-w-md">
            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.35, ease: 'easeOut' }}
              className="rounded-2xl border border-border bg-card/70 p-8 shadow-2xl shadow-black/10 backdrop-blur-xl"
            >
              <div className="mb-8 text-center">
                <h1 className="text-xl font-bold tracking-tight">{title}</h1>
                <p className="mt-1 text-sm text-muted-foreground">{subtitle}</p>
              </div>
              {children}
            </motion.div>
          </div>
        </div>
      </div>

      {/* Footer */}
      <footer className="px-6 py-4">
        <div className="mx-auto flex max-w-7xl items-center justify-between text-xs text-muted-foreground">
          <p>Personal engineering companion</p>
          <p>© {new Date().getFullYear()} Forge</p>
        </div>
      </footer>
    </div>
  );
}
