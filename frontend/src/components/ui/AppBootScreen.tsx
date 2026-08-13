import { useEffect, useState } from 'react';
import { Logo } from '@/components/brand/Logo';
import { Button } from '@/components/ui/Button';

const RETRY_THRESHOLD = 25;

export default function AppBootScreen() {
  const [elapsed, setElapsed] = useState(0);

  useEffect(() => {
    const t = setInterval(() => setElapsed((e) => e + 1), 1000);
    return () => clearInterval(t);
  }, []);

  const stuck = elapsed >= RETRY_THRESHOLD;

  return (
    <div
      className="flex min-h-screen flex-col items-center justify-center gap-6 bg-background px-6"
      role="status"
      aria-live="polite"
    >
      <div className="flex flex-col items-center gap-5">
        <Logo size="lg" className="animate-pulse" />
        <div className="text-center">
          <h1 className="text-lg font-semibold tracking-tight">Preparing your workspace</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {stuck ? 'The forge is taking a moment to warm up.' : 'Loading your dashboard…'}
          </p>
        </div>
        <div className="h-1 w-48 overflow-hidden rounded-full bg-secondary">
          <div
            className="h-full rounded-full bg-primary transition-all"
            style={{
              width: `${Math.min(100, Math.round((elapsed / (RETRY_THRESHOLD * 2)) * 100))}%`,
              animation: !stuck ? 'pulse 2s ease-in-out infinite' : 'none',
            }}
          />
        </div>
        <p className="text-xs text-subtle-foreground" aria-hidden="true">
          {elapsed}s
        </p>
        {stuck && (
          <Button variant="outline" type="button" onClick={() => window.location.reload()}>
            Retry
          </Button>
        )}
      </div>
    </div>
  );
}
