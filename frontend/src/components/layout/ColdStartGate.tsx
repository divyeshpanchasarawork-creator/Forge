import { useState, useEffect, useRef, useCallback, type ReactNode } from 'react';
import { Logo } from '@/components/brand/Logo';
import { buttonVariants } from '@/components/ui/Button';

const POLL_INTERVAL = 4000;
const REQUEST_TIMEOUT = 10000;
const MAX_RETRIES = 30;
const HEALTH_CACHE_KEY = 'forge_health_ok';
const HEALTH_CACHE_TTL = 5 * 60 * 1000;

function hasRecentHealth(): boolean {
  try {
    const stored = sessionStorage.getItem(HEALTH_CACHE_KEY);
    if (!stored) return false;
    const t = Number(stored);
    return Number.isFinite(t) && Date.now() - t < HEALTH_CACHE_TTL;
  } catch {
    return false;
  }
}

export default function ColdStartGate({ children }: { children: ReactNode }) {
  const [state, setState] = useState<'loading' | 'ready' | 'timeout'>(() =>
    hasRecentHealth() ? 'ready' : 'loading'
  );
  const [elapsed, setElapsed] = useState(0);
  const [attempt, setAttempt] = useState(0);
  const [cycle, setCycle] = useState(0);
  const resolvedRef = useRef(false);
  const reqControllerRef = useRef<AbortController | null>(null);

  const poll = useCallback(() => {
    if (resolvedRef.current) return;
    const baseUrl = import.meta.env.VITE_API_URL || '/api';
    const reqController = new AbortController();
    reqControllerRef.current = reqController;
    const timeoutTimer = setTimeout(() => reqController.abort(), REQUEST_TIMEOUT);

    fetch(`${baseUrl}/health`, {
      signal: reqController.signal,
    })
      .then((res) => {
        if (!res.ok) throw new Error('health check failed');
        if (resolvedRef.current) return;
        resolvedRef.current = true;
        clearTimeout(timeoutTimer);
        try {
          sessionStorage.setItem(HEALTH_CACHE_KEY, String(Date.now()));
        } catch {
          /* ignore */
        }
        setState('ready');
      })
      .catch(() => {
        clearTimeout(timeoutTimer);
        if (resolvedRef.current || reqController.signal.aborted) return;
        setAttempt((a) => a + 1);
      });
  }, []);

  useEffect(() => {
    if (state !== 'loading') return;
    resolvedRef.current = false;

    const elapsedInterval = setInterval(() => {
      setElapsed((e) => e + 1);
    }, 1000);

    poll();

    return () => {
      clearInterval(elapsedInterval);
      reqControllerRef.current?.abort();
    };
  }, [state, cycle, poll]);

  useEffect(() => {
    if (state !== 'loading' || resolvedRef.current || attempt === 0) return;
    if (attempt >= MAX_RETRIES) {
      setState('timeout');
      return;
    }
    const pollTimer = setTimeout(poll, POLL_INTERVAL);
    return () => clearTimeout(pollTimer);
  }, [attempt, state, cycle, poll]);

  if (state === 'ready') return <>{children}</>;

  const minutes = Math.floor(elapsed / 60);
  const seconds = elapsed % 60;
  const timeLabel = minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`;

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background">
      <div className="flex flex-col items-center gap-6">
        <div className="relative">
          <div className="absolute inset-0 animate-ping rounded-full bg-primary/20" style={{ animationDuration: '3s' }} />
          <Logo size="lg" className="animate-pulse" />
        </div>
        <div className="text-center">
          <h1 className="text-xl font-bold tracking-tight">Forge</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {state === 'timeout' ? 'Taking longer than usual...' : 'Waking up the forge...'}
          </p>
        </div>
        <div className="h-1.5 w-48 overflow-hidden rounded-full bg-secondary">
          <div
            className="h-full rounded-full bg-primary transition-all"
            style={{
              width: state === 'timeout' ? '95%' : `${Math.min(95, (attempt / MAX_RETRIES) * 100)}%`,
              animation: state !== 'timeout' ? 'pulse 2s ease-in-out infinite' : 'none',
            }}
          />
        </div>
        <p className="text-xs text-muted-foreground/60">
          {state === 'timeout' ? (
            <span className="flex items-center gap-2">
              The server is still starting.
              <button
                onClick={() => {
                  setState('loading');
                  setAttempt(0);
                  setElapsed(0);
                  setCycle((c) => c + 1);
                }}
                className={buttonVariants({ variant: 'ghost', size: 'sm' })}
              >
                Retry
              </button>
            </span>
          ) : (
            `Warming up for ${timeLabel}...`
          )}
        </p>
      </div>
    </div>
  );
}
