export type ScoreTone = 'success' | 'warning' | 'danger';

export function scoreTone(score: number, { good = 70, fair = 40 }: { good?: number; fair?: number } = {}): ScoreTone {
  if (score >= good) return 'success';
  if (score >= fair) return 'warning';
  return 'danger';
}

export const toneText: Record<ScoreTone, string> = {
  success: 'text-success',
  warning: 'text-warning',
  danger: 'text-destructive',
};

export const toneBg: Record<ScoreTone, string> = {
  success: 'bg-success/10',
  warning: 'bg-warning/10',
  danger: 'bg-destructive/10',
};

export const toneFill: Record<ScoreTone, string> = {
  success: 'bg-success',
  warning: 'bg-warning',
  danger: 'bg-destructive',
};

export const toneVar: Record<ScoreTone, string> = {
  success: 'var(--color-success)',
  warning: 'var(--color-warning)',
  danger: 'var(--color-destructive)',
};
