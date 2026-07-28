import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Flame, BarChart3, BookOpen, Code2, RefreshCw, ArrowRight, Zap, Target } from 'lucide-react';

const features = [
  { icon: BookOpen, title: 'Topic Mastery', desc: 'Track your understanding across every concept with confidence scoring.' },
  { icon: Code2, title: 'LeetCode Sync', desc: 'Connect your profile. Auto-import solved problems, tags, and streaks.' },
  { icon: BarChart3, title: 'Deep Analytics', desc: 'Visualize weak spots, strengths, and progress over time.' },
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

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Navbar */}
      <nav className="relative z-10 flex items-center justify-between border-b border-border px-6 py-4 lg:px-12">
        <div className="flex items-center gap-2.5">
          <Flame className="h-7 w-7 text-primary" />
          <span className="text-xl font-bold tracking-tight">Forge</span>
        </div>
        <div className="flex items-center gap-6 text-sm text-muted-foreground">
          <a href="#features" className="transition-colors hover:text-foreground">Features</a>
          <a href="#hero" className="transition-colors hover:text-foreground">Get Started</a>
          <div className="h-4 w-px bg-border" />
          <button
            onClick={switchToLogin}
            className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-secondary"
          >
            Sign In
          </button>
        </div>
      </nav>

      {/* Hero Section */}
      <section id="hero" className="relative flex flex-1 items-center overflow-hidden">
        <div className="pointer-events-none absolute left-1/2 top-1/2 -z-10 h-[600px] w-[600px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary/5 blur-[120px]" />

        <div className="mx-auto grid w-full max-w-7xl grid-cols-1 gap-12 px-6 py-16 lg:grid-cols-2 lg:gap-0 lg:py-0">
          {/* Left — Branding */}
          <div className="flex flex-col justify-center space-y-8">
            <div className="space-y-4">
              <div className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-4 py-1.5 text-xs font-medium text-primary">
                <Zap className="h-3 w-3" />
                Built for engineers who want to get better
              </div>
              <h1 className="text-4xl font-bold leading-tight tracking-tight lg:text-5xl xl:text-6xl">
                Master your
                <br />
                <span className="text-primary">engineering craft</span>
              </h1>
              <p className="max-w-md text-lg text-muted-foreground">
                Track topics, solve problems, build consistency. Forge connects to your LeetCode profile and turns raw data into a personalized learning engine.
              </p>
            </div>

            <div className="flex items-center gap-4">
              <button
                onClick={() => { switchToRegister(); setTimeout(() => document.getElementById('auth-form')?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 100); }}
                className="flex items-center gap-2 rounded-lg bg-primary px-6 py-3 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
              >
                Get Started Free
                <ArrowRight className="h-4 w-4" />
              </button>
              <a href="#features" className="text-sm text-muted-foreground transition-colors hover:text-foreground">
                See how it works
              </a>
            </div>

            <div className="flex gap-8 pt-4">
              <div>
                <p className="text-2xl font-bold">LeetCode</p>
                <p className="text-xs text-muted-foreground">Deep Integration</p>
              </div>
              <div className="h-12 w-px bg-border" />
              <div>
                <p className="text-2xl font-bold">Spaced</p>
                <p className="text-xs text-muted-foreground">Repetition Engine</p>
              </div>
              <div className="h-12 w-px bg-border" />
              <div>
                <p className="text-2xl font-bold">Smart</p>
                <p className="text-xs text-muted-foreground">Recommendations</p>
              </div>
            </div>
          </div>

          {/* Right — Auth Card */}
          <div id="auth-form" className="flex items-center justify-center">
            <div className="flex w-full max-w-md flex-col rounded-2xl border border-border bg-card p-8 shadow-2xl shadow-black/20" style={{ minHeight: '480px' }}>
              {/* Tabs */}
              <div className="flex rounded-lg bg-secondary p-1">
                <button
                  type="button"
                  onClick={switchToLogin}
                  className={`flex-1 rounded-md py-2.5 text-sm font-medium transition-colors ${!isRegister ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'}`}
                >
                  Sign In
                </button>
                <button
                  type="button"
                  onClick={switchToRegister}
                  className={`flex-1 rounded-md py-2.5 text-sm font-medium transition-colors ${isRegister ? 'bg-card text-foreground shadow-sm' : 'text-muted-foreground hover:text-foreground'}`}
                >
                  Create Account
                </button>
              </div>

              <p className="mt-4 text-center text-sm text-muted-foreground">
                {isRegister ? 'Start mastering your craft in 30 seconds' : 'Welcome back. Sign in to continue.'}
              </p>

              <form onSubmit={handleSubmit} className="mt-4 flex flex-1 flex-col space-y-4">
                <div>
                  <label className="block text-sm font-medium text-muted-foreground mb-1.5">Username</label>
                  <input
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
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
                    className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                    placeholder="minimum 6 characters"
                    required
                  />
                </div>

                {isRegister && (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-muted-foreground mb-1.5">Display Name <span className="text-muted-foreground/50">(optional)</span></label>
                      <input
                        type="text"
                        value={displayName}
                        onChange={(e) => setDisplayName(e.target.value)}
                        className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                        placeholder="Forge User"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-muted-foreground mb-1.5">Email <span className="text-muted-foreground/50">(optional)</span></label>
                      <input
                        type="email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                        placeholder="you@example.com"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-muted-foreground mb-1.5">LeetCode Username <span className="text-muted-foreground/50">(optional)</span></label>
                      <input
                        type="text"
                        value={leetcodeUsername}
                        onChange={(e) => setLeetcodeUsername(e.target.value)}
                        className="w-full rounded-lg border border-input bg-secondary px-4 py-2.5 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                        placeholder="your_leetcode_id"
                      />
                      <p className="mt-1.5 text-xs text-muted-foreground">We'll auto-import your solved problems and stats</p>
                    </div>
                  </>
                )}

                {/* Spacer pushes error + button to bottom */}
                <div className="flex-1" />

                {error && (
                  <div className="rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">
                    {error}
                  </div>
                )}

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full rounded-lg bg-primary py-2.5 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
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

              <div className="mt-4 text-center text-xs text-muted-foreground">
                {isRegister ? (
                  <>Already have an account?{' '}
                    <button onClick={switchToLogin} className="font-medium text-primary hover:underline">Sign in</button>
                  </>
                ) : (
                  <>Don't have an account?{' '}
                    <button onClick={switchToRegister} className="font-medium text-primary hover:underline">Create one</button>
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="border-t border-border bg-card/50 px-6 py-20">
        <div className="mx-auto max-w-6xl">
          <div className="mb-12 text-center">
            <h2 className="text-3xl font-bold tracking-tight">Everything you need to level up</h2>
            <p className="mt-3 text-muted-foreground">One tool. All your data. Smarter than spreadsheets.</p>
          </div>
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {features.map((f) => (
              <div key={f.title} className="group rounded-xl border border-border bg-card p-6 transition-colors hover:border-primary/30 hover:bg-primary/5">
                <div className="mb-4 flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
                  <f.icon className="h-5 w-5 text-primary" />
                </div>
                <h3 className="mb-1.5 text-sm font-semibold">{f.title}</h3>
                <p className="text-sm leading-relaxed text-muted-foreground">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border px-6 py-8">
        <div className="mx-auto flex max-w-6xl items-center justify-between text-sm text-muted-foreground">
          <div className="flex items-center gap-2">
            <Flame className="h-4 w-4 text-primary" />
            <span className="font-medium">Forge</span>
          </div>
          <p>Personal engineering companion</p>
        </div>
      </footer>
    </div>
  );
}
