interface LoadingStateProps {
  loading: boolean;
  error: string | null;
  children: React.ReactNode;
}

export default function LoadingState({
  loading,
  error,
  children,
}: LoadingStateProps) {
  if (loading) {
    return <p>Loading…</p>;
  }

  if (error) {
    return <p className="text-red-600">{error}</p>;
  }

  return <>{children}</>;
}
