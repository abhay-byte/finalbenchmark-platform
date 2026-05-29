@extends('layouts.app')

@section('title', 'Privacy Policy - FinalBenchmark 2')

@section('content')
<section class="py-12 max-w-4xl mx-auto">
    <div class="glass-panel rounded-3xl p-8 sm:p-12 glow-card-cyan">
        <h1 class="text-3xl font-extrabold text-white tracking-tight mb-2">Privacy Policy</h1>
        <p class="text-xs text-slate-500 mb-8">Last Updated: May 2026</p>
        
        <div class="space-y-6 text-sm text-slate-400 leading-relaxed">
            <div>
                <h2 class="text-lg font-bold text-white mb-2">1. Information We Collect</h2>
                <p>
                    FinalBenchmark 2 is designed with a privacy-first approach. By default, the application runs entirely offline. If you opt-in to share your scores, we collect:
                </p>
                <ul class="list-disc list-inside mt-2 space-y-1 pl-4">
                    <li>Device model, manufacturer, and system specifications (CPU model, core counts, RAM type, storage technology).</li>
                    <li>Benchmark test execution times and calculation metrics.</li>
                    <li>Battery state, thermal parameters, and operating system API levels during the test.</li>
                </ul>
            </div>
            
            <div>
                <h2 class="text-lg font-bold text-white mb-2">2. Anonymous vs Authenticated Mode</h2>
                <p>
                    <strong>Anonymous Mode:</strong> Benchmark results are uploaded without any account or user identification. The data is utilized strictly for aggregate global device rankings.
                </p>
                <p class="mt-2">
                    <strong>Authenticated Mode:</strong> If you choose to log in (using your email or Google Account), we associate submitted benchmark results with your account ID so you can synchronize and track your personal benchmark history across multiple devices.
                </p>
            </div>

            <div>
                <h2 class="text-lg font-bold text-white mb-2">3. How We Use Your Data</h2>
                <p>
                    We process uploaded benchmark data to generate:
                </p>
                <ul class="list-disc list-inside mt-2 space-y-1 pl-4">
                    <li>Global leaderboards and rankings tables.</li>
                    <li>Analytical device comparison models.</li>
                    <li>Aggregated thermal stability and performance charts.</li>
                </ul>
            </div>

            <div>
                <h2 class="text-lg font-bold text-white mb-2">4. Data Sharing & Security</h2>
                <p>
                    We do not sell, rent, or trade your personal account information. All data uploaded to our Firebase Realtime Database is handled securely. Public leaderboards only exhibit device specifications and performance ratings, never your private account credentials.
                </p>
            </div>

            <div>
                <h2 class="text-lg font-bold text-white mb-2">5. Your Control & Rights (GDPR)</h2>
                <p>
                    You remain in full control of your data. You may delete any of your uploaded benchmark runs from your local history or authenticated account profile. If you wish to permanently delete all data associated with your cloud account, you may request account deletion directly within the application settings or by contacting us.
                </p>
            </div>

            <div class="border-t border-slate-900 pt-6">
                <p class="text-xs text-slate-500">
                    Your trust is paramount. We strive to provide transparent diagnostics while strictly safeguarding your privacy and security.
                </p>
            </div>
        </div>
    </div>
</section>
@endsection
