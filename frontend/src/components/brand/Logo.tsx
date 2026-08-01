import { Flame } from 'lucide-react';
import { cn } from '@/lib/utils';

type LogoSize = 'sm' | 'md' | 'lg';
type LogoVariant = 'gradient' | 'soft' | 'solid' | 'flame';

interface LogoProps {
  size?: LogoSize;
  variant?: LogoVariant;
  withText?: boolean;
  textClassName?: string;
  className?: string;
}

const sizeStyles: Record<LogoSize, { tile: string; icon: string; text: string }> = {
  sm: { tile: 'h-7 w-7 rounded-lg', icon: 'h-4 w-4', text: 'text-sm' },
  md: { tile: 'h-8 w-8 rounded-lg', icon: 'h-5 w-5', text: 'text-base' },
  lg: { tile: 'h-11 w-11 rounded-xl', icon: 'h-6 w-6', text: 'text-xl' },
};

const variantStyles: Record<'gradient' | 'soft' | 'solid', string> = {
  gradient: 'bg-gradient-to-br from-primary via-purple-500 to-blue-500 shadow-glow',
  soft: 'bg-primary/10',
  solid: 'bg-white/15 backdrop-blur-sm',
};

export function Logo({
  size = 'md',
  variant = 'gradient',
  withText = false,
  textClassName,
  className,
}: LogoProps) {
  const s = sizeStyles[size];
  const onGradient = variant === 'gradient' || variant === 'solid';

  if (variant === 'flame') {
    return (
      <span className={cn('inline-flex shrink-0 items-center gap-2', className)}>
        <Flame className={cn(s.icon, 'text-primary')} />
        {withText && (
          <span className={cn('font-bold tracking-tight', s.text, textClassName)}>Forge</span>
        )}
      </span>
    );
  }

  return (
    <span className={cn('inline-flex shrink-0 items-center gap-2.5', className)}>
      <span className={cn('flex items-center justify-center', s.tile, variantStyles[variant])}>
        <Flame className={cn(s.icon, onGradient ? 'text-white' : 'text-primary')} />
      </span>
      {withText && (
        <span className={cn('font-bold tracking-tight', s.text, onGradient && 'text-white', textClassName)}>
          Forge
        </span>
      )}
    </span>
  );
}
