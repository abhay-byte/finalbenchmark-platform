'use client'

import Image from 'next/image'
import Link from 'next/link'
import { useState, useEffect } from 'react'

export default function AppBar() {
  const [scrolled, setScrolled] = useState(false)

  // Handle scroll effect
  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 10)
    }
    window.addEventListener('scroll', handleScroll)
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  return (
    <nav 
      className={`
        fixed top-0 left-0 right-0 z-50 transition-all duration-300 ease-in-out
        ${scrolled 
          ? 'bg-white shadow-lg border-b border-gray-100' 
          : 'bg-white/95 backdrop-blur-sm shadow-sm border-b border-gray-50'
        }
        h-16 md:h-16 flex items-center justify-between px-4 md:px-6 lg:px-8
      `}
      role="navigation"
      aria-label="Main navigation"
    >
      {/* Left Section: Logo + Brand Name */}
      <div className="flex items-center gap-3">
        <Link 
          href="/" 
          className="flex items-center gap-3 hover:scale-105 transition-transform duration-200 focus:outline-none focus:ring-2 focus:ring-mascot-primary focus:ring-offset-2 rounded-lg p-1"
          aria-label="FinalBenchmark2 - Home"
        >
          <Image 
            src="/assets/logo_2.png"
            alt="FinalBenchmark2 Logo"
            width={40}
            height={40}
            priority
            className="drop-shadow-sm"
          />
          <span className="text-xl font-bold hidden sm:block text-gray-900">
            FinalBenchmark2
          </span>
        </Link>
      </div>

      {/* Right Section: Download Buttons */}
      <div className="flex items-center gap-3">
        {/* Play Store Button */}
        <Link 
          href="#download"
          className="
            group relative overflow-hidden rounded-lg transition-all duration-200 
            hover:scale-105 hover:shadow-lg focus:outline-none focus:ring-2 
            focus:ring-mascot-primary focus:ring-offset-2
          "
          aria-label="Download from Google Play Store"
        >
          <div className="bg-gradient-to-r from-[#34A853] to-[#4285F4] px-4 py-2 flex items-center gap-2">
            <div className="w-6 h-6 bg-white rounded-sm flex items-center justify-center">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" className="text-[#4285F4]">
                <path d="M3 20V4L20 12L3 20Z" fill="currentColor"/>
                <path d="M10 14V6L20 12L10 14Z" fill="currentColor"/>
              </svg>
            </div>
            <span className="text-white text-sm font-medium hidden md:block">
              Play Store
            </span>
          </div>
        </Link>

        {/* F-Droid Button */}
        <Link 
          href="#download"
          className="
            group relative overflow-hidden rounded-lg transition-all duration-200 
            hover:scale-105 hover:shadow-lg focus:outline-none focus:ring-2 
            focus:ring-mascot-primary focus:ring-offset-2 px-4 py-2
            bg-gradient-to-r from-[#1976D2] to-[#2196F3] text-white
          "
          aria-label="Download from F-Droid"
        >
          <span className="text-sm font-medium">F-Droid</span>
        </Link>
      </div>
    </nav>
  )
}