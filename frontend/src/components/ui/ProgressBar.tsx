import type { ScoreTone } from '@/lib/score';
import { toneFill } from '@/lib/score';
import { cn } from '@/lib/utils';

interface ProgressBarProps {
  value: number;
  tone?: ScoreTone | 'primary';
  className?: string;
  ariaLabel?: string;
}

const fills: Record<ScoreTone | 'primary', string> = {
  primary: 'bg-primary',
  ...toneFill,
};

export function ProgressBar({ value, tone = 'primary', className, ariaLabel }: ProgressBarProps) {
  const pct = Math.min(100, Math.max(0, value));
  return (
    <div
      className={cn('h-2 overflow-hidden rounded-full bg-secondary', className)}
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(pct)}
      aria-label={ariaLabel}
    >
      <div
        className={cn('h-full rounded-full transition-all duration-500', fills[tone])}
        style={{ width: `${pct}%` }}
      />
    </div>
  );
}
