import type { ReactNode, SelectHTMLAttributes } from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils';

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  variant?: 'sm' | 'md';
  className?: string;
  children?: ReactNode;
}

const variantStyles = {
  sm: 'h-8 py-1 pl-2.5 pr-7 text-xs',
  md: 'h-10 py-2 pl-3.5 pr-9 text-sm',
};

const chevronStyles = {
  sm: 'right-2 h-3.5 w-3.5',
  md: 'right-3 h-4 w-4',
};

export function Select({ variant = 'md', className, children, ...props }: SelectProps) {
  return (
    <div className={cn('relative w-full', className)}>
      <select
        className={cn(
          'w-full cursor-pointer appearance-none rounded-lg border border-input bg-secondary/50 text-foreground transition-all focus:border-primary focus:bg-secondary focus:outline-none focus:ring-1 focus:ring-primary',
          variantStyles[variant]
        )}
        {...props}
      >
        {children}
      </select>
      <ChevronDown
        className={cn('pointer-events-none absolute top-1/2 -translate-y-1/2 text-muted-foreground', chevronStyles[variant])}
      />
    </div>
  );
}
