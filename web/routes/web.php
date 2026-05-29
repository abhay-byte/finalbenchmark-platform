<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\PageController;

Route::get('/', [PageController::class, 'home'])->name('home');
Route::get('/benchmarks', [PageController::class, 'benchmarks'])->name('benchmarks');
Route::get('/rankings', [PageController::class, 'rankings'])->name('rankings');
Route::get('/terms', [PageController::class, 'terms'])->name('terms');
Route::get('/privacy', [PageController::class, 'privacy'])->name('privacy');

