import { z } from 'zod';

export const registerSchema = z
  .object({
    name: z
      .string()
      .min(1, '이름을 입력해주세요')
      .min(2, '이름은 2자 이상이어야 합니다')
      .max(50, '이름은 최대 50자까지 입력 가능합니다'),
    email: z
      .string()
      .min(1, '이메일을 입력해주세요')
      .email('올바른 이메일 형식이 아닙니다'),
    password: z
      .string()
      .min(1, '비밀번호를 입력해주세요')
      .min(8, '비밀번호는 8자 이상이어야 합니다')
      .regex(/[a-zA-Z]/, '영문과 숫자를 모두 포함해야 합니다')
      .regex(/[0-9]/, '영문과 숫자를 모두 포함해야 합니다'),
    confirmPassword: z.string().min(1, '비밀번호 확인을 입력해주세요'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: '비밀번호가 일치하지 않습니다',
    path: ['confirmPassword'],
  });

export type RegisterFormValues = z.infer<typeof registerSchema>;
