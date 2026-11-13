/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html","./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        jarvis: {
          bg: "#0A0E1A",
          surface: "#0F1A2E",
          card: "#162840",
          blue: "#00D4FF",
          accent: "#00FFD1",
          text: "#E8F0FF"
        }
      },
      animation: {
        pulseGlow: 'pulseGlow 2s ease-in-out infinite',
        rotateCore: 'rotateCore 8s linear infinite'
      },
      keyframes: {
        pulseGlow: { '0%,100%': { opacity: '0.8', transform: 'scale(1)' }, '50%': { opacity: '1', transform: 'scale(1.05)' } },
        rotateCore: { '0%': { transform: 'rotate(0deg)' }, '100%': { transform: 'rotate(360deg)' } }
      }
    }
  },
  plugins: []
}
