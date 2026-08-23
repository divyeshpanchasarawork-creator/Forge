export function parseApiError(err: unknown): string {
  if (err && typeof err === 'object' && 'response' in err) {
    const axiosErr = err as { response?: { data?: { message?: string } } };
    if (axiosErr.response?.data?.message) {
      return axiosErr.response.data.message;
    }
  }
  if (err && typeof err === 'object' && 'code' in err) {
    const code = (err as { code?: string }).code;
    if (code === 'ECONNABORTED') {
      return 'Request timed out — the backend may be waking up. Try again in a moment.';
    }
    if (code === 'ERR_NETWORK') {
      return 'Network error — could not reach the server.';
    }
  }
  if (err instanceof Error) return err.message;
  return 'An unexpected error occurred';
}
