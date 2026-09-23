/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        agent: {
          bg: '#0A0E17',
          card: '#111827',
          surface: '#1A2234',
          border: '#273349',
          primary: '#6366F1',
          primaryHover: '#4F46E5',
          accent: '#10B981',
          warning: '#F59E0B',
          danger: '#EF4444'
        }
      },
      animation: {
        'pulse-subtle': 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'ripple': 'ripple 0.6s linear',
        'wave': 'wave 1.2s ease-in-out infinite'
      },
      keyframes: {
        ripple: {
          '0%': { transform: 'scale(0.8)', opacity: '1' },
          '100%': { transform: 'scale(2.4)', opacity: '0' }
        },
        wave: {
          '0%, 100%': { height: '8px' },
          '50%': { height: '28px' }
        }
      }
    },
  },
  plugins: [],
}
