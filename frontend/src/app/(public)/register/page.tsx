import { RegisterForm } from '@/features/auth';
import { AuthBrandPanel } from '@/widgets/auth-brand-panel';

export const metadata = {
  title: '회원가입 | Wisecan',
};

export default function RegisterPage() {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <AuthBrandPanel variant="register" />
      <div className="flex items-center justify-center bg-white px-6 py-12 lg:px-12">
        <RegisterForm />
      </div>
    </div>
  );
}
