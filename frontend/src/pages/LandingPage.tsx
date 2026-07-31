import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Flame, BarChart3, BookOpen, Code2, RefreshCw, Zap, Target, ArrowRight, Sparkles, ChevronDown, X } from 'lucide-react';
import { useState } from 'react';

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
  const [showSignedOut, setShowSignedOut] = useState(
    (location.state as { signedOut?: boolean } | null)?.signedOut === true
  );

  const dismissSignedOut = () => {
    setShowSignedOut(false);
    navigate(location.pathname, { replace: true });
  };

  const scrollToFeatures = () => {
    document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Signed-out acknowledgment */}
      {showSignedOut && (
        <div className="fixed left-1/2 top-20 z-30 -translate-x-1/2 px-4">
          <div className="flex items-center gap-3 rounded-full border border-green-500/30 bg-card px-4 py-2 text-sm shadow-lg">
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
      <nav className="fixed top-0 z-20 w-full border-b border-border bg-background/80 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10">
              <Flame className="h-5 w-5 text-primary" />
            </div>
            <span className="text-lg font-bold tracking-tight">Forge</span>
          </div>
          <div className="flex items-center gap-6 text-sm">
            <button onClick={scrollToFeatures} className="hidden text-muted-foreground transition-colors hover:text-foreground sm:inline">Features</button>
            <Link
              to="/login"
              className="rounded-lg border border-border bg-secondary/50 px-4 py-2 text-sm font-medium text-foreground transition-all hover:border-primary/30 hover:bg-secondary"
            >
              Sign In
            </Link>
            <Link
              to="/register"
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-all hover:bg-primary/90"
            >
              Get Started
            </Link>
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
              <Link
                to="/register"
                className="group flex items-center gap-2 rounded-xl bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-all hover:shadow-xl hover:shadow-primary/30 hover:brightness-110"
              >
                Get Started Free
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
              </Link>
              <button onClick={scrollToFeatures} className="flex items-center gap-2 text-sm text-muted-foreground transition-colors hover:text-foreground">
                See how it works
                <ChevronDown className="h-4 w-4" />
              </button>
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

          {/* Right - Visual */}
          <div className="flex items-center justify-center pt-8 lg:pt-0">
            <div className="w-full max-w-md rounded-2xl border border-border bg-card/70 p-8 shadow-2xl shadow-black/10 backdrop-blur-xl">
              <div className="mb-6 flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10">
                  <Flame className="h-6 w-6 text-primary" />
                </div>
                <div>
                  <p className="font-semibold">Your daily forge</p>
                  <p className="text-xs text-muted-foreground">Personalized plan, every morning</p>
                </div>
              </div>
              <div className="space-y-3">
                {[
                  ['Knowledge health at a glance', 'Mastery, confidence, and retention in one view'],
                  ['Recommendations that adapt', 'Generated from your real solving patterns'],
                  ['Practice queue that learns', 'Weak-tag and plan problems, prioritized for you'],
                  ['Spaced revision, never forgotten', 'SM-2 scheduling tuned to your pace'],
                ].map(([title, desc]) => (
                  <div key={title} className="rounded-xl bg-secondary/50 p-4">
                    <p className="text-sm font-medium">{title}</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">{desc}</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="relative border-t border-border px-6 py-24">
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
              <div
                key={f.title}
                className="group rounded-xl border border-border bg-card/30 p-6 transition-all hover:border-primary/20 hover:bg-card/60 hover:shadow-lg hover:shadow-primary/5"
              >
                <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-primary/10 transition-colors group-hover:bg-primary/15">
                  <f.icon className="h-5 w-5 text-primary" />
                </div>
                <h3 className="mb-2 text-base font-semibold">{f.title}</h3>
                <p className="text-sm leading-relaxed text-muted-foreground">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border px-6 py-8">
        <div className="mx-auto flex max-w-6xl items-center justify-between text-sm text-muted-foreground">
          <div className="flex items-center gap-2.5">
            <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/10">
              <Flame className="h-4 w-4 text-primary" />
            </div>
            <span className="font-semibold">Forge</span>
          </div>
          <p className="text-xs">Personal engineering companion</p>
        </div>
      </footer>
    </div>
  );
}
