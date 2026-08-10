import type { ReactNode } from 'react';
import type { ScoreTone } from '@/lib/score';
import { toneText } from '@/lib/score';
import { cn } from '@/lib/utils';

interface StatTileProps {
  label: string;
  value: ReactNode;
  hint?: string;
  tone?: 'default' | 'primary' | ScoreTone;
  className?: string;
}

const valueStyles: Record<NonNullable<StatTileProps['tone']>, string> = {
  default: 'text-foreground',
  primary: 'text-primary',
  success: toneText.success,
  warning: toneText.warning,
  danger: toneText.danger,
};

export function StatTile({ label, value, hint, tone = 'default', className }: StatTileProps) {
  return (
    <div className={cn('rounded-xl bg-secondary/50 p-3 text-center', className)}>
      <p className={cn('text-lg font-semibold tracking-tight tabular-nums', valueStyles[tone])}>{value}</p>
      <p className="mt-0.5 text-caption text-muted-foreground">{label}</p>
      {hint && <p className="mt-0.5 text-micro text-subtle-foreground">{hint}</p>}
    </div>
  );
}
