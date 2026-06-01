/* ── WiseCan Tailwind 설정 — _shared/tw-config.js ───────────────────── */
tailwind.config = {
  theme: {
    extend: {
      colors: {
        bone:     '#ffffff',
        cloud:    '#f6f8fa',
        mist:     '#eef1f4',
        line:     '#e4e8ec',
        ink:      '#0a0d12',
        graphite: '#16191f',
        slate2:   '#5b6470',
        stone:    '#8b9099',
        psblue: {
          main:  '#0070D1',
          deep:  '#003791',
          soft:  '#5e96d1',
          wash:  '#e8f1fb'
        },
        psred: {
          main:  '#cf0a2c',
          deep:  '#a40320'
        },
        burgundy: {
          main:  '#7c2532',
          deep:  '#5c1b25'
        },
        navy: {
          main:  '#1a2b5c',
          deep:  '#0f1e3d'
        }
      },
      fontFamily: {
        sans: ['Pretendard', '-apple-system', 'BlinkMacSystemFont', 'Inter', 'sans-serif']
      }
    }
  }
};
