import { describe, expect, it } from 'vitest';
import { parseApiError } from './error';

describe('parseApiError', () => {
  it('extracts the backend message from an axios error', () => {
    const err = { response: { data: { message: 'Outcome must be one of ...' } } };
    expect(parseApiError(err)).toBe('Outcome must be one of ...');
  });

  it('falls back when the response carries no message', () => {
    const err = { response: { data: {} } };
    expect(parseApiError(err)).toBe('An unexpected error occurred');
  });

  it('returns the message of a plain Error', () => {
    expect(parseApiError(new Error('network down'))).toBe('network down');
  });

  it('handles non-error values', () => {
    expect(parseApiError(undefined)).toBe('An unexpected error occurred');
    expect(parseApiError('weird')).toBe('An unexpected error occurred');
    expect(parseApiError(null)).toBe('An unexpected error occurred');
  });
});
