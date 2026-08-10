import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
  dashed?: boolean;
  className?: string;
}

export function EmptyState({ icon, title, description, action, dashed = true, className }: EmptyStateProps) {
  return (
    <div
      className={cn(
        'rounded-xl px-6 py-10 text-center',
        dashed ? 'border border-dashed border-border' : '',
        className
      )}
    >
      {icon && <div className="mx-auto mb-3 flex h-11 w-11 items-center justify-center rounded-xl bg-secondary text-muted-foreground">{icon}</div>}
      <p className="text-sm font-medium text-foreground">{title}</p>
      {description && <p className="mx-auto mt-1 max-w-sm text-sm leading-relaxed text-muted-foreground">{description}</p>}
      {action && <div className="mt-4 flex justify-center">{action}</div>}
    </div>
  );
}
