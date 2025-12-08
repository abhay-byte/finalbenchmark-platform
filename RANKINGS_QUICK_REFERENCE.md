# Rankings Feature - Quick Reference Guide

## 📋 What Was Implemented

Three new files created + one existing file updated:

| File | Type | Purpose |
|------|------|---------|
| `RankingViewModel.kt` | New ViewModel | Business logic for ranking data |
| `RankingsScreen.kt` | New UI Screen | Complete Rankings UI |
| `MainNavigation.kt` | Updated | Added navigation route & bottom bar item |

---

## 🎯 Feature Highlights

### ✅ Bottom Navigation
- **New Button:** "Rankings" with Leaderboard icon
- **Position:** Between "Device" and "History"
- **Route:** `"rankings"`

### ✅ Rankings Display (CPU Category)
- **Data Source:** Hardcoded reference devices + user's best score
- **Hardcoded Devices:** 7 snapdragon/dimensity chipsets
- **User Device:** Auto-inserted from HistoryRepository
- **Sorting:** By normalized score (descending)
- **Ranking:** Numbers assigned after sort (1, 2, 3...)

### ✅ UI Components
| Component | Description |
|-----------|-------------|
| Filter Bar | LazyRow with 7 category chips (CPU selected by default) |
| Ranking Card | Rank badge + device name + score bar + normalized score |
| Medal Colors | Gold (#1), Silver (#2), Bronze (#3) |
| User Highlight | Subtle border + tint on user's device |
| Progress Bar | Visual representation relative to top score (1200) |
| Coming Soon | Placeholder for GPU/RAM/Storage/etc. |

### ✅ States
- **Loading:** Shows spinner while fetching data
- **Success:** Displays ranked list
- **Error:** Shows error message
- **Coming Soon:** For non-CPU categories

---

## 📊 Data Structure

### RankingItem
```kotlin
data class RankingItem(
    val rank: Int = 0,           // Assigned dynamically
    val name: String,             // "Snapdragon 8 Elite" or "Your Device (Model)"
    val normalizedScore: Int,     // 200-1200
    val singleCore: Int,          // 900-2850 (proportional)
    val multiCore: Int,           // 3000-10200 (proportional)
    val isCurrentUser: Boolean    // true for user's device
)
```

### Hardcoded Reference Data
```
Snapdragon 8 Elite      → 1200
Snapdragon 8 Gen 3      → 900
Snapdragon 8s Gen 3     → 750
Snapdragon 7+ Gen 3     → 720
Dimensity 8300          → 650
Helio G95               → 250
Snapdragon 845          → 200
```

---

## 🔄 Data Flow Example

### Scenario: User's device scores 700 on CPU benchmark

1. **User runs CPU benchmark** → Score stored in DB as 700 (normalizedScore)
2. **User navigates to Rankings** → RankingsScreen loads
3. **ViewModel initializes** → RankingViewModelFactory injects HistoryRepository
4. **loadRankings() executes:**
   - Queries DB for CPU benchmarks
   - Finds highest score: 700
   - Creates RankingItem: "Your Device (Pixel 8)" with 700
5. **Merge & Sort:**
   - Adds to list: [1200, 900, 750, 720, 700, 650, 250, 200]
   - After sort (DESC): [1200, 900, 750, 720, 700, 650, 250, 200]
6. **Assign Ranks:**
   - Your Device (700) → Rank #5
7. **UI Renders:**
   - Card with #5 badge, "Your Device (Pixel 8)", progress bar at 58%, score 700

---

## 🎨 Visual Design

### Colors (Dark Theme)
```
Rank Badge Background: rankColor.copy(alpha = 0.2f)
Card Background: MaterialTheme.colorScheme.surfaceVariant
User Device Tint: primaryContainer.copy(alpha = 0.1f)
User Device Border: primary.copy(alpha = 0.3f)
Progress Bar: GruvboxDarkAccent (#FE8019)
Rank #1-3: Gold/Silver/Bronze
```

### Spacing
```
Card Padding:        12.dp
Item Spacing:        10.dp
Horizontal Padding:  12.dp
Filter Spacing:      8.dp
Progress Bar Height: 4.dp
```

---

## 🚀 Usage

### For Users
1. Open app → Navigate to bottom bar
2. Tap **Rankings** button (Leaderboard icon)
3. See CPU rankings with your device highlighted
4. Tap other category chips to see "Coming Soon"

### For Developers
```kotlin
// Access from any composable
val viewModel: RankingViewModel = viewModel(
    factory = RankingViewModelFactory(historyRepository)
)

// Collect state
val screenState by viewModel.screenState.collectAsState()

// Select category
viewModel.selectCategory("CPU")
```

---

## 📁 File Locations

```
app/src/main/java/com/ivarna/finalbenchmark2/
├── ui/viewmodels/
│   └── RankingViewModel.kt           ← NEW
├── ui/screens/
│   └── RankingsScreen.kt             ← NEW
└── navigation/
    └── MainNavigation.kt             ← UPDATED
```

---

## ✨ Key Features

✅ **Auto-Ranking**
- Highest user score automatically finds correct position
- Example: 700 score ranks between 8s Gen 3 (750) and Dimensity (650)

✅ **Visual Indicators**
- Gold/Silver/Bronze medals for top 3
- Progress bar shows relative score performance
- User device has distinct styling (border + tint)

✅ **Dark Theme Ready**
- Uses MaterialTheme.colorScheme throughout
- Works with Gruvbox, Nord, Dracula, Solarized themes
- Proper alpha values for visual hierarchy

✅ **State Management**
- Loading state during data fetch
- Error handling
- Coming Soon placeholder for future categories
- Reactive updates via StateFlow

✅ **Performance**
- LazyColumn for efficient rendering
- LazyRow for filter chips
- ViewModel scope prevents memory leaks
- Efficient database queries (maxByOrNull)

---

## 🧪 Testing Checklist

- [ ] Tap Rankings button from Home
- [ ] Verify CPU category shows 7 hardcoded devices
- [ ] Run a CPU benchmark, check if user device appears
- [ ] Verify user device has correct rank and styling
- [ ] Tap GPU/RAM/Storage categories, verify "Coming Soon"
- [ ] Verify progress bars display correctly
- [ ] Verify medals (gold/silver/bronze) for top 3
- [ ] Check loading spinner appears briefly
- [ ] Navigate away and back, verify data persists
- [ ] Test on different theme modes

---

## 🔮 Future Enhancements

1. **GPU Rankings** - Same pattern as CPU
2. **Filters** - Date range, device type
3. **Details Screen** - Tap ranking to see full specs
4. **Export** - Share as image or CSV
5. **Trends** - Score history over time
6. **Global Leaderboard** - Connect to backend API

---

## 📝 Notes

- **No breaking changes:** Existing functionality untouched
- **Database:** Uses existing BenchmarkDao, no schema changes
- **Dependencies:** No new dependencies added
- **Kotlin:** 100% Kotlin, follows project patterns
- **Compose:** Uses Material3 design system
- **Coroutines:** Proper scoping with viewModelScope

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| "Coming Soon" shows for all categories | Feature only implemented for CPU category (by design) |
| User device not appearing | Ensure CPU benchmark was run and score saved in DB |
| Progress bar doesn't show | Check if normalizedScore is between 0-1200 |
| Medal colors look wrong | Verify dark theme is active in app |
| Rankings not updating | Pull to refresh / restart app |

---

**Status:** ✅ Complete & Ready for Testing
**Last Updated:** December 8, 2025
