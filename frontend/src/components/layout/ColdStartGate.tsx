import { useState, useEffect, useRef, type ReactNode } from 'react';
import { Logo } from '@/components/brand/Logo';

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

  useEffect(() => {
    if (state === 'ready') return;
    let pollTimer: ReturnType<typeof setTimeout>;
    let timeoutTimer: ReturnType<typeof setTimeout>;
    let elapsedInterval: ReturnType<typeof setInterval>;

    resolvedRef.current = false;

    const stopTimers = () => {
      clearTimeout(pollTimer);
      clearTimeout(timeoutTimer);
      clearInterval(elapsedInterval);
    };

    const fail = () => {
      if (resolvedRef.current) return;
      setAttempt((a) => {
        const next = a + 1;
        if (next >= MAX_RETRIES) {
          setState('timeout');
          stopTimers();
          return next;
        }
        pollTimer = setTimeout(poll, POLL_INTERVAL);
        return next;
      });
    };

    const poll = () => {
      const baseUrl = import.meta.env.VITE_API_URL || '/api';
      const reqController = new AbortController();
      timeoutTimer = setTimeout(() => reqController.abort(), REQUEST_TIMEOUT);

      fetch(`${baseUrl}/health`, {
        signal: reqController.signal,
      })
        .then((res) => {
          if (!res.ok) throw new Error('health check failed');
          resolvedRef.current = true;
          stopTimers();
          try {
            sessionStorage.setItem(HEALTH_CACHE_KEY, String(Date.now()));
          } catch {
            /* ignore */
          }
          setState('ready');
        })
        .catch(() => {
          clearTimeout(timeoutTimer);
          fail();
        });
    };

    elapsedInterval = setInterval(() => {
      setElapsed((e) => e + 1);
    }, 1000);

    poll();

    return () => {
      stopTimers();
    };
  }, [state, cycle]);

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
          <h1 className="text-2xl font-bold tracking-tight">Forge</h1>
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
                className="font-medium text-primary hover:text-primary/80 transition-colors"
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
