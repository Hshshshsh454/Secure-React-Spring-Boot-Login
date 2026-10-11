import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useLocation } from 'wouter';
import { useGetCsrfToken, getGetCsrfTokenQueryKey, getHealthCheckQueryKey, useHealthCheck, useRegisterAccount } from '@workspace/api-client-react';
import type { RegisterInput } from '@workspace/api-client-react';
import { AuthFrame, CsrfNote, HealthBadge, InlineSuccess, PasswordField, ServerAlert, SubmitLabel } from '@/components/auth-ui';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { getAuthErrorMessage } from '@/lib/auth-errors';

const registerSchema = z.object({
  email: z.string().trim().email('Enter a valid email address.').max(254, 'Email must be 254 characters or fewer.'),
  password: z.string().min(12, 'Use at least 12 characters.').max(72, 'Password must be 72 characters or fewer.'),
});
type RegisterFormValues = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const [, setLocation] = useLocation();
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [serverError, setServerError] = useState('');
  const [acknowledgement, setAcknowledgement] = useState('');
  const csrf = useGetCsrfToken({ query: { queryKey: getGetCsrfTokenQueryKey(), retry: false } });
  const health = useHealthCheck({ query: { queryKey: getHealthCheckQueryKey(), retry: false, refetchInterval: 30_000 } });
  const register = useRegisterAccount();
  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { email: '', password: '' },
  });

  async function submit(values: RegisterFormValues) {
    if (!csrf.data || register.isPending || acknowledgement) return;
    setServerError('');
    try {
      const result = await register.mutateAsync({ data: values as RegisterInput });
      setAcknowledgement(result.message);
      window.setTimeout(() => setLocation('/login'), 1800);
    } catch (error) {
      setServerError(getAuthErrorMessage(error, 'register'));
    }
  }

  const csrfReady = Boolean(csrf.data);
  const healthState = health.isLoading ? 'checking' : health.isSuccess ? 'ready' : 'down';
  return (
    <AuthFrame register>
      <HealthBadge state={healthState} detail={health.data?.status} onRetry={() => void health.refetch()} />
      <div className="form-kicker">Start with a little space</div>
      <h1 className="form-title">Create your account</h1>
      <p className="form-description">A private place starts with an email and a password only you know.</p>

      {acknowledgement ? (
        <InlineSuccess>{acknowledgement}</InlineSuccess>
      ) : (
        <Form {...form}>
          <form className="auth-form" onSubmit={form.handleSubmit(submit)} noValidate>
            {serverError && <ServerAlert>{serverError}</ServerAlert>}
            <FormField
              control={form.control}
              name="email"
              render={({ field }) => (
                <FormItem className="field-block">
                  <FormLabel className="field-label">Email address</FormLabel>
                  <FormControl>
                    <input {...field} type="email" autoComplete="email" inputMode="email" placeholder="you@example.com" className="field-input" data-testid="input-email" />
                  </FormControl>
                  <FormMessage className="field-error" />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="password"
              render={({ field }) => (
                <FormItem className="field-block">
                  <FormLabel className="field-label">Create a password</FormLabel>
                  <PasswordField visible={passwordVisible} onToggle={() => setPasswordVisible((value) => !value)}>
                    <FormControl>
                      <input {...field} type={passwordVisible ? 'text' : 'password'} autoComplete="new-password" placeholder="At least 12 characters" className="field-input" data-testid="input-password" />
                    </FormControl>
                  </PasswordField>
                  <FormMessage className="field-error" />
                </FormItem>
              )}
            />
            <button className="form-submit" type="submit" disabled={!csrfReady || register.isPending} data-testid="button-register">
              <SubmitLabel busy={register.isPending}>Create account</SubmitLabel>
            </button>
          </form>
        </Form>
      )}

      <p className="form-switch">Already have a space? <Link href="/login" className="text-link" data-testid="link-login">Sign in</Link></p>
      <div className="security-caption"><span aria-hidden="true">◈</span><span>For your privacy, we give the same confirmation whether or not this email can be registered.</span></div>
      <CsrfNote ready={csrfReady} failed={csrf.isError} onRetry={() => void csrf.refetch()} />
    </AuthFrame>
  );
}
