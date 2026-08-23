import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import KpiCard from './KpiCard';

const icon = <svg data-testid="icon" />;

describe('KpiCard', () => {
  it('renders value and label', () => {
    render(<KpiCard icon={icon} value={42} label="Problems solved" />);
    expect(screen.getByText('42')).toBeInTheDocument();
    expect(screen.getByText('Problems solved')).toBeInTheDocument();
  });

  it('exposes the tooltip through an accessible label only when provided', () => {
    const { rerender } = render(
      <KpiCard icon={icon} value="58%" label="Readiness" tooltip="Composite of mastery, retention and streak" />,
    );
    expect(screen.getByRole('button', { name: 'Readiness: Composite of mastery, retention and streak' }))
      .toBeInTheDocument();

    rerender(<KpiCard icon={icon} value="58%" label="Readiness" />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
