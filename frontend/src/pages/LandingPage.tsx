import { Link, useLocation, useNavigate } from 'react-router-dom';
import { BarChart3, BookOpen, Code2, RefreshCw, Zap, Target, ArrowRight, Sparkles, ChevronDown, X, CheckCircle2 } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Button, buttonVariants } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Logo } from '@/components/brand/Logo';
import ThemeToggle from '@/components/ui/ThemeToggle';
import AuthCard, { type AuthTab } from '@/components/auth/AuthCard';

const features = [
  { icon: BookOpen, title: 'Topic Mastery', desc: 'Track understanding across every concept with confidence scoring and spaced repetition.' },
  { icon: Code2, title: 'LeetCode Sync', desc: 'Connect your profile. Auto-import solved problems, tags, and streaks in one click.' },
  { icon: BarChart3, title: 'Deep Analytics', desc: 'Spot weak areas and watch progress compound over time.' },
  { icon: RefreshCw, title: 'Spaced Repetition', desc: 'Smart revision scheduling so nothing slips through the cracks.' },
  { icon: Target, title: 'Daily Missions', desc: 'Wake up to a focused plan. Know exactly what to work on today.' },
  { icon: Zap, title: 'Smart Insights', desc: 'Recommendations based on your actual solving patterns.' },
];

