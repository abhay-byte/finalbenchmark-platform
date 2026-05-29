@extends('layouts.app')

@section('title', 'Recent Benchmark Runs - FinalBenchmark 2')

@section('content')
<!-- Header Section -->
<section class="py-8">
    <div class="max-w-3xl">
        <h1 class="text-3xl sm:text-4xl font-bold text-white tracking-tight">Recent Benchmark Runs</h1>
        <p class="text-sm sm:text-base text-slate-400 mt-2">Explore live benchmark results uploaded anonymously by the community. Inspect hardware chipsets, detailed sub-test results, and thermal behaviors.</p>
    </div>
</section>

<!-- Search & Filter Toolbar -->
<section class="mb-10">
    <div class="glass-panel rounded-2xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <!-- Search Input -->
        <form method="GET" action="{{ route('benchmarks') }}" class="w-full md:w-80 flex items-center relative">
            @if(request('category'))
                <input type="hidden" name="category" value="{{ request('category') }}">
            @endif
            <input type="text" name="search" value="{{ $search }}" placeholder="Search chipset..." class="w-full bg-slate-950/60 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500">
            <button type="submit" class="absolute right-3 text-slate-500 hover:text-slate-300">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                </svg>
            </button>
        </form>

        <!-- Category Filters -->
        <div class="flex flex-wrap gap-2 items-center">
            @foreach(['all' => 'All', 'cpu' => 'CPU', 'gpu' => 'GPU', 'ram' => 'RAM', 'storage' => 'Storage', 'ai / ml' => 'AI / ML', 'productivity' => 'Productivity'] as $key => $label)
                <a href="{{ route('benchmarks', ['category' => $key, 'search' => $search]) }}" class="px-4 py-2 rounded-xl text-xs font-semibold tracking-wide uppercase transition-all {{ strtolower($selectedCategory) === $key ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25' : 'bg-slate-900 border border-slate-800 text-slate-300 hover:bg-slate-800 hover:text-white' }}">
                    {{ $label }}
                </a>
            @endforeach
        </div>
    </div>
</section>

<!-- Benchmarks List -->
<section class="space-y-6">
    @if(count($benchmarks) === 0)
    <div class="glass-panel rounded-2xl p-12 text-center flex flex-col items-center justify-center space-y-4">
        <div class="w-16 h-16 rounded-full bg-slate-800/80 flex items-center justify-center text-slate-500">
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path></svg>
        </div>
        <div>
            <h3 class="text-lg font-bold text-white">No results found</h3>
            <p class="text-sm text-slate-500 mt-1">Try widening your filters or adjusting your search phrase.</p>
        </div>
        <a href="{{ route('benchmarks') }}" class="px-5 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-semibold hover:bg-indigo-500 transition-colors">Reset All Filters</a>
    </div>
    @else
    <div class="glass-panel rounded-2xl overflow-hidden divide-y divide-slate-900/60 shadow-2xl">
    @foreach($benchmarks as $item)
    <div class="p-6 flex flex-col space-y-5 transition-colors hover:bg-slate-800/10">
    <!-- Top Section: Device Summary -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
    <div class="flex items-start space-x-4">
    <div class="w-12 h-12 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 shrink-0">
    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z"></path>
    </svg>
    </div>
    <div>
    <div class="flex flex-wrap items-center gap-2.5">
    <h2 class="text-lg sm:text-xl font-bold text-white leading-none">{{ $item['chipset'] }}</h2>
    <span class="px-2 py-0.5 rounded bg-indigo-500/10 text-[10px] text-indigo-300 border border-indigo-500/25 font-semibold uppercase tracking-wider">{{ $item['mode'] }}</span>
    </div>
    <p class="text-xs text-slate-400 mt-1.5">
    <strong class="text-slate-300">GPU Model:</strong> {{ $item['gpu_model'] }}
    </p>
    </div>
    </div>
    
    <!-- Score and Grade Details -->
    <div class="flex items-center space-x-5 shrink-0 self-end md:self-auto">
    <div class="text-right">
    <span class="text-[10px] text-slate-500 font-semibold block uppercase tracking-wider">Mode Score</span>
    <span class="text-2xl font-black text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 to-cyan-400">{{ number_format($item['score']) }}</span>
    </div>
    <div class="w-11 h-11 rounded-xl bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 font-extrabold text-base flex items-center justify-center shadow-lg shadow-indigo-500/5">
    {{ $item['grade'] }}
    </div>
    </div>
    </div>
    
    <!-- Mid Section: Sub-Metrics Grid -->
    <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 border-t border-slate-900/60 pt-4.5 text-xs">
    <div>
    <span class="text-[10px] text-slate-500 font-semibold uppercase block tracking-wider mb-0.5">Category</span>
    <span class="font-semibold text-slate-200 text-sm">{{ $item['category'] }}</span>
    </div>
    <div>
    <span class="text-[10px] text-slate-500 font-semibold uppercase block tracking-wider mb-0.5">Thermal Profile</span>
    <span class="font-semibold text-orange-400 text-sm flex items-center">
    <svg class="w-4 h-4 mr-1 text-orange-500" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg"><path fill-rule="evenodd" d="M3.172 5.172a4 4 0 015.656 0L10 6.343l1.172-1.171a4 4 0 115.656 5.656L10 17.657l-6.828-6.829a4 4 0 010-5.656z" clip-rule="evenodd"></path></svg>
    {{ $item['temp_start'] }}°C ➜ {{ $item['temp_end'] }}°C
    </span>
    </div>
    <div>
    <span class="text-[10px] text-slate-500 font-semibold uppercase block tracking-wider mb-0.5">Power Usage</span>
    <span class="font-semibold text-cyan-400 text-sm">{{ $item['power'] }}</span>
    </div>
    <div>
    <span class="text-[10px] text-slate-500 font-semibold uppercase block tracking-wider mb-0.5">Submitted</span>
    <span class="font-semibold text-slate-400 text-sm">{{ $item['time'] }}</span>
    </div>
    </div>
    
    <!-- Expandable Drawer for detailed subtests -->
    <div class="bg-slate-950/30 rounded-xl border border-slate-900/40 p-4 mt-2">
    <span class="text-[10px] text-slate-500 font-bold uppercase tracking-wider block mb-2">Detailed Sub-Tests</span>
    <div class="flex flex-wrap gap-2">
    @foreach($item['details'] as $testName => $resultVal)
    <div class="px-3 py-1.5 rounded-lg bg-slate-900/80 border border-slate-800/60 text-xs flex justify-between gap-4">
    <span class="text-slate-400">{{ $testName }}:</span>
    <span class="font-bold text-white">{{ $resultVal }}</span>
    </div>
    @endforeach
    </div>
    </div>
    </div>
    @endforeach
    </div>
    @endif
</section>
@endsection
