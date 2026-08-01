import type { ReactElement } from 'react';

interface KpiCardProps {
  icon: ReactElement;
  value: string | number;
  label: string;
  tooltip?: string;
}

export default function KpiCard({ icon, value, label, tooltip }: KpiCardProps) {
  return (
    <div className="group relative rounded-xl border border-border bg-card p-5 shadow-soft transition-all hover:border-primary/20 active:scale-[0.98]">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-2xl font-bold tracking-tight">{value}</p>
          <p className="mt-1 text-sm text-muted-foreground">{label}</p>
        </div>
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
          {icon}
        </div>
      </div>
      {tooltip && (
        <div className="absolute -bottom-2 left-1/2 z-10 hidden w-56 -translate-x-1/2 translate-y-full rounded-lg border border-border bg-card px-3 py-2 text-xs text-muted-foreground shadow-2xl group-hover:block">
          {tooltip}
        </div>
      )}
    </div>
  );
}
