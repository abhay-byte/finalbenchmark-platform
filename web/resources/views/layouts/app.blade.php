<!DOCTYPE html>
<html lang="en" class="h-full">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="description" content="FinalBenchmark 2 - The ultimate multi-component performance benchmark for Android. Compare CPU, GPU, RAM, Storage, AI, and Productivity performance.">
    <title>@yield('title', 'FinalBenchmark 2 - Next-Gen Android Performance Evaluation')</title>
    <!-- Favicon -->
    <link rel="icon" type="image/png" href="/assets/logo_2.png?v=1.0.1">
    <!-- Outfit & Inter Fonts -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    
    @vite(['resources/css/app.css', 'resources/js/app.js'])
</head>
<body class="bg-[#070b13] text-slate-100 font-sans h-full flex flex-col antialiased selection:bg-indigo-500 selection:text-white overflow-x-hidden">
    
    <!-- Top-Level Background Glimmers -->
    <div class="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] rounded-full bg-indigo-500/10 blur-[120px] pointer-events-none"></div>
    <div class="absolute top-[30%] right-[-10%] w-[45%] h-[45%] rounded-full bg-cyan-500/10 blur-[120px] pointer-events-none"></div>

    <!-- Sticky Navigation Header -->
    <header class="sticky top-0 z-50 w-full px-4 sm:px-6 lg:px-8 py-4">
        <nav class="max-w-7xl mx-auto glass-panel rounded-2xl px-6 py-4 flex items-center justify-between">
            <!-- Brand Logo -->
            <a href="{{ route('home') }}" class="flex items-center space-x-3 group">
                <img src="/assets/logo_2.png" alt="FinalBenchmark Logo" class="w-10 h-10 object-contain rounded-xl shadow-lg shadow-indigo-500/10 group-hover:scale-105 transition-transform" />
                <div class="flex flex-col">
                    <span class="text-xl font-bold tracking-tight font-sans text-white">FinalBenchmark <span class="text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 to-cyan-400 font-extrabold">2</span></span>
                    <span class="text-[10px] tracking-wider text-slate-400 font-medium">ANDROID PLATFORM</span>
                </div>
            </a>

            <!-- Navigation Links -->
            <div class="hidden md:flex items-center space-x-8">
                <a href="{{ route('home') }}" class="text-sm font-medium transition-colors {{ Route::currentRouteName() === 'home' ? 'text-indigo-400' : 'text-slate-300 hover:text-white' }}">Home</a>
                <a href="{{ route('benchmarks') }}" class="text-sm font-medium transition-colors {{ Route::currentRouteName() === 'benchmarks' ? 'text-indigo-400' : 'text-slate-300 hover:text-white' }}">Recent Benchmarks</a>
                <a href="{{ route('rankings') }}" class="text-sm font-medium transition-colors {{ Route::currentRouteName() === 'rankings' ? 'text-indigo-400' : 'text-slate-300 hover:text-white' }}">Leaderboard</a>
            </div>

            <!-- Header Actions / Mobile Menu Button -->
            <div class="flex items-center space-x-4">
                <a href="{{ route('home') }}#download" class="hidden sm:inline-flex items-center justify-center px-5 py-2.5 rounded-xl text-sm font-semibold text-white bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 transition-all shadow-md shadow-indigo-600/20 active:scale-[0.98]">
                    Download App
                </a>
                
                <!-- Mobile Navigation Toggle -->
                <button type="button" class="md:hidden text-slate-400 hover:text-white focus:outline-none" id="mobile-menu-toggle">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16m-7 6h7"></path>
                    </svg>
                </button>
            </div>
        </nav>

        <!-- Mobile Menu Overlay -->
        <div class="hidden md:hidden mt-2 px-2" id="mobile-menu">
            <div class="glass-panel rounded-2xl px-4 py-4 flex flex-col space-y-3">
                <a href="{{ route('home') }}" class="px-3 py-2 rounded-xl text-sm font-medium {{ Route::currentRouteName() === 'home' ? 'bg-indigo-500/10 text-indigo-400 font-semibold' : 'text-slate-300 hover:bg-slate-800/40 hover:text-white' }}">Home</a>
                <a href="{{ route('benchmarks') }}" class="px-3 py-2 rounded-xl text-sm font-medium {{ Route::currentRouteName() === 'benchmarks' ? 'bg-indigo-500/10 text-indigo-400 font-semibold' : 'text-slate-300 hover:bg-slate-800/40 hover:text-white' }}">Recent Benchmarks</a>
                <a href="{{ route('rankings') }}" class="px-3 py-2 rounded-xl text-sm font-medium {{ Route::currentRouteName() === 'rankings' ? 'bg-indigo-500/10 text-indigo-400 font-semibold' : 'text-slate-300 hover:bg-slate-800/40 hover:text-white' }}">Leaderboard</a>
                <a href="{{ route('home') }}#download" class="w-full text-center px-4 py-2.5 mt-2 rounded-xl bg-indigo-600 text-white font-semibold text-sm hover:bg-indigo-500">Download App</a>
            </div>
        </div>
    </header>

    <!-- Main Content -->
    <main class="flex-grow max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 relative z-10">
        @yield('content')
    </main>

    <!-- Footer -->
    <footer class="w-full bg-[#04060c] border-t border-slate-900 px-4 sm:px-6 lg:px-8 py-12 relative z-10">
        <div class="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-4 gap-8">
            <div class="md:col-span-2 flex flex-col space-y-4">
                <a href="{{ route('home') }}" class="flex items-center space-x-3">
                    <img src="/assets/logo_2.png" alt="FinalBenchmark Logo" class="w-8 h-8 object-contain rounded-lg" />
                    <span class="text-lg font-bold font-sans text-white">FinalBenchmark <span class="text-indigo-400 font-extrabold">2</span></span>
                </a>
                <p class="text-sm text-slate-400 max-w-sm">
                    FinalBenchmark 2 is an open-source, comprehensive Android benchmarking utility measuring CPU, GPU, RAM, Storage, AI, and Productivity performance with ultimate precision.
                </p>
                <div class="flex items-center space-x-4 pt-2">
                    <a href="https://github.com/abhay-byte/finalbenchmark-platform" class="text-slate-400 hover:text-white transition-colors" aria-label="GitHub">
                        <svg class="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/></svg>
                    </a>
                    <a href="https://discord.gg/khzKmGzfRf" class="text-slate-400 hover:text-white transition-colors" aria-label="Discord">
                        <svg class="w-5 h-5 fill-current" viewBox="0 0 24 24"><path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057a.082.082 0 0 0 .031.057 19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.873-.894.077.077 0 0 1-.008-.128c.126-.093.252-.19.372-.287a.075.075 0 0 1 .077-.011c3.92 1.793 8.18 1.793 12.061 0a.073.073 0 0 1 .078.009c.12.099.246.195.373.289a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.894.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03zM8.02 15.33c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.956-2.419 2.156-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.956 2.418-2.156 2.418zm7.975 0c-1.183 0-2.157-1.085-2.157-2.419 0-1.333.955-2.419 2.156-2.419 1.21 0 2.176 1.096 2.157 2.42 0 1.333-.946 2.418-2.156 2.418z"/></svg>
                    </a>
                </div>
            </div>
            
            <div class="flex flex-col space-y-4">
                <span class="text-sm font-semibold tracking-wider text-slate-200">RESOURCES</span>
                <div class="flex flex-col space-y-2 text-sm text-slate-400">
                    <a href="{{ route('home') }}" class="hover:text-white transition-colors">Home</a>
                    <a href="{{ route('benchmarks') }}" class="hover:text-white transition-colors">Recent Benchmarks</a>
                    <a href="{{ route('rankings') }}" class="hover:text-white transition-colors">Global Leaderboard</a>
                </div>
            </div>

            <div class="flex flex-col space-y-4">
                <span class="text-sm font-semibold tracking-wider text-slate-200">LEGAL</span>
                <div class="flex flex-col space-y-2 text-sm text-slate-400">
                    <a href="{{ route('terms') }}" class="hover:text-white transition-colors">Terms of Service</a>
                    <a href="{{ route('privacy') }}" class="hover:text-white transition-colors">Privacy Policy</a>
                </div>
            </div>
        </div>
        
        <div class="max-w-7xl mx-auto mt-12 pt-8 border-t border-slate-900/60 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500">
            <span>&copy; {{ date('Y') }} FinalBenchmark 2. All rights reserved.</span>
            <span class="mt-2 sm:mt-0">Built with Laravel 13, Firebase & Tailwind v4</span>
        </div>
    </footer>

    <!-- Mobile Menu Toggle Script -->
    <script>
        document.getElementById('mobile-menu-toggle').addEventListener('click', function() {
            var menu = document.getElementById('mobile-menu');
            menu.classList.toggle('hidden');
        });
    </script>
</body>
</html>
