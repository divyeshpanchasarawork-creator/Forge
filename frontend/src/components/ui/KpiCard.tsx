import type { ReactElement } from 'react';

interface KpiCardProps {
  icon: ReactElement;
  value: string | number;
  label: string;
  tooltip?: string;
  trend?: 'up' | 'down' | 'neutral';
  trendValue?: string;
}

export default function KpiCard({ icon, value, label, tooltip, trend, trendValue }: KpiCardProps) {
  return (
    <div className="group relative rounded-xl border border-border bg-card p-5 transition-all hover:border-primary/20 hover:shadow-sm">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-2xl font-bold tracking-tight">{value}</p>
          <p className="mt-1 text-sm text-muted-foreground">{label}</p>
          {trend && trendValue && (
            <p className={`mt-1 text-xs ${trend === 'up' ? 'text-green-400' : trend === 'down' ? 'text-red-400' : 'text-muted-foreground'}`}>
              {trend === 'up' ? '↑' : trend === 'down' ? '↓' : '→'} {trendValue}
            </p>
          )}
        </div>
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
          {icon}
        </div>
      </div>
      {tooltip && (
        <div className="absolute -bottom-2 left-1/2 z-10 hidden w-56 -translate-x-1/2 translate-y-full rounded-lg border border-border bg-card px-3 py-2 text-xs text-muted-foreground shadow-lg group-hover:block">
          {tooltip}
        </div>
      )}
    </div>
  );
}