export default function LandingPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [authTab, setAuthTab] = useState<AuthTab>('signin');
  const authRef = useRef<HTMLDivElement>(null);
  const [showSignedOut, setShowSignedOut] = useState(
    (location.state as { signedOut?: boolean } | null)?.signedOut === true
  );

  const dismissSignedOut = useCallback(() => {
    setShowSignedOut(false);
    navigate(location.pathname, { replace: true });
  }, [navigate, location.pathname]);

  const focusAuth = useCallback((tab: AuthTab) => {
    setAuthTab(tab);
    authRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }, []);

  useEffect(() => {
    if (!showSignedOut) return;
    const t = setTimeout(dismissSignedOut, 5000);
    return () => clearTimeout(t);
  }, [showSignedOut, dismissSignedOut]);

  const scrollToFeatures = () => {
    document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Signed-out acknowledgment */}
      {showSignedOut && (
        <div className="fade-in-up fixed left-1/2 top-20 z-30 -translate-x-1/2 px-4">
          <div className="flex items-center gap-3 rounded-full border border-border bg-card px-4 py-2 text-sm shadow-card">
            <CheckCircle2 className="h-4 w-4 text-success" />
            <span className="font-medium">Signed out</span>
            <span className="text-muted-foreground">Your progress is saved.</span>
            <button
              onClick={dismissSignedOut}
              className="flex h-5 w-5 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
              aria-label="Dismiss"
            >
              <X className="h-3 w-3" />
            </button>
          </div>
        </div>
      )}

      {/* Soft, static backdrop — dot grid + a single restrained accent, never animated */}
      <div className="pointer-events-none fixed inset-0 -z-10 bg-dots" />
      <div className="pointer-events-none fixed inset-0 -z-10">
        <div className="absolute -top-40 left-1/2 h-[480px] w-[720px] -translate-x-1/2 rounded-full bg-primary/6 blur-[120px]" />
      </div>

      {/* Navbar */}
      <nav className="fixed top-0 z-20 w-full border-b border-border/60 bg-background/85 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
          <Link to="/" className="flex items-center gap-2.5">
            <Logo size="sm" variant="flame" withText />
          </Link>
          <div className="flex items-center gap-3 text-sm">
            <button onClick={scrollToFeatures} className="hidden text-muted-foreground transition-colors hover:text-foreground sm:inline">
              Features
            </button>
            <ThemeToggle size="md" />
            <button
              type="button"
              onClick={() => focusAuth('signin')}
              className={buttonVariants({ variant: 'secondary', className: 'h-9 px-4' })}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => focusAuth('signup')}
              className={buttonVariants({ variant: 'primary', className: 'h-9 px-4' })}
            >
              Get Started
            </button>
          </div>
        </div>
      </nav>

      {/* Hero — 60/40 split, display type, single accent moment */}
      <section className="relative flex min-h-screen items-center pt-16">
        <div className="mx-auto grid w-full max-w-7xl grid-cols-1 items-center gap-12 px-6 lg:grid-cols-[3fr_2fr] lg:gap-16">
          <div className="flex flex-col justify-center py-20 lg:py-0">
            <div className="mb-7 inline-flex w-fit items-center gap-2 rounded-full border border-border bg-secondary/60 px-3.5 py-1.5 text-caption font-medium text-muted-foreground">
              <Sparkles className="h-3 w-3 text-primary" />
              Built for engineers who want to get better
            </div>
            <h1 className="text-4xl font-semibold leading-[1.05] tracking-tight sm:text-5xl xl:text-display">
              Master your{' '}
              <span className="bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
                engineering craft
              </span>
            </h1>
            <p className="mt-6 max-w-md text-base leading-relaxed text-muted-foreground lg:text-lg">
              Track topics, solve problems, build consistency. Forge turns your LeetCode data into a
              personal learning engine.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-4">
              <Button size="lg" onClick={() => focusAuth('signup')} className="group">
                Get Started Free
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
              </Button>
              <Button
                variant="ghost"
                onClick={scrollToFeatures}
                className="h-auto gap-2 px-0 hover:bg-transparent"
              >
                See how it works
                <ChevronDown className="h-4 w-4" />
              </Button>
            </div>
            <div className="mt-12 flex items-center gap-8 text-sm">
              <div>
                <p className="text-lg font-semibold tabular-nums">LeetCode</p>
                <p className="text-xs text-muted-foreground">Deep Integration</p>
              </div>
              <div className="h-10 w-px bg-border" />
              <div>
                <p className="text-lg font-semibold">Spaced</p>
                <p className="text-xs text-muted-foreground">Repetition Engine</p>
              </div>
              <div className="h-10 w-px bg-border" />
              <div>
                <p className="text-lg font-semibold">Smart</p>
                <p className="text-xs text-muted-foreground">Recommendations</p>
              </div>
            </div>
          </div>

          {/* Auth card */}
          <div className="flex items-center justify-center pb-20 lg:pb-0">
            <div ref={authRef} className="w-full max-w-md scroll-mt-24">
              <AuthCard tab={authTab} onTabChange={setAuthTab} />
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="px-6 py-24">
        <div className="mx-auto max-w-6xl">
          <div className="mb-14 text-center">
            <h2 className="text-2xl font-semibold tracking-tight sm:text-page">Everything you need to level up</h2>
            <p className="mt-3 text-muted-foreground">One tool. All your data. Smarter than spreadsheets.</p>
          </div>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {features.map((f) => (
              <Card key={f.title} className="p-6 transition-colors hover:border-border/80 hover:bg-card/80">
                <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-secondary text-muted-foreground">
                  <f.icon className="h-5 w-5" />
                </div>
                <h3 className="mb-1.5 text-base font-semibold tracking-tight">{f.title}</h3>
                <p className="text-sm leading-relaxed text-muted-foreground">{f.desc}</p>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border/60 px-6 py-12">
        <div className="mx-auto grid max-w-6xl gap-10 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <Logo size="sm" variant="flame" withText />
            <p className="mt-4 text-sm leading-relaxed text-muted-foreground">
              Personal engineering companion. One tool for all your learning data.
            </p>
          </div>
          <div>
            <p className="text-sm font-semibold">Product</p>
            <ul className="mt-4 space-y-2.5 text-sm text-muted-foreground">
              <li>
                <button onClick={scrollToFeatures} className="transition-colors hover:text-foreground">Features</button>
              </li>
              <li>
                <button onClick={() => focusAuth('signup')} className="transition-colors hover:text-foreground">Get Started</button>
              </li>
            </ul>
          </div>
          <div>
            <p className="text-sm font-semibold">Account</p>
            <ul className="mt-4 space-y-2.5 text-sm text-muted-foreground">
              <li>
                <button onClick={() => focusAuth('signin')} className="transition-colors hover:text-foreground">Sign In</button>
              </li>
              <li>
                <button onClick={() => focusAuth('signup')} className="transition-colors hover:text-foreground">Create Account</button>
              </li>
            </ul>
          </div>
          <div>
            <p className="text-sm font-semibold">Contact</p>
            <ul className="mt-4 space-y-2.5 text-sm text-muted-foreground">
              <li>
                <a href="mailto:divyeshpanchasara.work@gmail.com" className="transition-colors hover:text-foreground">
                  Send feedback
                </a>
              </li>
            </ul>
          </div>
        </div>
        <div className="mx-auto mt-12 flex max-w-6xl items-center justify-between text-xs text-muted-foreground">
          <p>© {new Date().getFullYear()} Forge</p>
          <p>Personal engineering companion</p>
        </div>
      </footer>
    </div>
  );
}
