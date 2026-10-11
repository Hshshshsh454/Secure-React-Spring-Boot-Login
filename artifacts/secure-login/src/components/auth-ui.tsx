import type { ReactNode } from 'react';
import { Link } from 'wouter';
import { ArrowRight, Check, CircleAlert, Eye, EyeOff, LockKeyhole, ShieldCheck, Waves } from 'lucide-react';

export function Brand({ mobile = false }: { mobile?: boolean }) {
  return (
    <Link href="/" className={`brand${mobile ? ' mobile-brand' : ''}`} aria-label="Stillwater home">
      <span className="brand-mark" aria-hidden="true"><Waves size={17} strokeWidth={1.5} /></span>
      <span className="brand-name">Stillwater</span>
    </Link>
  );
}

export function WelcomePanel({ register = false }: { register?: boolean }) {
  return (
    <aside className="welcome-panel" aria-label="Stillwater">
      <Brand />
      <div className="welcome-copy">
        <div className="eyebrow">A quieter kind of account</div>
        <h1>{register ? 'A little space, just for you.' : 'Your own space, held safely.'}</h1>
        <p>
          {register
            ? 'Make a private place to return to. Your account belongs to you, and only you.'
            : 'Come back to what matters. Your session stays private, and your account stays yours.'}
        </p>
      </div>
      <div className="panel-note"><ShieldCheck size={16} strokeWidth={1.7} /> Private by design. Yours by default.</div>
    </aside>
  );
}

export function AuthFrame({ children, register = false }: { children: ReactNode; register?: boolean }) {
  return (
    <main className="app-frame">
      <WelcomePanel register={register} />
      <section className="auth-main">
        <div className="auth-content">
          <Brand mobile />
          {children}
        </div>
      </section>
    </main>
  );
}

export function HealthBadge({
  state,
  detail,
  onRetry,
}: {
  state: 'checking' | 'ready' | 'down';
  detail?: string;
  onRetry?: () => void;
}) {
  const copy = state === 'ready' ? `Service ready${detail ? ` · ${detail}` : ''}` : state === 'checking' ? 'Checking service' : 'Service unavailable';
  return (
    <div className={`health-pill ${state}`} role="status" aria-live="polite" data-testid="status-backend-health">
      <span className="health-dot" aria-hidden="true" />
      <span>{copy}</span>
      {state === 'down' && onRetry && <button type="button" className="health-retry" onClick={onRetry} data-testid="button-retry-health">Retry</button>}
    </div>
  );
}

export function CsrfNote({ ready, failed, onRetry }: { ready: boolean; failed: boolean; onRetry: () => void }) {
  return (
    <div className="csrf-note" role="status" aria-live="polite" data-testid="status-browser-security">
      {ready ? <><ShieldCheck size={13} /> Browser session protected</> : failed ? <><CircleAlert size={13} /> Security check unavailable <button className="health-retry" type="button" onClick={onRetry} data-testid="button-retry-security">Retry</button></> : <><LockKeyhole size={13} /> Preparing a secure session…</>}
    </div>
  );
}

export function ServerAlert({ children }: { children: ReactNode }) {
  return <div className="alert" role="alert" data-testid="status-auth-error"><CircleAlert size={15} /> <span>{children}</span></div>;
}

export function PasswordField({
  visible,
  onToggle,
  children,
}: {
  visible: boolean;
  onToggle: () => void;
  children: ReactNode;
}) {
  return (
    <div className="password-wrap">
      {children}
      <button
        type="button"
        className="password-toggle"
        onClick={onToggle}
        aria-label={visible ? 'Hide password' : 'Show password'}
        data-testid="button-toggle-password"
      >
        {visible ? <EyeOff size={17} /> : <Eye size={17} />}
      </button>
    </div>
  );
}

export function SubmitLabel({ busy, children }: { busy: boolean; children: ReactNode }) {
  return <>{busy ? <>Please wait…</> : <>{children}<ArrowRight size={16} /></>}</>;
}

export function InlineSuccess({ children }: { children: ReactNode }) {
  return <div className="success-message" role="status" aria-live="polite" data-testid="status-registration-success"><Check size={17} /><span>{children}</span></div>;
}
