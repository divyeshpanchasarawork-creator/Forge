import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

interface CalloutProps {
  children: ReactNode;
  icon?: ReactNode;
  title?: string;
  tone?: 'default' | 'primary' | 'success' | 'warning' | 'danger';
  className?: string;
}

const toneStyles: Record<NonNullable<CalloutProps['tone']>, string> = {
  default: 'border-border bg-secondary/40 text-foreground',
  primary: 'border-primary/20 bg-primary/5 text-foreground',
  success: 'border-success/20 bg-success/5 text-foreground',
  warning: 'border-warning/20 bg-warning/5 text-foreground',
  danger: 'border-destructive/20 bg-destructive/5 text-foreground',
};

export function Callout({ children, icon, title, tone = 'default', className }: CalloutProps) {
  return (
    <div className={cn('flex items-start gap-3 rounded-xl border p-4 text-sm leading-relaxed', toneStyles[tone], className)}>
      {icon && <span className="mt-0.5 shrink-0 text-muted-foreground">{icon}</span>}
      <div className="min-w-0 flex-1">
        {title && <p className="mb-0.5 font-semibold text-foreground">{title}</p>}
        {children}
      </div>
    </div>
  );
}
