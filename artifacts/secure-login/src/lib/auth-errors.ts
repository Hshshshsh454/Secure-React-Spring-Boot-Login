export function getAuthErrorMessage(error: unknown, action: 'login' | 'register' | 'logout'): string {
  const status = typeof error === 'object' && error !== null && 'status' in error
    ? Number((error as { status?: unknown }).status)
    : undefined;

  if (status === 401 && action === 'login') {
    return 'We couldn’t sign you in with those details. Check your email and password, then try again.';
  }
  if (status === 401 && action === 'logout') {
    return 'Your session has already ended. You can safely sign in again.';
  }
  if (status === 429) return 'There have been too many attempts. Please wait a moment before trying again.';
  if (status !== undefined && status >= 500) {
    return 'The service is having a moment. Your details are safe; please try again shortly.';
  }
  if (status === undefined) {
    return 'We couldn’t reach the service. Check your connection and try again.';
  }
  return action === 'register'
    ? 'We couldn’t complete that request right now. Please try again.'
    : 'That request couldn’t be completed. Please try again.';
}

export function getErrorStatus(error: unknown): number | undefined {
  if (typeof error !== 'object' || error === null || !('status' in error)) return undefined;
  const status = Number((error as { status?: unknown }).status);
  return Number.isFinite(status) ? status : undefined;
}
