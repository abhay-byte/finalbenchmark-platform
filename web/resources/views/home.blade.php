@extends('layouts.app')

@section('title', 'FinalBenchmark 2 - Unleash & Measure Android Performance')

@section('content')
<!-- Hero Section -->
<section class="py-16 lg:py-24 relative">
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
        <!-- Text Column -->
        <div class="lg:col-span-7 flex flex-col space-y-6 text-center lg:text-left">
            <div class="inline-flex self-center lg:self-start items-center space-x-2 px-3 py-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-xs font-semibold text-indigo-300 uppercase tracking-wider">
                <span>⚡ Version 1.0.0 Now Live</span>
            </div>
            
            <h1 class="text-4xl sm:text-6xl font-bold font-sans text-white leading-tight">
                Unleash & Measure <br>
                <span class="text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 via-purple-400 to-cyan-400 font-extrabold">Android Performance</span>
            </h1>
            
            <p class="text-base sm:text-lg text-slate-400 max-w-xl mx-auto lg:mx-0">
                Experience the ultimate open-source benchmarking framework. Run 46 native CPU, GPU, RAM, Storage, AI/ML, and Productivity stress tests with detailed real-time thermal and power metrics.
            </p>
            
            <div class="flex flex-col sm:flex-row items-center justify-center lg:justify-start gap-4 pt-4">
                <a href="#download" class="w-full sm:w-auto inline-flex items-center justify-center px-8 py-4 rounded-2xl text-base font-bold text-white bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 transition-all shadow-xl shadow-indigo-600/30 active:scale-[0.98]">
                    <svg class="w-5 h-5 mr-3" fill="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path d="M17.523 15.3l-5.523 5.523-5.523-5.523 1.414-1.414 3.109 3.11v-12.82h2v12.82l3.109-3.11z"></path>
                    </svg>
                    Download APK
                </a>
                <a href="{{ route('rankings') }}" class="w-full sm:w-auto inline-flex items-center justify-center px-8 py-4 rounded-2xl text-base font-bold text-slate-300 hover:text-white glass-panel hover:bg-slate-800/40 transition-all">
                    View Leaderboard
                </a>
            </div>
        </div>
        
        <!-- App Mockup Column -->
        <div class="lg:col-span-5 relative">
            <div class="absolute -inset-4 bg-gradient-to-tr from-indigo-500 to-cyan-400 rounded-3xl opacity-20 blur-3xl pointer-events-none"></div>
            <!-- Glassmorphic Mockup Panel -->
            <div class="glass-panel rounded-3xl p-6 relative z-10 glow-card-violet max-w-sm mx-auto shadow-2xl">
                <!-- Inner Screen Header -->
                <div class="flex justify-between items-center border-b border-slate-800/50 pb-4 mb-6">
                    <div class="flex items-center space-x-2">
                        <div class="w-3 h-3 rounded-full bg-red-500"></div>
                        <div class="w-3 h-3 rounded-full bg-yellow-500"></div>
                        <div class="w-3 h-3 rounded-full bg-green-500"></div>
                    </div>
                    <span class="text-xs font-semibold text-slate-400">FinalBenchmark App HUD</span>
                </div>
                
                <!-- Reactor Progress Ring Representation -->
                <div class="flex flex-col items-center py-6">
                    <div class="w-40 h-40 rounded-full border-[10px] border-indigo-500/10 border-t-indigo-500 border-r-cyan-400 flex items-center justify-center relative animate-[spin_6s_linear_infinite]">
                        <div class="absolute w-28 h-28 rounded-full bg-[#080d16] flex flex-col items-center justify-center -rotate-90 select-none animate-[pulse_2s_infinite]">
                            <span class="text-3xl font-extrabold text-white">98%</span>
                            <span class="text-[9px] tracking-widest text-cyan-400 font-bold uppercase mt-1">PROCESSING</span>
                        </div>
                    </div>
                    <span class="text-sm font-semibold text-slate-200 mt-6 tracking-wide">RAM Sequential Write Test</span>
                    <span class="text-xs text-indigo-400 font-bold mt-1">32.8 GB/s (Active)</span>
                </div>

                <!-- Stats Footer -->
                <div class="grid grid-cols-3 gap-2 border-t border-slate-800/50 pt-4 mt-4 text-center">
                    <div>
                        <div class="text-[10px] text-slate-500 font-semibold uppercase">CPU Load</div>
                        <div class="text-sm font-bold text-slate-200">87%</div>
                    </div>
                    <div>
                        <div class="text-[10px] text-slate-500 font-semibold uppercase">CPU Temp</div>
                        <div class="text-sm font-bold text-orange-400">38.2°C</div>
                    </div>
                    <div>
                        <div class="text-[10px] text-slate-500 font-semibold uppercase">Memory</div>
                        <div class="text-sm font-bold text-slate-200">1420MB</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</section>

