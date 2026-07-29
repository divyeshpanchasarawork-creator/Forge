interface LoadingSkeletonProps {
  count?: number;
  height?: string;
  className?: string;
}

export default function LoadingSkeleton({ count = 1, height = 'h-24', className = '' }: LoadingSkeletonProps) {
  return (
    <div className={`space-y-4 ${className}`}>
      {[...Array(count)].map((_, i) => (
        <div key={i} className={`animate-pulse rounded-2xl bg-secondary ${height}`} />
      ))}
    </div>
  );
}
