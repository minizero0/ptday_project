import { zodResolver } from '@hookform/resolvers/zod';
import axios from 'axios';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { Button } from '../../../components/Button';
import { Card } from '../../../components/Card';
import { Input } from '../../../components/Input';
import type { ApiResponse } from '../../../types/api';
import { login } from '../api/authApi';
import { useAuthStore } from '../store/authStore';

// FE 검증은 UX 보조. 최종 검증 책임은 백엔드 (CLAUDE.md §6)
const loginSchema = z.object({
  username: z.string().min(1, '아이디를 입력하세요.'),
  password: z.string().min(1, '비밀번호를 입력하세요.'),
});

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const setAuth = useAuthStore((state) => state.setAuth);
  const isSessionExpired = useAuthStore((state) => state.isSessionExpired);
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) });

  const onSubmit = async (values: LoginForm) => {
    setServerError(null);
    try {
      const response = await login(values);
      setAuth(response);
    } catch (error) {
      const message =
        axios.isAxiosError<ApiResponse<never>>(error) && error.response?.data.error
          ? error.response.data.error.message
          : '로그인 중 오류가 발생했습니다.';
      setServerError(message);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-sm">
        <div className="mb-6 text-center">
          <h1 className="text-xl font-bold">ptday 관리자</h1>
          <p className="mt-1 text-sm text-text-muted">로그인하여 시작하세요</p>
        </div>

        {/* 작업 도중 갑자기 이 화면으로 오면 당황하므로 이유를 알려준다. 직접 로그아웃한 경우에는 보이지 않는다 */}
        {isSessionExpired && (
          <p role="status" className="mb-4 rounded-md bg-background px-3 py-2 text-sm text-text-muted">
            로그인이 만료되었습니다. 다시 로그인해 주세요.
          </p>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
          <Input
            label="아이디"
            placeholder="admin"
            autoComplete="username"
            error={errors.username?.message}
            {...register('username')}
          />
          <Input
            label="비밀번호"
            type="password"
            placeholder="••••••••"
            autoComplete="current-password"
            error={errors.password?.message}
            {...register('password')}
          />

          {serverError && (
            <p className="rounded-md bg-danger/10 px-3 py-2 text-sm text-danger">{serverError}</p>
          )}

          <Button type="submit" className="w-full" isLoading={isSubmitting}>
            로그인
          </Button>
        </form>
      </Card>
    </div>
  );
}
