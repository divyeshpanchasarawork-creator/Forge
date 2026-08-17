import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { authApi, unwrap } from '@/api';
import { parseApiError } from '@/lib/error';
import { getTargetLevel } from '@/lib/targetLevels';
import { StatTile } from '@/components/ui/StatTile';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Callout } from '@/components/ui/Callout';
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
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
      });
      setUser(unwrap(res));
      navigate('/app');
    } catch (err: unknown) {
      setError(parseApiError(err));
      setSaving(false);
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center px-4 py-16">
      <div className="pointer-events-none fixed inset-0 -z-10 bg-dots" />

      <div className="w-full max-w-lg space-y-6">
        <div className="flex flex-col items-center gap-3 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary/10">
            <Flame className="h-7 w-7 text-primary" />
          </div>
          <div>
            <h1 className="text-xl font-semibold tracking-tight">Set up your forge</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Two quick steps to personalize your learning engine.
            </p>
          </div>
        </div>

        <div className="space-y-6 rounded-2xl border border-border bg-card p-6">
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
              <span className="text-sm font-semibold text-foreground">{config.label}</span>
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
              <div className="grid grid-cols-4 gap-2">
                <StatTile label="Problems" value={config.targetTotal} tone="primary" />
                <StatTile label="Easy" value={`${config.easyPct}%`} tone="success" />
                <StatTile label="Medium" value={`${config.mediumPct}%`} tone="warning" />
                <StatTile label="Hard" value={`${config.hardPct}%`} tone="danger" />
              </div>
              <p className="mt-3 text-center text-caption text-muted-foreground">{config.companies}</p>
            </div>
          </div>

          {/* Step 2: LeetCode */}
          <div className="border-t border-border pt-5">
            <div>
              <p className="flex items-center gap-1.5 text-sm font-semibold">
                <Code2 className="h-4 w-4 text-primary" />
                2 · Connect LeetCode (optional)
              </p>
              <p className="mt-0.5 text-xs text-muted-foreground">
                Skip for now — you can sync anytime from Profile.
              </p>
            </div>
            <Input
              type="text"
              value={leetcodeUsername}
              onChange={(e) => setLeetcodeUsername(e.target.value)}
              className="mt-3"
              placeholder="your_leetcode_username"
            />
          </div>

          {error && <Callout tone="danger">{error}</Callout>}
        </div>

        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            className="flex-1"
            onClick={() => saveAndContinue(true)}
            disabled={saving}
          >
            Skip for now
          </Button>
          <Button
            className="group flex-[2]"
            onClick={() => saveAndContinue(false)}
            disabled={saving}
            loading={saving}
          >
            {saving ? 'Saving…' : 'Start Forging'}
            {!saving && <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />}
          </Button>
        </div>
      </div>
    </div>
  );
}
