import { Component, type ReactNode } from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: unknown, info: unknown) {
    console.error('Forge UI error:', error, info);
  }

  private handleReset = () => {
    this.setState({ hasError: false });
  };

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-background px-6 text-center">
        <div className="flex max-w-sm flex-col items-center gap-4">
          <span className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-destructive/10 text-destructive">
            <AlertTriangle className="h-7 w-7" />
          </span>
          <h1 className="text-lg font-bold tracking-tight">Something went wrong</h1>
          <p className="text-sm leading-relaxed text-muted-foreground">
            An unexpected error interrupted this screen. Your data is safe — reload to continue.
          </p>
          <div className="flex items-center gap-2">
            <button
              onClick={this.handleReset}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
            >
              <RefreshCw className="h-4 w-4" />
              Try again
            </button>
            <button
              onClick={() => window.location.reload()}
              className="rounded-lg border border-border bg-secondary px-4 py-2 text-sm font-medium text-foreground transition-colors hover:bg-secondary/70"
            >
              Reload page
            </button>
          </div>
        </div>
      </div>
    );
  }
}
