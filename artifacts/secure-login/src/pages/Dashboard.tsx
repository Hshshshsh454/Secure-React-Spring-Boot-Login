import { useEffect, useMemo, useState } from 'react';
import { useLocation } from 'wouter';
import { useQueryClient } from '@tanstack/react-query';
import { ArrowUpRight, CalendarDays, CircleAlert, LockKeyhole, ShieldCheck, UserRound } from 'lucide-react';
import { getGetCsrfTokenQueryKey, getGetCurrentUserQueryKey, useGetCsrfToken, useGetCurrentUser, useLogout } from '@workspace/api-client-react';
import { Brand, CsrfNote } from '@/components/auth-ui';
import { getAuthErrorMessage, getErrorStatus } from '@/lib/auth-errors';

function SessionSkeleton() {
  return (
    <div className="loading-shell" role="status" aria-live="polite">
      <div className="loading-card">
        <div className="skeleton short" />
        <div className="skeleton medium" />
        <div className="skeleton" />
        <div className="loading-label">Confirming your private session…</div>
      </div>
    </div>
  );
}

export default function DashboardPage() {
  const [, setLocation] = useLocation();
  const queryClient = useQueryClient();
  const [logoutError, setLogoutError] = useState('');
  const currentUser = useGetCurrentUser({
    query: { queryKey: getGetCurrentUserQueryKey(), retry: false, refetchOnMount: 'always' },
  });
  const csrf = useGetCsrfToken({ query: { queryKey: getGetCsrfTokenQueryKey(), retry: false } });
  const logout = useLogout();

  useEffect(() => {
    if (currentUser.isError && getErrorStatus(currentUser.error) === 401) setLocation('/login');
  }, [currentUser.isError, currentUser.error, setLocation]);

  const createdOn = useMemo(() => {
    if (!currentUser.data?.createdAt) return '—';
    const date = new Date(currentUser.data.createdAt);
    if (Number.isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat(undefined, { month: 'long', day: 'numeric', year: 'numeric' }).format(date);
  }, [currentUser.data?.createdAt]);

  async function signOut() {
    if (!csrf.data || logout.isPending) return;
    setLogoutError('');
    try {
      await logout.mutateAsync();
      queryClient.removeQueries({ queryKey: getGetCurrentUserQueryKey() });
      setLocation('/login');
    } catch (error) {
      if (getErrorStatus(error) === 401) {
        queryClient.removeQueries({ queryKey: getGetCurrentUserQueryKey() });
        setLocation('/login');
        return;
      }
      setLogoutError(getAuthErrorMessage(error, 'logout'));
    }
  }

  if (currentUser.isLoading || (currentUser.isError && getErrorStatus(currentUser.error) === 401)) {
    return <SessionSkeleton />;
  }

  if (currentUser.isError || !currentUser.data) {
    return (
      <main className="loading-shell">
        <section className="fatal-state" aria-labelledby="session-error-title">
          <CircleAlert size={24} color="hsl(var(--destructive))" aria-hidden="true" />
          <h1 id="session-error-title">We couldn’t confirm your session.</h1>
          <p>Your account details haven’t been changed. Try checking your connection and loading the session again.</p>
          <button className="form-submit" type="button" onClick={() => void currentUser.refetch()} data-testid="button-retry-session">Try again <ArrowUpRight size={16} /></button>
          <button className="text-link" type="button" onClick={() => setLocation('/login')} data-testid="button-return-login">Return to sign in</button>
        </section>
      </main>
    );
  }

  return (
    <main className="dashboard-frame">
      <header className="dash-topbar">
        <Brand />
        <button className="logout-button" type="button" onClick={() => void signOut()} disabled={!csrf.data || logout.isPending} data-testid="button-logout">
          {logout.isPending ? 'Signing out…' : 'Sign out'} <ArrowUpRight size={14} />
        </button>
      </header>
      <section className="dash-content" aria-labelledby="dashboard-title">
        <div className="dash-eyebrow">Your private space</div>
        <h1 className="dash-title" id="dashboard-title">You’re signed in.</h1>
        <p className="dash-intro">This browser is securely connected to your account.</p>

        <article className="session-card" aria-labelledby="session-heading">
          <h2 className="session-heading" id="session-heading">
            <span className="session-icon"><ShieldCheck size={17} /></span>
            Account session
            <span className="session-status" data-testid="status-session">Active</span>
          </h2>
          <div className="detail-grid">
            <div className="detail">
              <div className="detail-label">Signed in as</div>
              <div className="detail-value" data-testid="text-account-email"><UserRound size={14} style={{ verticalAlign: '-2px', marginRight: 7 }} />{currentUser.data.email}</div>
            </div>
            <div className="detail">
              <div className="detail-label">Account since</div>
              <div className="detail-value" data-testid="text-account-created"><CalendarDays size={14} style={{ verticalAlign: '-2px', marginRight: 7 }} />{createdOn}</div>
            </div>
          </div>
        </article>

        {logoutError && (
          <div className="alert" role="alert" data-testid="status-logout-error">
            <CircleAlert size={15} /><span>{logoutError}</span>
          </div>
        )}
        <p className="trust-note"><LockKeyhole size={16} /><span>This session uses a same-origin, HttpOnly browser cookie. Your password is never kept in browser storage.</span></p>
        <CsrfNote ready={Boolean(csrf.data)} failed={csrf.isError} onRetry={() => void csrf.refetch()} />
      </section>
    </main>
  );
}
