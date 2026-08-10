import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

interface HeroCardProps {
  children: ReactNode;
  className?: string;
}

export function HeroCard({ children, className }: HeroCardProps) {
  return (
    <section
      className={cn(
        'relative overflow-hidden rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/10 via-card to-card',
        className
      )}
    >
      <div className="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-primary/10 blur-3xl" />
      <div className="relative">{children}</div>
    </section>
  );
}
