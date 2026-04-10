import { LoginForm } from '@/features/auth';
import { AuthBrandPanel } from '@/widgets/auth-brand-panel';

export const metadata = {
  title: '로그인 | Wisecan',
};

export default function LoginPage() {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <AuthBrandPanel variant="login" />
      <div className="flex items-center justify-center bg-white px-6 py-12 lg:px-12">
        <LoginForm />
      </div>
    </div>
  );
}
