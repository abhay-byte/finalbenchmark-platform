import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './app/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      fontFamily: {
        roboto: ['Roboto', 'sans-serif'],
      },
      colors: {
        mascot: {
          primary: '#FF6B35', // Extract from mascot.png
          secondary: '#F7931E',
          accent: '#FBB03B',
        },
      },
    },
  },
  plugins: [],
}
export default config