import { cn } from '@/lib/utils';

interface SkeletonProps {
  className?: string;
}

export function Skeleton({ className }: SkeletonProps) {
  return <div className={cn('animate-pulse rounded-xl bg-secondary', className)} aria-hidden="true" />;
}

export function SkeletonText({ className }: SkeletonProps) {
  return <Skeleton className={cn('h-4', className)} />;
}

export function SkeletonCard({ className }: SkeletonProps) {
  return <Skeleton className={cn('h-32 rounded-2xl border border-border', className)} />;
}

export function SkeletonRow({ className }: SkeletonProps) {
  return <Skeleton className={cn('h-20 rounded-xl border border-border', className)} />;
}

export function SkeletonList({ rows = 5, className }: { rows?: number; className?: string }) {
  return (
    <div className={cn('space-y-3', className)} role="status" aria-label="Loading">
      {Array.from({ length: rows }).map((_, i) => (
        <SkeletonRow key={i} />
      ))}
    </div>
  );
}

export function SkeletonGrid({ cells = 4, className }: { cells?: number; className?: string }) {
  return (
    <div className={cn('grid grid-cols-2 gap-4 md:grid-cols-4', className)} role="status" aria-label="Loading">
      {Array.from({ length: cells }).map((_, i) => (
        <SkeletonCard key={i} />
      ))}
    </div>
  );
}

export function DashboardSkeleton() {
  return (
    <div className="space-y-6" role="status" aria-label="Loading dashboard">
      <Skeleton className="h-40 rounded-2xl border border-border" />
      <SkeletonGrid />
      <SkeletonList rows={3} />
    </div>
  );
}

export function PageSkeleton() {
  return (
    <div className="space-y-6" role="status" aria-label="Loading page">
      <Skeleton className="h-8 w-44 rounded-lg" />
      <Skeleton className="h-36 rounded-2xl border border-border" />
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <SkeletonCard key={i} />
        ))}
      </div>
      <SkeletonList rows={3} />
    </div>
  );
}

export function ChartSkeleton({ className }: { className?: string }) {
  return (
    <div className={cn('space-y-2', className)} role="status" aria-label="Loading chart">
      <SkeletonText className="w-40" />
      <Skeleton className="h-[250px] rounded-xl" />
    </div>
  );
}
