@extends('layouts.app')

@section('title', 'Terms of Service - FinalBenchmark 2')

@section('content')
<section class="py-12 max-w-4xl mx-auto">
    <div class="glass-panel rounded-3xl p-8 sm:p-12 glow-card-violet">
        <h1 class="text-3xl font-extrabold text-white tracking-tight mb-2">Terms of Service</h1>
        <p class="text-xs text-slate-500 mb-8">Last Updated: May 2026</p>
        
        <div class="space-y-6 text-sm text-slate-400 leading-relaxed">
            <div>
                <h2 class="text-lg font-bold text-white mb-2">1. Agreement to Terms</h2>
                <p>
                    By downloading the FinalBenchmark 2 Android application or accessing the FinalBenchmark 2 website, you agree to comply with and be bound by these Terms of Service. If you do not agree to these terms, you must immediately uninstall the application and discontinue use of the site.
                </p>
            </div>
            
            <div>
                <h2 class="text-lg font-bold text-white mb-2">2. Description of Service</h2>
                <p>
                    FinalBenchmark 2 provides native benchmarking and stress-testing tools for Android mobile devices. We provide diagnostic results, performance ratings, and leaderboards comparing device scores. The service is provided "as is" and we are not responsible for any direct or indirect damage to your device arising from performance testing.
                </p>
            </div>

            <div>
                <h2 class="text-lg font-bold text-white mb-2">3. Thermal Stress & Device Safety</h2>
                <p>
                    FinalBenchmark 2 executes CPU and GPU tests that intentionally place a high workload on your device. While the application features active thermal safety checks to protect your device from overheating, you acknowledge that conducting benchmarks carries an inherent risk of thermal stress and battery drain. Use the application at your own risk.
                </p>
            </div>

            <div>
                <h2 class="text-lg font-bold text-white mb-2">4. User Uploads & License</h2>
                <p>
                    When you upload benchmark results to our global leaderboards (either anonymously or via an authenticated account), you grant us a perpetual, worldwide, royalty-free, non-exclusive license to use, host, store, reproduce, and display these hardware specifications and test results for analytics and public device comparisons.
                </p>
            </div>

            <div>
                <h2 class="text-lg font-bold text-white mb-2">5. Prohibited Conduct</h2>
                <p>
                    You agree not to modify or interfere with the benchmark calculations, inject false values into uploaded datasets, use automated scraping tools to extract rankings data from our API, or impersonate other devices to corrupt global scoring lists.
                </p>
            </div>

            <div class="border-t border-slate-900 pt-6">
                <p class="text-xs text-slate-500">
                    If you have any questions or feedback regarding these terms, please connect with our community in our official Discord server.
                </p>
            </div>
        </div>
    </div>
</section>
@endsection
