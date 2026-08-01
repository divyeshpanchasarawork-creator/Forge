import { Moon, Sun } from 'lucide-react';
import { useTheme } from '@/contexts/ThemeContext';
import { cn } from '@/lib/utils';

interface ThemeToggleProps {
  className?: string;
  size?: 'sm' | 'md';
}

const sizeStyles = {
  sm: 'h-8 w-8',
  md: 'h-9 w-9',
};

export default function ThemeToggle({ className, size = 'sm' }: ThemeToggleProps) {
  const { theme, toggle } = useTheme();
  const dark = theme === 'dark';

  return (
    <button
      type="button"
      onClick={toggle}
      className={cn(
        'flex items-center justify-center rounded-lg text-muted-foreground transition-all hover:bg-secondary hover:text-foreground active:scale-95',
        sizeStyles[size],
        className
      )}
      aria-label={dark ? 'Switch to light mode' : 'Switch to dark mode'}
      title={dark ? 'Light mode' : 'Dark mode'}
    >
      {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
    </button>
  );
}
