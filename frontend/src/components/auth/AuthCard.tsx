import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, Check, Eye, EyeOff, X } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { useAuth } from '@/contexts/AuthContext';
import { parseApiError } from '@/lib/error';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/Button';

const PASSWORD_RULE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

const inputClass =
  'w-full rounded-lg border border-input bg-secondary/50 px-4 py-2.5 text-foreground placeholder:text-muted-foreground/50 transition-all focus:border-primary focus:bg-secondary focus:outline-none focus:ring-1 focus:ring-primary';

export type AuthTab = 'signin' | 'signup';

interface AuthCardProps {
  tab: AuthTab;
  onTabChange: (tab: AuthTab) => void;
}

export default function AuthCard({ tab, onTabChange }: AuthCardProps) {
  const navigate = useNavigate();
  const { login, register } = useAuth();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const [email, setEmail] = useState('');
  const [signupPassword, setSignupPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showSignupPassword, setShowSignupPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (tab === 'signin' && email && !username) setUsername(email);
  }, [tab, email, username]);

  const checks = [
    { label: '8+ characters', ok: signupPassword.length >= 8 },
    { label: 'Uppercase', ok: /[A-Z]/.test(signupPassword) },
    { label: 'Lowercase', ok: /[a-z]/.test(signupPassword) },
    { label: 'Number', ok: /\d/.test(signupPassword) },
    { label: 'Special', ok: /[^A-Za-z0-9]/.test(signupPassword) },
  ];
  const passwordTouched = signupPassword.length > 0;

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(username, password);
      navigate('/app');
    } catch (err: unknown) {
      const msg = parseApiError(err);
      if (msg.toLowerCase().includes('disabled') || msg.toLowerCase().includes('locked')) {
        setError(msg);
      } else {
        setError('Invalid username or password.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!PASSWORD_RULE.test(signupPassword)) {
      setError('Password must be at least 8 characters with uppercase, lowercase, a number, and a special character.');
      return;
    }
    if (signupPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    setLoading(true);
    try {
      await register({ email, password: signupPassword });
      navigate('/onboarding');
    } catch (err: unknown) {
      const msg = parseApiError(err);
      if (msg.toLowerCase().includes('email already') || msg.toLowerCase().includes('already registered')) {
        setError('This email is already registered.');
      } else if (msg.toLowerCase().includes('username already')) {
        setError('This username is already taken.');
      } else {
        setError('Registration failed. Please check your details.');
      }
    } finally {
      setLoading(false);
    }
  };

  const passwordField = (
    id: string,
    value: string,
    onChange: (v: string) => void,
    placeholder: string,
    autoComplete: string,
    show: boolean,
    setShow: (v: boolean) => void,
    ariaLabel: string
  ) => (
    <div className="relative">
      <input
        id={id}
        type={show ? 'text' : 'password'}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className={`${inputClass} pr-10`}
        placeholder={placeholder}
        autoComplete={autoComplete}
        required
      />
      <button
        type="button"
        onClick={() => setShow(!show)}
        aria-label={ariaLabel}
        className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground transition-colors hover:text-foreground"
      >
        {show ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
      </button>
    </div>
  );

  const errorBox = error ? (
    <div className="flex items-start gap-2 rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">
      <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
      <span>{error}</span>
    </div>
  ) : null;

  return (
    <div className="rounded-2xl border border-border bg-card/70 p-6 shadow-2xl backdrop-blur-xl sm:p-8">
      <div className="flex rounded-lg bg-secondary/60 p-1">
        <button
          type="button"
          onClick={() => onTabChange('signin')}
          className={cn(
            'flex-1 rounded-md py-2 text-sm font-medium transition-colors',
            tab === 'signin' ? 'bg-card text-foreground shadow' : 'text-muted-foreground hover:text-foreground'
          )}
        >
          Sign In
        </button>
        <button
          type="button"
          onClick={() => onTabChange('signup')}
          className={cn(
            'flex-1 rounded-md py-2 text-sm font-medium transition-colors',
            tab === 'signup' ? 'bg-card text-foreground shadow' : 'text-muted-foreground hover:text-foreground'
          )}
        >
          Create Account
        </button>
      </div>

      <div className="mt-6 mb-8 text-center">
        <h2 className="text-lg font-bold tracking-tight">
          {tab === 'signin' ? 'Welcome back' : 'Create your account'}
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          {tab === 'signin' ? 'Sign in to continue your forge' : 'Start building your learning engine'}
        </p>
      </div>

      <AnimatePresence mode="wait" initial={false}>
        {tab === 'signin' ? (
          <motion.form
            key="signin"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.18 }}
            onSubmit={handleLogin}
            className="space-y-4"
          >
            <div>
              <label htmlFor="username" className="mb-1.5 block text-sm font-medium text-muted-foreground">
                Email or Username
              </label>
              <input
                id="username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className={inputClass}
                placeholder="you@example.com"
                autoComplete="username"
                autoFocus
                required
              />
            </div>
            <div>
              <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-muted-foreground">
                Password
              </label>
              {passwordField('password', password, setPassword, 'your password', 'current-password', showPassword, setShowPassword, showPassword ? 'Hide password' : 'Show password')}
            </div>
            {errorBox}
            <Button type="submit" size="lg" className="w-full" loading={loading}>
              {loading ? 'Signing in...' : 'Sign In'}
            </Button>
          </motion.form>
        ) : (
          <motion.form
            key="signup"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.18 }}
            onSubmit={handleRegister}
            className="space-y-4"
          >
            <div>
              <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-muted-foreground">
                Email
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className={inputClass}
                placeholder="you@example.com"
                autoComplete="email"
                required
              />
            </div>
            <div>
              <label htmlFor="signup-password" className="mb-1.5 block text-sm font-medium text-muted-foreground">
                Password
              </label>
              {passwordField('signup-password', signupPassword, setSignupPassword, '8+ chars, upper+lower+digit+special', 'new-password', showSignupPassword, setShowSignupPassword, showSignupPassword ? 'Hide password' : 'Show password')}
              {passwordTouched && (
                <div className="mt-2 flex flex-wrap gap-1.5">
                  {checks.map((c) => (
                    <span
                      key={c.label}
                      className={cn(
                        'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-medium transition-colors',
                        c.ok ? 'bg-green-500/10 text-green-400' : 'bg-secondary text-muted-foreground'
                      )}
                    >
                      {c.ok ? <Check className="h-3 w-3" /> : <X className="h-3 w-3" />}
                      {c.label}
                    </span>
                  ))}
                </div>
              )}
            </div>
            <div>
              <label htmlFor="confirmPassword" className="mb-1.5 block text-sm font-medium text-muted-foreground">
                Confirm Password
              </label>
              {passwordField('confirmPassword', confirmPassword, setConfirmPassword, 'repeat your password', 'new-password', showConfirm, setShowConfirm, showConfirm ? 'Hide password' : 'Show password')}
            </div>
            {errorBox}
            <Button type="submit" size="lg" className="w-full" loading={loading}>
              {loading ? 'Creating account...' : 'Create Account'}
            </Button>
          </motion.form>
        )}
      </AnimatePresence>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        {tab === 'signin' ? (
          <>
            Don't have an account?{' '}
            <button
              type="button"
              onClick={() => onTabChange('signup')}
              className="font-medium text-primary transition-colors hover:text-primary/80 hover:underline"
            >
              Create one
            </button>
          </>
        ) : (
          <>
            Already have an account?{' '}
            <button
              type="button"
              onClick={() => onTabChange('signin')}
              className="font-medium text-primary transition-colors hover:text-primary/80 hover:underline"
            >
              Sign in
            </button>
          </>
        )}
      </p>
    </div>
  );
}
