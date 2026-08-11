import { cn } from '@/lib/utils';

interface SignalChipProps {
  name: string;
  contribution: number;
  value?: number;
  weight?: number;
  className?: string;
}

export function SignalChip({ name, contribution, value, weight, className }: SignalChipProps) {
  const tooltip =
    value != null && weight != null ? `${name}: ${value}/100 × weight ${weight}` : name;
  return (
    <span
      title={tooltip}
      className={cn(
        'inline-flex items-center gap-1 rounded-full bg-secondary px-2 py-[3px] text-micro text-muted-foreground tabular-nums',
        className
      )}
    >
      <span>{name}</span>
      <span>+{contribution}</span>
    </span>
  );
}
