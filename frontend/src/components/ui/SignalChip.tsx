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
        'rounded-full bg-secondary px-2 py-0.5 text-tag font-medium text-muted-foreground',
        className
      )}
    >
      {name} <span className="text-primary">+{contribution}</span>
    </span>
  );
}
