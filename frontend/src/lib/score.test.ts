import { describe, expect, it } from 'vitest';
import { scoreTone, toneText, toneBg, toneFill, toneVar } from './score';

describe('scoreTone', () => {
  it('maps scores at or above the good threshold to success', () => {
    expect(scoreTone(70)).toBe('success');
    expect(scoreTone(100)).toBe('success');
  });

  it('maps scores between fair and good to warning', () => {
    expect(scoreTone(69)).toBe('warning');
    expect(scoreTone(40)).toBe('warning');
  });

  it('maps scores below fair to danger', () => {
    expect(scoreTone(39)).toBe('danger');
    expect(scoreTone(0)).toBe('danger');
  });

  it('honours custom thresholds', () => {
    expect(scoreTone(50, { good: 80, fair: 45 })).toBe('warning');
    expect(scoreTone(85, { good: 80, fair: 45 })).toBe('success');
    expect(scoreTone(10, { good: 80, fair: 45 })).toBe('danger');
  });
});

describe('tone utility maps', () => {
  it('cover every tone with token-based classes', () => {
    for (const tone of ['success', 'warning', 'danger'] as const) {
      expect(toneText[tone]).toMatch(/^text-(success|warning|destructive)$/);
      expect(toneBg[tone]).toMatch(/^bg-(success|warning|destructive)\/10$/);
      expect(toneFill[tone]).toMatch(/^bg-(success|warning|destructive)$/);
      expect(toneVar[tone]).toMatch(/^var\(--color-(success|warning|destructive)\)$/);
    }
  });
});
