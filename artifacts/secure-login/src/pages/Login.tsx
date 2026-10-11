import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useLocation, Link } from 'wouter';
import { useQueryClient } from '@tanstack/react-query';
import { getGetCsrfTokenQueryKey, getGetCurrentUserQueryKey, getHealthCheckQueryKey, useGetCsrfToken, useGetCurrentUser, useHealthCheck, useLogin } from '@workspace/api-client-react';
import type { LoginInput } from '@workspace/api-client-react';
import { AuthFrame, CsrfNote, HealthBadge, PasswordField, ServerAlert, SubmitLabel } from '@/components/auth-ui';
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form';
import { getAuthErrorMessage } from '@/lib/auth-errors';

const loginSchema = z.object({
  email: z.string().trim().email('Enter a valid email address.').max(254, 'Email must be 254 characters or fewer.'),
  password: z.string().min(1, 'Enter your password.').max(72, 'Password must be 72 characters or fewer.'),
});
type LoginFormValues = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const [, setLocation] = useLocation();
  const queryClient = useQueryClient();
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [serverError, setServerError] = useState('');
  const csrf = useGetCsrfToken({
    query: { queryKey: getGetCsrfTokenQueryKey(), retry: false },
  });
  const health = useHealthCheck({
    query: { queryKey: getHealthCheckQueryKey(), retry: false, refetchInterval: 30_000 },
  });
  const currentUser = useGetCurrentUser({
    query: { queryKey: getGetCurrentUserQueryKey(), retry: false },
  });
  const login = useLogin();
  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  });

  async function submit(values: LoginFormValues) {
    if (!csrf.data || login.isPending) return;
    setServerError('');
    try {
      const profile = await login.mutateAsync({ data: values as LoginInput });
      queryClient.setQueryData(getGetCurrentUserQueryKey(), profile);
      setLocation('/dashboard');
    } catch (error) {
      setServerError(getAuthErrorMessage(error, 'login'));
    }
  }

  useEffect(() => {
    if (currentUser.isSuccess) setLocation('/dashboard');
  }, [currentUser.isSuccess, setLocation]);

  const csrfReady = Boolean(csrf.data);
  const healthState = health.isLoading ? 'checking' : health.isSuccess ? 'ready' : 'down';

  return (
    <AuthFrame>
      <HealthBadge state={healthState} detail={health.data?.status} onRetry={() => void health.refetch()} />
      <div className="form-kicker">Welcome back</div>
      <h1 className="form-title">Sign in</h1>
      <p className="form-description">Step back into your private space.</p>

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
                <FormLabel className="field-label">Password</FormLabel>
                <PasswordField visible={passwordVisible} onToggle={() => setPasswordVisible((value) => !value)}>
                  <FormControl>
                    <input {...field} type={passwordVisible ? 'text' : 'password'} autoComplete="current-password" placeholder="Your password" className="field-input" data-testid="input-password" />
                  </FormControl>
                </PasswordField>
                <FormMessage className="field-error" />
              </FormItem>
            )}
          />
          <button className="form-submit" type="submit" disabled={!csrfReady || login.isPending} data-testid="button-login">
            <SubmitLabel busy={login.isPending}>Continue securely</SubmitLabel>
          </button>
        </form>
      </Form>

      <p className="form-switch">New to Stillwater? <Link href="/register" className="text-link" data-testid="link-register">Create an account</Link></p>
      <div className="security-caption"><span aria-hidden="true">◈</span><span>Your sign-in is protected by a private browser session. We never store your password in this browser.</span></div>
      <CsrfNote ready={csrfReady} failed={csrf.isError} onRetry={() => void csrf.refetch()} />
    </AuthFrame>
  );
}
