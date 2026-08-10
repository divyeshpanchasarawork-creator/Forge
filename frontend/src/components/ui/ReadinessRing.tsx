import { scoreTone, toneVar } from '@/lib/score';
import { cn } from '@/lib/utils';

interface ReadinessRingProps {
  score: number;
  label?: string;
  size?: number;
  className?: string;
}

export default function ReadinessRing({ score, label = 'Ready', size = 96, className }: ReadinessRingProps) {
  const clamped = Math.min(100, Math.max(0, score));
  const r = 34;
  const c = 2 * Math.PI * r;
  const color = toneVar[scoreTone(clamped, { good: 80, fair: 50 })];

  return (
    <div
      className={cn('relative shrink-0', className)}
      style={{ width: size, height: size }}
      role="meter"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(clamped)}
      aria-label={`${label}: ${Math.round(clamped)}%`}
    >
      <svg
        className="-rotate-90"
        viewBox="0 0 80 80"
        style={{ width: size, height: size }}
        aria-hidden="true"
      >
        <circle cx="40" cy="40" r={r} fill="none" stroke="var(--color-secondary)" strokeWidth="7" />
        <circle
          cx="40" cy="40" r={r} fill="none" stroke={color} strokeWidth="7"
          strokeLinecap="round" strokeDasharray={c} strokeDashoffset={c * (1 - clamped / 100)}
          className="transition-all duration-700"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-xl font-bold leading-none tabular-nums">{score}</span>
        <span className="mt-1 text-micro uppercase tracking-wider text-muted-foreground">{label}</span>
      </div>
    </div>
  );
}
