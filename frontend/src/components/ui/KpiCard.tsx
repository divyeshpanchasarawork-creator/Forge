import type { ReactElement } from 'react';
import { Info } from 'lucide-react';
import { Card } from '@/components/ui/Card';

interface KpiCardProps {
  icon: ReactElement;
  value: string | number;
  label: string;
  tooltip?: string;
}

export default function KpiCard({ icon, value, label, tooltip }: KpiCardProps) {
  return (
    <Card className="group relative p-5 transition-all hover:border-primary/20 hover:z-20 active:scale-[0.98]">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-2xl font-bold tracking-tight">{value}</p>
          <p className="mt-1 text-sm text-muted-foreground">{label}</p>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
            {icon}
          </div>
          {tooltip && (
            <button
              type="button"
              aria-label={`${label}: ${tooltip}`}
              className="rounded p-0.5 text-muted-foreground/60 transition-colors hover:text-muted-foreground"
            >
              <Info className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      </div>
      {tooltip && (
        <div className="pointer-events-none absolute -bottom-2 left-1/2 z-50 hidden w-56 -translate-x-1/2 translate-y-full rounded-lg border border-border bg-card px-3 py-2 text-xs leading-relaxed text-muted-foreground group-hover:block group-focus-within:block">
          {tooltip}
        </div>
      )}
    </Card>
  );
}
