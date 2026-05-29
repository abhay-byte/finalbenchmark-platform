@extends('layouts.app')

@section('title', 'Global Leaderboards - FinalBenchmark 2')

@section('content')
<!-- Header Section -->
<section class="py-8">
    <div class="max-w-3xl">
        <h1 class="text-3xl sm:text-4xl font-bold text-white tracking-tight">Global Leaderboard</h1>
        <p class="text-sm sm:text-base text-slate-400 mt-2">See how the top mobile silicon and chipsets stack up. Compare scores based on geometric mean evaluations scaled to a reference chipset.</p>
    </div>
</section>

<!-- Benchmark Mode & Category Navigation -->
<section class="mb-10 flex flex-col gap-4">
    <!-- Mode Selection Row -->
    <div class="glass-panel rounded-2xl p-2 flex flex-col sm:flex-row gap-2 self-start">
        @foreach($modes as $mode)
            <a href="{{ route('rankings', ['mode' => $mode, 'category' => $selectedCategory]) }}" class="px-5 py-3 rounded-xl text-sm font-semibold tracking-wide uppercase transition-all text-center {{ $selectedMode === $mode ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/25' : 'text-slate-400 hover:text-white' }}">
                {{ $mode }}
            </a>
        @endforeach
    </div>

    <!-- Category Filter Row (Only applicable if Mode is Full Benchmark) -->
    @if($selectedMode === 'Full Benchmark')
    <div class="flex flex-wrap gap-2 items-center">
        @foreach(['all' => 'Overall Score', 'cpu' => 'CPU Only', 'gpu' => 'GPU Only'] as $key => $label)
            <a href="{{ route('rankings', ['mode' => $selectedMode, 'category' => $key]) }}" class="px-4 py-2 rounded-xl text-xs font-semibold tracking-wide uppercase transition-all {{ $selectedCategory === $key ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/40' : 'bg-slate-900 border border-slate-800 text-slate-400 hover:text-white' }}">
                {{ $label }}
            </a>
        @endforeach
    </div>
    @endif
</section>

<!-- Leaderboard Standings Grid/Table -->
<section class="glass-panel rounded-2xl overflow-hidden shadow-2xl">
    <div class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
            <thead>
                <tr class="bg-slate-950/80 border-b border-slate-900 text-[10px] text-slate-500 font-bold uppercase tracking-wider">
                    <th class="px-6 py-4">Rank</th>
                    <th class="px-6 py-4">SoC / Chipset</th>
                    <th class="px-6 py-4">Graphics Processor</th>
                    <th class="px-6 py-4">Avg Score</th>
                    <th class="px-6 py-4">Grade</th>
                    <th class="px-6 py-4">Avg Temp Range</th>
                </tr>
            </thead>
            <tbody class="divide-y divide-slate-900/60 text-sm">
                @foreach($rankings as $row)
                <tr class="hover:bg-slate-800/20 transition-colors">
                    <!-- Rank Column with medals -->
                    <td class="px-6 py-3 font-bold">
                        @if($row['rank'] === 1)
                            <span class="inline-flex items-center justify-center w-8 h-8 rounded-full bg-yellow-500/10 border border-yellow-500/30 text-yellow-500 text-sm">🥇</span>
                        @elseif($row['rank'] === 2)
                            <span class="inline-flex items-center justify-center w-8 h-8 rounded-full bg-slate-400/10 border border-slate-400/30 text-slate-300 text-sm">🥈</span>
                        @elseif($row['rank'] === 3)
                            <span class="inline-flex items-center justify-center w-8 h-8 rounded-full bg-amber-600/10 border border-amber-600/30 text-amber-500 text-sm">🥉</span>
                        @else
                            <span class="inline-flex items-center justify-center w-8 h-8 text-slate-500 text-sm">{{ $row['rank'] }}</span>
                        @endif
                    </td>
                    <!-- SoC Name -->
                    <td class="px-6 py-3 font-bold text-white text-sm">{{ $row['chipset'] }}</td>
                    <!-- GPU Model -->
                    <td class="px-6 py-3 text-slate-400 text-sm">{{ $row['gpu_model'] }}</td>
                    <!-- Score -->
                    <td class="px-6 py-3 font-extrabold text-indigo-400 font-sans text-sm">{{ number_format($row['score']) }}</td>
                    <!-- Grade -->
                    <td class="px-6 py-3">
                        <span class="px-2.5 py-1 rounded-lg text-xs font-bold bg-indigo-500/10 border border-indigo-500/20 text-indigo-300">{{ $row['grade'] }}</span>
                    </td>
                    <!-- Thermal metrics -->
                    <td class="px-6 py-3 text-slate-400 text-xs">{{ $row['temp'] }}</td>
                </tr>
                @endforeach
            </tbody>
        </table>
    </div>
</section>
@endsection
