import { Card, CardContent } from '@/components/ui/Card';
import type { ReactNode } from 'react';

interface TeachingEmptyStateProps {
  icon: ReactNode;
  title: string;
  description: string;
  steps: string[];
  action?: ReactNode;
  className?: string;
}

export default function TeachingEmptyState({ icon, title, description, steps, action, className }: TeachingEmptyStateProps) {
  return (
    <Card className={className}>
      <CardContent className="px-6 py-10">
        <div className="mx-auto max-w-md text-center">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10">
            {icon}
          </div>
          <h3 className="text-lg font-semibold">{title}</h3>
          <p className="mt-1.5 text-sm leading-relaxed text-muted-foreground">{description}</p>
          <ol className="mx-auto mt-5 max-w-sm space-y-2 text-left">
            {steps.map((step, i) => (
              <li key={i} className="flex items-start gap-2.5 text-sm">
                <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary/10 text-[10px] font-bold text-primary">
                  {i + 1}
                </span>
                <span className="text-muted-foreground">{step}</span>
              </li>
            ))}
          </ol>
          {action && <div className="mt-6">{action}</div>}
        </div>
      </CardContent>
    </Card>
  );
}
