import { useState, useEffect, type ReactNode } from 'react';
import { Flame } from 'lucide-react';

const POLL_INTERVAL = 4000;
const MAX_RETRIES = 15;

export default function ColdStartGate({ children }: { children: ReactNode }) {
  const [state, setState] = useState<'loading' | 'ready' | 'timeout'>('loading');
  const [retries, setRetries] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout>;

    const poll = () => {
      const baseUrl = import.meta.env.VITE_API_URL || '';
      fetch(`${baseUrl}/api/auth/profile`, {
        credentials: 'include',
        signal: controller.signal,
      })
        .then((res) => {
          if (res.ok || res.status === 401) {
            setState('ready');
          }
        })
        .catch(() => {
          setRetries((r) => {
            const next = r + 1;
            if (next >= MAX_RETRIES) {
              setState('timeout');
              return next;
            }
            timer = setTimeout(poll, POLL_INTERVAL);
            return next;
          });
        });
    };

    poll();
    return () => {
      controller.abort();
      clearTimeout(timer);
    };
  }, []);

  if (state === 'ready') return <>{children}</>;

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-background">
      <div className="flex flex-col items-center gap-6">
        <div className="relative">
          <Flame className="h-16 w-16 animate-pulse text-primary" />
          <div className="absolute inset-0 animate-ping rounded-full bg-primary/20" style={{ animationDuration: '3s' }} />
        </div>
        <div className="text-center">
          <h1 className="text-2xl font-bold tracking-tight">Forge</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {state === 'timeout' ? 'Still waking up...' : 'Waking up the forge...'}
          </p>
        </div>
        <div className="h-1.5 w-48 overflow-hidden rounded-full bg-secondary">
          <div
            className="h-full rounded-full bg-primary transition-all"
            style={{
              width: state === 'timeout' ? '95%' : `${(retries / MAX_RETRIES) * 100}%`,
              animation: state !== 'timeout' ? 'pulse 2s ease-in-out infinite' : 'none',
            }}
          />
        </div>
        <p className="text-xs text-muted-foreground/60">
          {state === 'timeout'
            ? 'Taking longer than usual. Try refreshing.'
            : `Retrying... (${retries}/${MAX_RETRIES})`}
        </p>
      </div>
    </div>
  );
}
