import ky from 'ky';

export const api = ky.create({
  prefix: process.env.NEXT_PUBLIC_API_URL || '/api',
  timeout: 10000,
  hooks: {
    beforeRequest: [
      ({ request }) => {
        const token = typeof window !== 'undefined'
          ? localStorage.getItem('token')
          : null;
        if (token) {
          request.headers.set('Authorization', `Bearer ${token}`);
        }
      },
    ],
  },
});
