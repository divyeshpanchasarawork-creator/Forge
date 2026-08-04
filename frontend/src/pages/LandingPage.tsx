import { Link, useLocation, useNavigate } from 'react-router-dom';
import { BarChart3, BookOpen, Code2, RefreshCw, Zap, Target, ArrowRight, Sparkles, ChevronDown, X, CheckCircle2 } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Button, buttonVariants } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Logo } from '@/components/brand/Logo';
import ThemeToggle from '@/components/ui/ThemeToggle';
import AuthCard, { type AuthTab } from '@/components/auth/AuthCard';

const features = [
  { icon: BookOpen, title: 'Topic Mastery', desc: 'Track your understanding across every concept with confidence scoring and spaced repetition.' },
  { icon: Code2, title: 'LeetCode Sync', desc: 'Connect your profile. Auto-import solved problems, tags, and streaks in one click.' },
  { icon: BarChart3, title: 'Deep Analytics', desc: 'Visualize weak spots, strengths, and progress over time with rich charts.' },
  { icon: RefreshCw, title: 'Spaced Repetition', desc: 'Smart revision scheduling so nothing slips through the cracks.' },
  { icon: Target, title: 'Daily Missions', desc: 'Wake up to a focused plan. Know exactly what to work on today.' },
  { icon: Zap, title: 'Smart Insights', desc: 'AI-powered recommendations based on your actual solving patterns.' },
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
          <div className="flex items-center gap-3 rounded-full border border-green-500/30 bg-card px-4 py-2 text-sm">
            <CheckCircle2 className="h-4 w-4 text-green-400" />
            <span className="font-medium text-green-400">Signed out</span>
            <span className="text-muted-foreground">See you soon — your progress is saved.</span>
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
      {/* Animated background gradient */}
      <div className="pointer-events-none fixed inset-0 -z-10">
        <div className="absolute left-1/4 top-0 h-[600px] w-[600px] animate-pulse rounded-full bg-primary/8 blur-[160px]" />
        <div className="absolute right-1/4 bottom-0 h-[500px] w-[500px] animate-pulse rounded-full bg-purple-500/5 blur-[140px]" style={{ animationDelay: '2s' }} />
        <div className="absolute left-1/2 top-1/3 h-[400px] w-[400px] animate-pulse rounded-full bg-blue-500/4 blur-[120px]" style={{ animationDelay: '4s' }} />
      </div>

      {/* Navbar */}
      <nav className="fixed top-0 z-20 w-full bg-background/80 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
          <Link to="/" className="flex items-center gap-2.5">
            <Logo size="sm" variant="flame" withText />
          </Link>
          <div className="flex items-center gap-3 text-sm">
            <button onClick={scrollToFeatures} className="hidden text-muted-foreground transition-colors hover:text-foreground sm:inline">Features</button>
            <ThemeToggle size="md" />
            <button
              type="button"
              onClick={() => focusAuth('signin')}
              className={buttonVariants({ variant: 'outline', className: 'h-9 px-4' })}
            >
              Sign In
            </button>
            <button
              type="button"
              onClick={() => focusAuth('signup')}
              className={buttonVariants({ className: 'h-9 px-4' })}
            >
              Get Started
            </button>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="relative flex min-h-screen items-center pt-16">
        <div className="mx-auto grid w-full max-w-7xl grid-cols-1 gap-12 px-6 lg:grid-cols-2 lg:gap-16">
          {/* Left - Branding */}
          <div className="flex flex-col justify-center pt-16 lg:pt-0">
            <div className="mb-6 inline-flex w-fit items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-4 py-1.5 text-xs font-medium text-primary">
              <Sparkles className="h-3 w-3" />
              Built for engineers who want to get better
            </div>
            <h1 className="text-4xl font-extrabold leading-[1.1] tracking-tight lg:text-5xl xl:text-6xl">
              Master your{' '}
              <span className="bg-gradient-to-r from-primary via-purple-400 to-blue-400 bg-clip-text text-transparent">
                engineering craft
              </span>
            </h1>
            <p className="mt-6 max-w-md text-base leading-relaxed text-muted-foreground lg:text-lg">
              Track topics, solve problems, build consistency. Forge connects to your LeetCode profile and turns raw data into a personalized learning engine.
            </p>
            <div className="mt-8 flex flex-wrap items-center gap-4">
              <button
                type="button"
                onClick={() => focusAuth('signup')}
                className={buttonVariants({ size: 'lg', className: 'group rounded-xl hover:brightness-110' })}
              >
                Get Started Free
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
              </button>
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
                <p className="text-xl font-bold text-foreground">LeetCode</p>
                <p className="text-xs text-muted-foreground">Deep Integration</p>
              </div>
              <div className="h-10 w-px bg-border" />
              <div>
                <p className="text-xl font-bold text-foreground">Spaced</p>
                <p className="text-xs text-muted-foreground">Repetition Engine</p>
              </div>
              <div className="h-10 w-px bg-border" />
              <div>
                <p className="text-xl font-bold text-foreground">Smart</p>
                <p className="text-xs text-muted-foreground">Recommendations</p>
              </div>
            </div>
          </div>

          {/* Right - Auth card */}
          <div className="flex items-center justify-center pt-8 lg:pt-0">
            <div ref={authRef} className="w-full max-w-md scroll-mt-24">
              <AuthCard tab={authTab} onTabChange={setAuthTab} />
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="relative px-6 py-24">
        <div className="pointer-events-none absolute inset-0 -z-10">
          <div className="absolute left-1/3 top-0 h-[400px] w-[400px] rounded-full bg-primary/3 blur-[100px]" />
        </div>
        <div className="mx-auto max-w-6xl">
          <div className="mb-16 text-center">
            <h2 className="text-3xl font-bold tracking-tight lg:text-4xl">Everything you need to level up</h2>
            <p className="mt-4 text-muted-foreground">One tool. All your data. Smarter than spreadsheets.</p>
          </div>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {features.map((f) => (
              <Card
                key={f.title}
                className="group bg-card/30 p-6 transition-all hover:border-primary/20 hover:bg-card/60"
              >
                <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 transition-colors group-hover:bg-primary/15">
                  <f.icon className="h-5 w-5 text-primary" />
                </div>
                <h3 className="mb-2 text-base font-semibold">{f.title}</h3>
                <p className="text-sm leading-relaxed text-muted-foreground">{f.desc}</p>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="px-6 py-12">
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
                <button onClick={scrollToFeatures} className="transition-colors hover:text-foreground">
                  Features
                </button>
              </li>
              <li>
                <button onClick={() => focusAuth('signup')} className="transition-colors hover:text-foreground">
                  Get Started
                </button>
              </li>
            </ul>
          </div>
          <div>
            <p className="text-sm font-semibold">Account</p>
            <ul className="mt-4 space-y-2.5 text-sm text-muted-foreground">
              <li>
                <button onClick={() => focusAuth('signin')} className="transition-colors hover:text-foreground">
                  Sign In
                </button>
              </li>
              <li>
                <button onClick={() => focusAuth('signup')} className="transition-colors hover:text-foreground">
                  Create Account
                </button>
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