<!-- Core Metrics Counters -->
<section class="py-8">
    <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
        <div class="glass-panel rounded-2xl p-6 text-center">
            <span class="block text-3xl font-extrabold text-white">46</span>
            <span class="text-xs text-slate-400 font-medium uppercase tracking-wider mt-1 block">Stress Tests</span>
        </div>
        <div class="glass-panel rounded-2xl p-6 text-center">
            <span class="block text-3xl font-extrabold text-indigo-400">6</span>
            <span class="text-xs text-slate-400 font-medium uppercase tracking-wider mt-1 block">Categories</span>
        </div>
        <div class="glass-panel rounded-2xl p-6 text-center">
            <span class="block text-3xl font-extrabold text-cyan-400">SD 8 Gen 3</span>
            <span class="text-xs text-slate-400 font-medium uppercase tracking-wider mt-1 block">Baseline Ref</span>
        </div>
        <div class="glass-panel rounded-2xl p-6 text-center">
            <span class="block text-3xl font-extrabold text-white">100%</span>
            <span class="text-xs text-slate-400 font-medium uppercase tracking-wider mt-1 block">Open Source</span>
        </div>
    </div>
</section>

<!-- Features Grid -->
<section class="py-16 relative">
    <div class="text-center max-w-2xl mx-auto mb-16">
        <h2 class="text-3xl sm:text-4xl font-bold text-white tracking-tight">Comprehensive Device Diagnostics</h2>
        <p class="text-sm sm:text-base text-slate-400 mt-3">FinalBenchmark 2 evaluates and stresses every component of your Android hardware using standard reference algorithms.</p>
    </div>
    
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <!-- CPU Card -->
        <div class="glass-panel rounded-2xl p-6 glow-card-violet glass-panel-hover flex flex-col space-y-4">
            <div class="w-12 h-12 rounded-xl bg-violet-500/10 border border-violet-500/20 flex items-center justify-center text-violet-400">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 5h10a2 2 0 012 2v10a2 2 0 01-2 2H7a2 2 0 01-2-2V7a2 2 0 012-2z"></path>
                </svg>
            </div>
            <h3 class="text-xl font-bold text-white">CPU Performance</h3>
            <p class="text-sm text-slate-400">Tests integer, floating-point, Ray Tracing, JSON parsing, MD5/SHA hashes, and recursive algorithms in Single and Multi-core modes.</p>
        </div>

        <!-- GPU Card -->
        <div class="glass-panel rounded-2xl p-6 glow-card-cyan glass-panel-hover flex flex-col space-y-4">
            <div class="w-12 h-12 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path>
                </svg>
            </div>
            <h3 class="text-xl font-bold text-white">GPU pipelines</h3>
            <p class="text-sm text-slate-400">Includes Vulkan/OpenGL triangle stress, custom particle systems (100K+), compute shaders, dynamic tessellation, and deep Unreal/Unity deep linking scenes.</p>
        </div>

        <!-- AI/ML Card -->
        <div class="glass-panel rounded-2xl p-6 glow-card-violet glass-panel-hover flex flex-col space-y-4">
            <div class="w-12 h-12 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z"></path>
                </svg>
            </div>
            <h3 class="text-xl font-bold text-white">AI / ML Inference</h3>
            <p class="text-sm text-slate-400">Evaluates on-device AI speeds: LLM inference (llama.cpp with TinyLlama), MobileNetV3 image classification, YOLOv8 object detection, and Whisper speech-to-text.</p>
        </div>

        <!-- RAM Card -->
        <div class="glass-panel rounded-2xl p-6 glow-card-cyan glass-panel-hover flex flex-col space-y-4">
            <div class="w-12 h-12 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
            </div>
            <h3 class="text-xl font-bold text-white">RAM Bandwidth</h3>
            <p class="text-sm text-slate-400">Stresses random and sequential memory access. Evaluates copying speeds, concurrent multi-threaded bandwidth, and maps L1/L2/L3 cache hierarchies.</p>
        </div>

        <!-- Storage Card -->
        <div class="glass-panel rounded-2xl p-6 glow-card-violet glass-panel-hover flex flex-col space-y-4">
            <div class="w-12 h-12 rounded-xl bg-violet-500/10 border border-violet-500/20 flex items-center justify-center text-violet-400">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7v8a2 2 0 002 2h6M8 7V5a2 2 0 012-2h4a2 2 0 012 2v2M8 7H6a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V9a2 2 0 00-2-2h-2M10 11h4m-4 4h4"></path>
                </svg>
            </div>
            <h3 class="text-xl font-bold text-white">Storage Performance</h3>
            <p class="text-sm text-slate-400">Measures sequential read/write, random write with 4K blocks, tiny metadata file indexing, and SQLite database commit transaction times.</p>
        </div>

        <!-- Productivity Card -->
        <div class="glass-panel rounded-2xl p-6 glow-card-cyan glass-panel-hover flex flex-col space-y-4">
            <div class="w-12 h-12 rounded-xl bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z"></path>
                </svg>
            </div>
            <h3 class="text-xl font-bold text-white">Productivity Tests</h3>
            <p class="text-sm text-slate-400">Benchmarks real-world workflows: dynamic UI rendering (RecyclerView), H.264/H.265 transcoding, image filters, PDF layout compiling, and active multi-tasking.</p>
        </div>
    </div>
