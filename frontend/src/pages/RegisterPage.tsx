import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertCircle, ArrowLeft, Check, Eye, EyeOff, X } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { parseApiError } from '@/lib/error';
import { cn } from '@/lib/utils';
import AuthShell from '@/components/layout/AuthShell';
import { Button } from '@/components/ui/Button';

const PASSWORD_RULE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

export default function RegisterPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const checks = [
    { label: '8+ characters', ok: password.length >= 8 },
    { label: 'Uppercase', ok: /[A-Z]/.test(password) },
    { label: 'Lowercase', ok: /[a-z]/.test(password) },
    { label: 'Number', ok: /\d/.test(password) },
    { label: 'Special', ok: /[^A-Za-z0-9]/.test(password) },
  ];
  const passwordTouched = password.length > 0;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!PASSWORD_RULE.test(password)) {
      setError('Password must be at least 8 characters with uppercase, lowercase, a number, and a special character.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      await register({ email, password });
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

  const inputClass =
    'w-full rounded-lg border border-input bg-secondary/50 px-4 py-2.5 text-foreground placeholder:text-muted-foreground/50 transition-all focus:border-primary focus:bg-secondary focus:outline-none focus:ring-1 focus:ring-primary';

  const passwordField = (id: string, value: string, onChange: (v: string) => void, placeholder: string, autoComplete: string, show: boolean, setShow: (v: boolean) => void, ariaLabel: string) => (
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

  return (
    <AuthShell title="Create your account" subtitle="Start building your learning engine">
      <form onSubmit={handleSubmit} className="space-y-4">
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
            autoFocus
            required
          />
        </div>
        <div>
          <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-muted-foreground">
            Password
          </label>
          {passwordField('password', password, setPassword, '8+ chars, upper+lower+digit+special', 'new-password', showPassword, setShowPassword, showPassword ? 'Hide password' : 'Show password')}
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

        {error && (
          <div className="flex items-start gap-2 rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <Button type="submit" size="lg" className="w-full" loading={loading}>
          {loading ? 'Creating account...' : 'Create Account'}
        </Button>
      </form>

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-primary transition-colors hover:text-primary/80 hover:underline">
          Sign in
        </Link>
      </p>
      <p className="mt-4 text-center">
        <Link
          to="/"
          className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground transition-colors hover:text-primary"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Back to home
        </Link>
      </p>
    </AuthShell>
  );
}
