import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { authApi } from '@/api';
import { parseApiError } from '@/lib/error';
import { getTargetLevel } from '@/lib/targetLevels';
import { Flame, Code2, ArrowRight } from 'lucide-react';

export default function OnboardingPage() {
  const { user, setUser } = useAuth();
  const navigate = useNavigate();
  const [targetLevel, setTargetLevel] = useState(user?.targetLevel ?? 5);
  const [leetcodeUsername, setLeetcodeUsername] = useState(user?.leetcodeUsername ?? '');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  const config = getTargetLevel(targetLevel);

  const saveAndContinue = async (skipLeetcode = false) => {
    setError('');
    setSaving(true);
    try {
      const res = await authApi.updateProfile({
        targetLevel,
        leetcodeUsername: skipLeetcode ? undefined : leetcodeUsername || undefined,
      });
      setUser(res.data.data);
      navigate('/app');
    } catch (err: unknown) {
      setError(parseApiError(err));
      setSaving(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-background px-4 py-16">
      <div className="pointer-events-none fixed inset-0 -z-10">
        <div className="absolute left-1/4 top-0 h-[600px] w-[600px] animate-pulse rounded-full bg-primary/8 blur-[160px]" />
        <div className="absolute right-1/4 bottom-0 h-[500px] w-[500px] animate-pulse rounded-full bg-purple-500/5 blur-[140px]" style={{ animationDelay: '2s' }} />
      </div>

      <div className="w-full max-w-lg space-y-6">
        <div className="flex flex-col items-center gap-3 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
            <Flame className="h-7 w-7 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight">Set up your forge</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Two quick steps to personalize your learning engine.
            </p>
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-6">
          {/* Step 1: Target level */}
          <div className="space-y-4">
            <div>
              <p className="text-sm font-semibold">1 · What's your career target?</p>
              <p className="mt-0.5 text-xs text-muted-foreground">
                Recommendations, difficulty mix, and problem goals adapt to this level.
              </p>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-sm font-medium">Level {targetLevel}</span>
              <span className={`text-sm font-semibold ${config.color}`}>{config.label}</span>
            </div>

            <input
              type="range"
              min={1}
              max={10}
              value={targetLevel}
              onChange={(e) => setTargetLevel(Number(e.target.value))}
              className="w-full accent-primary"
            />

            <div className="rounded-xl bg-secondary/50 p-4">
              <div className="grid grid-cols-4 gap-2 text-center">
                <div>
                  <p className="text-lg font-bold text-primary">{config.targetTotal}</p>
                  <p className="text-xs text-muted-foreground">Problems</p>
                </div>
                <div>
                  <p className="text-lg font-bold text-green-400">{config.easyPct}%</p>
                  <p className="text-xs text-muted-foreground">Easy</p>
                </div>
                <div>
                  <p className="text-lg font-bold text-yellow-400">{config.mediumPct}%</p>
                  <p className="text-xs text-muted-foreground">Medium</p>
                </div>
                <div>
                  <p className="text-lg font-bold text-red-400">{config.hardPct}%</p>
                  <p className="text-xs text-muted-foreground">Hard</p>
                </div>
              </div>
              <p className="mt-3 text-center text-xs text-muted-foreground">{config.companies}</p>
            </div>
          </div>

          {/* Step 2: LeetCode */}
          <div className="mt-6 border-t border-border pt-5">
            <div>
              <p className="flex items-center gap-1.5 text-sm font-semibold">
                <Code2 className="h-4 w-4 text-primary" />
                2 · Connect LeetCode (optional)
              </p>
              <p className="mt-0.5 text-xs text-muted-foreground">
                Skip for now — you can sync anytime from Profile.
              </p>
            </div>
            <input
              type="text"
              value={leetcodeUsername}
              onChange={(e) => setLeetcodeUsername(e.target.value)}
              className="mt-3 w-full rounded-lg border border-input bg-secondary/50 px-4 py-2.5 text-foreground placeholder:text-muted-foreground/50 transition-all focus:border-primary focus:bg-secondary focus:outline-none focus:ring-1 focus:ring-primary"
              placeholder="your_leetcode_username"
            />
          </div>

          {error && (
            <div className="mt-5 rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">
              {error}
            </div>
          )}
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => saveAndContinue(true)}
            disabled={saving}
            className="flex-1 rounded-xl border border-border bg-secondary/50 px-5 py-3 text-sm font-medium text-foreground transition-colors hover:bg-secondary disabled:opacity-50"
          >
            Skip for now
          </button>
          <button
            onClick={() => saveAndContinue(false)}
            disabled={saving}
            className="group flex flex-[2] items-center justify-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground transition-all hover:bg-primary/90 disabled:opacity-50"
          >
            {saving ? 'Saving...' : 'Start Forging'}
            <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
          </button>
        </div>
      </div>
    </div>
  );
}
