import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'FinalBenchmark2 - Professional CPU Benchmarking App',
  description: 'Comprehensive CPU benchmarking application with real-time testing, detailed reports, and performance analysis.',
  keywords: 'benchmark, CPU, Android, performance, testing',
  authors: [{ name: 'FinalBenchmark Team' }],
  openGraph: {
    title: 'FinalBenchmark2',
    description: 'Professional CPU Benchmarking for Android',
    type: 'website',
  },
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className="font-roboto antialiased">
        {children}
      </body>
    </html>
  )
}
