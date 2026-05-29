<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class PageController extends Controller
{
    /**
     * Get baseline dummy benchmark data.
     */
    private function getDummyBenchmarks()
    {
        return [
            [
                'id' => '1',
                'chipset' => 'Snapdragon 8 Gen 3',
                'gpu_model' => 'Adreno 750',
                'score' => 313,
                'grade' => 'A+',
                'mode' => 'Full Benchmark',
                'category' => 'CPU',
                'temp_start' => 31.2,
                'temp_end' => 38.5,
                'power' => '4.2W',
                'time' => '2 mins ago',
                'details' => ['Prime Gen' => '748 Mops/s', 'Matrix Mult' => '3.82 Gops/s', 'LZMA Comp' => '750 Mops/s']
            ],
            [
                'id' => '2',
                'chipset' => 'Snapdragon 8 Gen 3',
                'gpu_model' => 'Adreno 750',
                'score' => 97,
                'grade' => 'A+',
                'mode' => 'Full Benchmark',
                'category' => 'GPU',
                'temp_start' => 30.5,
                'temp_end' => 39.1,
                'power' => '4.5W',
                'time' => '15 mins ago',
                'details' => ['Triangle Stress' => '120 FPS', 'Compute Shader' => '4.1 TFLOPs', 'Particle Sim' => '105K particles']
            ],
            [
                'id' => '3',
                'chipset' => 'Google Tensor G4',
                'gpu_model' => 'Mali-G715 MC10',
                'score' => 84,
                'grade' => 'A',
                'mode' => 'Full Benchmark',
                'category' => 'AI / ML',
                'temp_start' => 32.0,
                'temp_end' => 41.2,
                'power' => '3.8W',
                'time' => '45 mins ago',
                'details' => ['LLM Inference' => '12.4 tok/s', 'Image Class' => '540 FPS', 'Speech-to-Text' => '0.45s transcription']
            ],
            [
                'id' => '4',
                'chipset' => 'Apple A17 Pro',
                'gpu_model' => 'Apple GPU 6-core',
                'score' => 95,
                'grade' => 'A+',
                'mode' => 'Full Benchmark',
                'category' => 'Productivity',
                'temp_start' => 29.8,
                'temp_end' => 37.4,
                'power' => '3.6W',
                'time' => '1 hour ago',
                'details' => ['UI Rendering' => '120 FPS', 'Video Encoding' => '45s render', 'PDF Gen' => '120 pages/s']
            ],
            [
                'id' => '5',
                'chipset' => 'Snapdragon 8 Gen 3',
                'gpu_model' => 'Adreno 750',
                'score' => 9120,
                'grade' => 'A+',
                'mode' => 'Throttle Test',
                'category' => 'CPU',
                'temp_start' => 33.0,
                'temp_end' => 45.0,
                'power' => '5.1W',
                'time' => '2 hours ago',
                'details' => ['Sustained Perf' => '88%', 'Time to Throttle' => '12 mins', 'Peak Temp' => '45.0°C']
            ],
            [
                'id' => '6',
                'chipset' => 'Snapdragon 8 Gen 3',
                'gpu_model' => 'Adreno 750',
                'score' => 95,
                'grade' => 'A+',
                'mode' => 'Full Benchmark',
                'category' => 'RAM',
                'temp_start' => 28.5,
                'temp_end' => 32.1,
                'power' => '3.2W',
                'time' => '3 hours ago',
                'details' => ['Seq Read/Write' => '41.2 GB/s', 'Latency' => '62.4 ns', 'Copy Bandwidth' => '38.5 GB/s']
            ],
            [
                'id' => '7',
                'chipset' => 'Snapdragon 8 Gen 2',
                'gpu_model' => 'Adreno 740',
                'score' => 89,
                'grade' => 'A',
                'mode' => 'Full Benchmark',
                'category' => 'Storage',
                'temp_start' => 29.0,
                'temp_end' => 33.8,
                'power' => '3.5W',
                'time' => '4 hours ago',
                'details' => ['Seq Read' => '3.1 GB/s', 'Seq Write' => '2.8 GB/s', 'Random IOPS' => '180K IOPS']
            ],
            [
                'id' => '8',
                'chipset' => 'Snapdragon 8+ Gen 1',
                'gpu_model' => 'Adreno 730',
                'score' => 9840,
                'grade' => 'A',
                'mode' => 'Efficiency Test',
                'category' => 'CPU',
                'temp_start' => 31.0,
                'temp_end' => 34.2,
                'power' => '2.8W',
                'time' => '5 hours ago',
                'details' => ['Perf/Watt' => '2,803 pts/W', 'Drain rate' => '0.5%/min', 'Peak Temp' => '34.2°C']
            ],
            [
                'id' => '9',
                'chipset' => 'Snapdragon 8 Gen 2',
                'gpu_model' => 'Adreno 740',
                'score' => 88,
                'grade' => 'A',
                'mode' => 'Full Benchmark',
                'category' => 'Productivity',
                'temp_start' => 32.5,
                'temp_end' => 40.1,
                'power' => '4.0W',
                'time' => 'Yesterday',
                'details' => ['UI Rendering' => '118 FPS', 'Video Encoding' => '52s render', 'Multi-tasking' => 'Fluid']
            ],
            [
                'id' => '10',
                'chipset' => 'Dimensity 7200 Ultra',
                'gpu_model' => 'Mali-G610 MC4',
                'score' => 54,
                'grade' => 'C',
                'mode' => 'Full Benchmark',
                'category' => 'AI / ML',
                'temp_start' => 30.2,
                'temp_end' => 36.8,
                'power' => '3.1W',
                'time' => '2 days ago',
                'details' => ['LLM Inference' => '4.2 tok/s', 'Image Class' => '180 FPS', 'Text Embed' => '8.2 vectors/s']
            ]
        ];
    }

    /**
     * Landing Home Page
     */
    public function home()
    {
        $recent = array_slice($this->getDummyBenchmarks(), 0, 3);
        return view('home', compact('recent'));
    }

    /**
     * Recent Benchmarks List
     */
    public function benchmarks(Request $request)
    {
        $category = $request->query('category', 'all');
        $search = $request->query('search', '');

        $benchmarks = $this->getDummyBenchmarks();

        if ($category !== 'all') {
            $benchmarks = array_filter($benchmarks, function ($item) use ($category) {
                return strtolower($item['category']) === strtolower($category);
            });
        }

        if (!empty($search)) {
            $benchmarks = array_filter($benchmarks, function ($item) use ($search) {
                return stripos($item['chipset'], $search) !== false;
            });
        }

        return view('benchmarks', [
            'benchmarks' => $benchmarks,
            'selectedCategory' => $category,
            'search' => $search
        ]);
    }

    /**
     * Rankings / Leaderboard
     */
    public function rankings(Request $request)
    {
        $mode = $request->query('mode', 'Full Benchmark');
        $category = $request->query('category', 'all');

        $allData = [
            'Full Benchmark' => [
                'all' => [
                    ['rank' => 1, 'chipset' => 'Snapdragon 8 Gen 3', 'gpu_model' => 'Adreno 750', 'score' => 1000, 'grade' => 'A+', 'temp' => '31°C - 38°C'],
                    ['rank' => 2, 'chipset' => 'MediaTek Dimensity 9300', 'gpu_model' => 'Immortalis-G720', 'score' => 965, 'grade' => 'A+', 'temp' => '32°C - 40°C'],
                    ['rank' => 3, 'chipset' => 'Apple A17 Pro', 'gpu_model' => 'Apple GPU 6-core', 'score' => 945, 'grade' => 'A+', 'temp' => '29°C - 37°C'],
                    ['rank' => 4, 'chipset' => 'Snapdragon 8 Gen 2', 'gpu_model' => 'Adreno 740', 'score' => 891, 'grade' => 'A', 'temp' => '29°C - 33°C'],
                    ['rank' => 5, 'chipset' => 'Google Tensor G4', 'gpu_model' => 'Mali-G715 MC10', 'score' => 842, 'grade' => 'A', 'temp' => '32°C - 41°C'],
                    ['rank' => 6, 'chipset' => 'Snapdragon 8+ Gen 1', 'gpu_model' => 'Adreno 730', 'score' => 785, 'grade' => 'B+', 'temp' => '30°C - 33°C'],
                    ['rank' => 7, 'chipset' => 'MediaTek Dimensity 8300', 'gpu_model' => 'Mali-G615 MC6', 'score' => 710, 'grade' => 'B', 'temp' => '31°C - 34°C'],
                    ['rank' => 8, 'chipset' => 'Google Tensor G3', 'gpu_model' => 'Mali-G715 MC9', 'score' => 680, 'grade' => 'B', 'temp' => '32°C - 36°C'],
                    ['rank' => 9, 'chipset' => 'Snapdragon 7+ Gen 2', 'gpu_model' => 'Adreno 725', 'score' => 650, 'grade' => 'B-', 'temp' => '31°C - 35°C'],
                    ['rank' => 10, 'chipset' => 'MediaTek Dimensity 7200 Ultra', 'gpu_model' => 'Mali-G610 MC4', 'score' => 543, 'grade' => 'C', 'temp' => '30°C - 36°C']
                ],
                'cpu' => [
                    ['rank' => 1, 'chipset' => 'Snapdragon 8 Gen 3', 'gpu_model' => 'Adreno 750', 'score' => 313, 'grade' => 'A+', 'temp' => '32°C - 38°C'],
                    ['rank' => 2, 'chipset' => 'Apple A17 Pro', 'gpu_model' => 'Apple GPU 6-core', 'score' => 308, 'grade' => 'A+', 'temp' => '29°C - 37°C'],
                    ['rank' => 3, 'chipset' => 'MediaTek Dimensity 9300', 'gpu_model' => 'Immortalis-G720', 'score' => 295, 'grade' => 'A+', 'temp' => '32°C - 40°C']
                ],
                'gpu' => [
                    ['rank' => 1, 'chipset' => 'Snapdragon 8 Gen 3', 'gpu_model' => 'Adreno 750', 'score' => 100, 'grade' => 'A+', 'temp' => '31°C - 39°C'],
                    ['rank' => 2, 'chipset' => 'MediaTek Dimensity 9300', 'gpu_model' => 'Immortalis-G720', 'score' => 98, 'grade' => 'A+', 'temp' => '32°C - 40°C'],
                    ['rank' => 3, 'chipset' => 'Apple A17 Pro', 'gpu_model' => 'Apple GPU 6-core', 'score' => 95, 'grade' => 'A+', 'temp' => '29°C - 37°C']
                ]
            ],
            'Throttle Test' => [
                'all' => [
                    ['rank' => 1, 'chipset' => 'Snapdragon 8 Gen 3', 'gpu_model' => 'Adreno 750', 'score' => 9540, 'grade' => 'A+', 'temp' => '32°C - 42°C (Stability: 96%)'],
                    ['rank' => 2, 'chipset' => 'Apple A17 Pro', 'gpu_model' => 'Apple GPU 6-core', 'score' => 9120, 'grade' => 'A+', 'temp' => '31°C - 43°C (Stability: 91%)'],
                    ['rank' => 3, 'chipset' => 'MediaTek Dimensity 9300', 'gpu_model' => 'Immortalis-G720', 'score' => 8850, 'grade' => 'A', 'temp' => '30°C - 44°C (Stability: 85%)']
                ]
            ],
            'Efficiency Test' => [
                'all' => [
                    ['rank' => 1, 'chipset' => 'Apple A17 Pro', 'gpu_model' => 'Apple GPU 6-core', 'score' => 12450, 'grade' => 'A+', 'temp' => '28°C - 31°C'],
                    ['rank' => 2, 'chipset' => 'Snapdragon 8 Gen 3', 'gpu_model' => 'Adreno 750', 'score' => 10120, 'grade' => 'A', 'temp' => '29°C - 32°C'],
                    ['rank' => 3, 'chipset' => 'Google Tensor G4', 'gpu_model' => 'Mali-G715 MC10', 'score' => 9840, 'grade' => 'A', 'temp' => '30°C - 33°C']
                ]
            ]
        ];

        // Safe fetch from multidimensional array with fallbacks
        $modesAvailable = array_keys($allData);
        $currentModeData = $allData[$mode] ?? $allData['Full Benchmark'];
        $rankings = $currentModeData[$category] ?? ($currentModeData['all'] ?? []);

        return view('rankings', [
            'rankings' => $rankings,
            'selectedMode' => $mode,
            'selectedCategory' => $category,
            'modes' => $modesAvailable
        ]);
    }

    /**
     * Terms of Conditions
     */
    public function terms()
    {
        return view('terms');
    }

    /**
     * Privacy Policy
     */
    public function privacy()
    {
        return view('privacy');
    }
}