</section>

<!-- Recent Benchmarks Segment -->
<section class="py-12">
    <div class="flex justify-between items-end mb-8">
        <div>
            <h2 class="text-2xl sm:text-3xl font-bold text-white">Recent Activities</h2>
            <p class="text-xs sm:text-sm text-slate-400 mt-1">Live submissions uploaded anonymously by users globally.</p>
        </div>
        <a href="{{ route('benchmarks') }}" class="text-sm text-indigo-400 hover:text-indigo-300 font-semibold transition-colors flex items-center">
            View All
            <svg class="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path></svg>
        </a>
    </div>
    
    <div class="glass-panel rounded-2xl overflow-hidden divide-y divide-slate-900/60 shadow-2xl">
        @foreach($recent as $item)
        <div class="p-5 sm:p-6 flex flex-col md:flex-row md:items-center justify-between gap-5 transition-colors hover:bg-slate-800/10">
            <div class="flex items-start space-x-4">
                <div class="w-11 h-11 rounded-xl bg-slate-900 border border-slate-800/80 flex items-center justify-center text-slate-400 shrink-0">
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
                    </svg>
                </div>
                <div>
                    <h3 class="text-base sm:text-lg font-bold text-white">{{ $item['chipset'] }}</h3>
                    <span class="text-xs text-slate-400">GPU: {{ $item['gpu_model'] }}</span>
                </div>
            </div>
            
            <div class="flex flex-wrap items-center gap-5 sm:gap-8">
                <div class="text-right">
                    <span class="text-[10px] text-slate-500 font-semibold block uppercase tracking-wider mb-0.5">Category</span>
                    <span class="text-sm font-semibold text-slate-300">{{ $item['category'] }}</span>
                </div>
                <div class="text-right">
                    <span class="text-[10px] text-slate-500 font-semibold block uppercase tracking-wider mb-0.5">Mode</span>
                    <span class="text-sm font-semibold text-slate-300">{{ $item['mode'] }}</span>
                </div>
                <div class="text-right">
                    <span class="text-[10px] text-slate-500 font-semibold block uppercase tracking-wider mb-0.5">Score</span>
                    <span class="text-sm sm:text-base font-extrabold text-indigo-400">{{ number_format($item['score']) }}</span>
                </div>
                <div class="flex items-center justify-center w-9 h-9 rounded-lg bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 font-extrabold text-sm shadow-md shadow-indigo-500/5">
                    {{ $item['grade'] }}
                </div>
            </div>
        </div>
        @endforeach
    </div>
