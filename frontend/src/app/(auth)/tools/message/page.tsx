'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';

export default function MessageToolRedirectPage() {
  const router = useRouter();

  useEffect(() => {
    router.replace('/dashboard/message-tools');
  }, [router]);

  return null;
}
