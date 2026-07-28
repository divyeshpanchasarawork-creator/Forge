import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Flame, BarChart3, BookOpen, Code2, RefreshCw, Zap, Target, ArrowRight, Sparkles, ChevronDown } from 'lucide-react';

const features = [
  { icon: BookOpen, title: 'Topic Mastery', desc: 'Track your understanding across every concept with confidence scoring and spaced repetition.' },
  { icon: Code2, title: 'LeetCode Sync', desc: 'Connect your profile. Auto-import solved problems, tags, and streaks in one click.' },
  { icon: BarChart3, title: 'Deep Analytics', desc: 'Visualize weak spots, strengths, and progress over time with rich charts.' },
  { icon: RefreshCw, title: 'Spaced Repetition', desc: 'Smart revision scheduling so nothing slips through the cracks.' },
  { icon: Target, title: 'Daily Missions', desc: 'Wake up to a focused plan. Know exactly what to work on today.' },
  { icon: Zap, title: 'Smart Insights', desc: 'AI-powered recommendations based on your actual solving patterns.' },
];

export default function LoginPage() {
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [leetcodeUsername, setLeetcodeUsername] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login, register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      if (isRegister) {
        await register({ username, password, email: email || undefined, displayName: displayName || undefined, leetcodeUsername: leetcodeUsername || undefined });
      } else {
        await login(username, password);
      }
      navigate('/');
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || '';
      if (msg.toLowerCase().includes('username already taken')) {
        setError('This username is already taken. Try another.');
      } else if (msg.toLowerCase().includes('email already in use')) {
        setError('This email is already registered.');
      } else if (isRegister) {
        setError('Registration failed. Please check your details and try again.');
      } else {
        setError('Invalid username or password.');
      }
    } finally {
      setLoading(false);
    }
  };

  const switchToRegister = () => { setIsRegister(true); setError(''); };
  const switchToLogin = () => { setIsRegister(false); setError(''); };

  const scrollToFeatures = () => {
    document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Animated background gradient */}
      <div className="pointer-events-none fixed inset-0 -z-10">
        <div className="absolute left-1/4 top-0 h-[600px] w-[600px] animate-pulse rounded-full bg-primary/8 blur-[160px]" />
        <div className="absolute right-1/4 bottom-0 h-[500px] w-[500px] animate-pulse rounded-full bg-purple-500/5 blur-[140px]" style={{ animationDelay: '2s' }} />
        <div className="absolute left-1/2 top-1/3 h-[400px] w-[400px] animate-pulse rounded-full bg-blue-500/4 blur-[120px]" style={{ animationDelay: '4s' }} />
      </div>

      {/* Navbar */}
      <nav className="fixed top-0 z-20 w-full border-b border-white/5 bg-background/80 backdrop-blur-xl">
        <div className="mx-auto flex h-16 max-w-7xl items-center justify-between px-6">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10">
              <Flame className="h-5 w-5 text-primary" />
            </div>
            <span className="text-lg font-bold tracking-tight">Forge</span>
          </div>
          <div className="flex items-center gap-6 text-sm">
            <button onClick={scrollToFeatures} className="text-muted-foreground transition-colors hover:text-foreground">Features</button>
            <button
              onClick={switchToLogin}
              className="rounded-lg border border-border bg-secondary/50 px-4 py-2 text-sm font-medium text-foreground transition-all hover:border-primary/30 hover:bg-secondary"
            >
              Sign In
            </button>
            <button
              onClick={() => { switchToRegister(); setTimeout(() => document.getElementById('auth-section')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 100); }}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-all hover:bg-primary/90"
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
                onClick={() => { switchToRegister(); setTimeout(() => document.getElementById('auth-section')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 100); }}
                className="group flex items-center gap-2 rounded-xl bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/25 transition-all hover:shadow-xl hover:shadow-primary/30 hover:brightness-110"
              >
                Get Started Free
                <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
              </button>
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

          {/* Right - Auth Card */}
          <div id="auth-section" className="flex items-center justify-center pt-8 lg:pt-0">
            <div className="w-full max-w-md rounded-2xl border border-white/10 bg-card/70 p-8 shadow-2xl shadow-black/30 backdrop-blur-xl" style={{ minHeight: '480px' }}>
              {/* Tabs */}
              <div className="flex rounded-xl bg-secondary p-1">
                <button
                  type="button"
                  onClick={switchToLogin}
                  className={`flex-1 rounded-lg py-2.5 text-sm font-medium transition-all ${!isRegister ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'}`}
                >
                  Sign In
                </button>
                <button
                  type="button"
                  onClick={switchToRegister}
                  className={`flex-1 rounded-lg py-2.5 text-sm font-medium transition-all ${isRegister ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'}`}
                >
                  Create Account
                </button>
              </div>

              <p className="mt-5 text-center text-sm text-muted-foreground">
                {isRegister ? 'Start mastering your craft in 30 seconds' : 'Welcome back. Sign in to continue.'}
              </p>

              <form onSubmit={handleSubmit} className="mt-5 flex flex-1 flex-col space-y-3.5">
                <div>
                  <label className="block text-sm font-medium text-muted-foreground mb-1.5">Username</label>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="w-full rounded-lg border border-input bg-secondary/50 px-4 py-2.5 text-foreground placeholder:text-muted-foreground/50 transition-all focus:border-primary focus:bg-secondary focus:outline-none focus:ring-1 focus:ring-primary"
                    placeholder="your_username"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-muted-foreground mb-1.5">Password</label>
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full rounded-lg border border-input bg-secondary/50 px-4 py-2.5 text-foreground placeholder:text-muted-foreground/50 transition-all focus:border-primary focus:bg-secondary focus:outline-none focus:ring-1 focus:ring-primary"
                    placeholder="minimum 6 characters"
                    required
                  />
                </div>

                {isRegister && (
                  <div className="space-y-3.5">
                    <div>
                      <label className="block text-sm font-medium text-muted-foreground mb-1.5">Display Name <span className="text-muted-foreground/50">(optional)</span></label>
                      <input
                        type="text"
                        value={displayName}
                        onChange={(e) => setDisplayName(e.target.value)}
                        className="w-full rounded-lg border border-input bg-secondary/50 px-4 py-2.5 text-foreground placeholder:text-muted-foreground/50 transition-all focus:border-primary focus:bg-secondary focus:outline-none focus:ring-1 focus:ring-primary"
                        placeholder="Forge User"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-muted-foreground mb-1.5">Email <span className="text-muted-foreground/50">(optional)</span></label>
                      <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="w-full rounded-lg border border-input bg-secondary/50 px-4 py-2.5 text-foreground placeholder:text-muted-foreground/50 transition-all focus:border-primary focus:bg-secondary focus:outline-none focus:ring-1 focus:ring-primary"
                        placeholder="you@example.com"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-muted-foreground mb-1.5">LeetCode Username <span className="text-muted-foreground/50">(optional)</span></label>
                      <input
                        type="text"
                        value={leetcodeUsername}
                        onChange={(e) => setLeetcodeUsername(e.target.value)}
                        className="w-full rounded-lg border border-input bg-secondary/50 px-4 py-2.5 text-foreground placeholder:text-muted-foreground/50 transition-all focus:border-primary focus:bg-secondary focus:outline-none focus:ring-1 focus:ring-primary"
                        placeholder="your_leetcode_id"
                      />
                      <p className="mt-1.5 text-xs text-muted-foreground/60">We'll auto-import your solved problems and stats</p>
                    </div>
                  </div>
                )}

                <div className="flex-1" />

                {error && (
                  <div className="rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">
                    {error}
                  </div>
                )}

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full rounded-xl bg-primary py-3 text-sm font-semibold text-primary-foreground shadow-lg shadow-primary/20 transition-all hover:bg-primary/90 hover:shadow-xl hover:shadow-primary/25 disabled:opacity-50"
                >
                  {loading ? (
                    <span className="flex items-center justify-center gap-2">
                      <span className="h-4 w-4 animate-spin rounded-full border-2 border-primary-foreground/30 border-t-primary-foreground" />
                      {isRegister ? 'Creating account...' : 'Signing in...'}
                    </span>
                  ) : (
                    isRegister ? 'Create Account' : 'Sign In'
                  )}
                </button>
              </form>

              <div className="mt-5 text-center text-sm text-muted-foreground">
                {isRegister ? (
                  <>Already have an account?{'  '}
                    <button onClick={switchToLogin} className="font-medium text-primary transition-colors hover:text-primary/80 hover:underline">Sign in</button>
                  </>
                ) : (
                  <>Don't have an account?{'  '}
                    <button onClick={switchToRegister} className="font-medium text-primary transition-colors hover:text-primary/80 hover:underline">Create one</button>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="relative border-t border-white/5 px-6 py-24">
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
                className="group rounded-xl border border-white/5 bg-card/30 p-6 transition-all hover:border-primary/20 hover:bg-card/60 hover:shadow-lg hover:shadow-primary/5"
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
      <footer className="border-t border-white/5 px-6 py-8">
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