</section>

<!-- Sideload/Download Section -->
<section id="download" class="py-16 scroll-mt-24">
    <div class="glass-panel rounded-3xl p-8 lg:p-12 relative overflow-hidden">
        <!-- background glimmers inside container -->
        <div class="absolute bottom-[-20%] right-[-10%] w-80 h-80 rounded-full bg-cyan-500/10 blur-[80px] pointer-events-none"></div>
        
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center relative z-10">
            <!-- Left Info column -->
            <div class="lg:col-span-7 flex flex-col space-y-6">
                <h2 class="text-3xl sm:text-4xl font-bold text-white tracking-tight">Get FinalBenchmark 2 on F-Droid or Direct Download</h2>
                <p class="text-slate-400 text-sm sm:text-base">
                    Enjoy ads-free and tracking-free hardware diagnostics. Verify and benchmark your devices with compile levels up to Android API Level 36.
                </p>
                <div class="flex flex-wrap items-center gap-4 pt-2">
                    <a href="https://f-droid.org/packages/com.ivarna.finalbenchmark2" class="inline-flex items-center px-6 py-3 rounded-2xl bg-slate-900 border border-slate-800 text-sm font-semibold text-white hover:bg-slate-800 transition-colors">
                        <img src="https://f-droid.org/artwork/badge/get-it-on.png" alt="Get it on F-Droid" class="h-8" />
                    </a>
                    <a href="https://github.com/abhay-byte/finalbenchmark-platform/releases/latest" class="inline-flex items-center justify-center px-6 py-4 rounded-2xl bg-indigo-600 hover:bg-indigo-500 text-sm font-bold text-white transition-all shadow-lg shadow-indigo-600/20 active:scale-[0.98]">
                        Direct APK Download
                    </a>
                </div>
            </div>
            
            <!-- Right installation steps column -->
            <div class="lg:col-span-5">
                <div class="glass-panel rounded-2xl p-6 bg-slate-950/40">
                    <h3 class="text-base font-bold text-white mb-4">Installation Steps</h3>
                    <ul class="space-y-3 text-sm text-slate-400">
                        <li class="flex items-start">
                            <span class="w-6 h-6 rounded-full bg-indigo-500/10 text-indigo-400 font-bold text-xs flex items-center justify-center shrink-0 mr-3">1</span>
                            <span>Download the latest <strong>.apk</strong> from F-Droid or GitHub Releases.</span>
                        </li>
                        <li class="flex items-start">
                            <span class="w-6 h-6 rounded-full bg-indigo-500/10 text-indigo-400 font-bold text-xs flex items-center justify-center shrink-0 mr-3">2</span>
                            <span>Open the downloaded file. Allow <strong>"Install from Unknown Sources"</strong> if prompted by your browser/system.</span>
                        </li>
                        <li class="flex items-start">
                            <span class="w-6 h-6 rounded-full bg-indigo-500/10 text-indigo-400 font-bold text-xs flex items-center justify-center shrink-0 mr-3">3</span>
                            <span>Launch the app, accept the required monitoring permissions, and select your benchmark mode!</span>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </div>
</section>
@endsection
