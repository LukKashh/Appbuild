# Capital Fortune — native Android implementation reference

This branch expands the Android game to match the breadth of the webapp while preserving the confirmed mobile layout and local-first game direction. This is a native Jetpack Compose implementation; it does not embed the webapp or require Firebase.

## Canonical mobile shell

- Portrait-first Android layout with a mobile-safe five-item bottom navigation: Assets, Markets, Tap, Empire, Corp.
- Neon purple / midnight surfaces, gold interaction accents, positive/negative financial states, compact charts and dense but touch-friendly cards.
- Empire retains Business, Real Estate, Research, Collectibles, Casino, Achievements and Statistics as nested destinations.
- Profile and Settings are editable local screens, not placeholders.

## Expanded gameplay and data

- Catalogue: expanded company and property portfolios, additional stock and cryptocurrency instruments, 300 collectible catalogue entries and an expanded research tree.
- Business operations: branches, staffing, multilevel upgrades, operating costs, reserves, liabilities, financial history, suspension and paid reactivation. Suspended businesses stop producing, retain the model's reduced idle schedule (15% of fixed operating costs and 50% of employee salaries), and do not generate research points.
- Property management: condition, occupancy, demand, operating expenses, maintenance arrears, valuation and multilevel amenities.
- Exchange: simulated time-based stock and crypto prices, cost basis, daily stock dividends, 5% annualized crypto staking yield when unlocked, trading fees, queued limit/stop/take-profit orders, reserved funds and holdings, partial fills, cancellation and expiry.
- Research: R&D laboratory construction, research-point generation, parallel project slots, prerequisite gating, timed completion and permanent effects.
- Corporation: local founding, treasury contributions, simulated local operators, objectives, management shifts, facilities, departments and subsidiaries.
- Collectibles: rarity, limited editions, price variation, collection bonuses, holding periods and rarity-based resale percentages.
- Casino: roulette, blackjack, mines, keno and hi-lo, with chip exchange, wagering history, net results and win/loss streaks.
- Progression: player XP and levels, tap yield bonuses, lifetime earnings and click statistics.
- Finance: consolidated asset values, cash-flow statement, transaction audit trail and debit/credit ledger.

## Persistence and networking

- Game state is stored locally using Room. Existing SharedPreferences JSON saves are imported on first load.
- The economic simulation, market prices, orders, research timers, progression and corporation simulation run on-device. No cloud account or remote game server is required.
- Corporation members are labelled local simulated operators. Cross-device multiplayer, real connected player membership, shared cloud saves, online leaderboards and server-authoritative play are not implemented in this offline-first build.
- Generated visual templates guide the Compose layout; the implementation uses native UI components rather than treating screenshots as interactive screens. The supplied artwork ZIP is packaged as an Android asset and matched to collectible, business, property, stock and crypto catalogue cards by filename. Images load off the UI thread, are downsampled for mobile display and use a bounded bitmap cache; unmatched catalogue entries retain native icon placeholders.

## Verification

- The APK workflow runs JVM unit tests before assembling the debug APK.
- Simulation regression tests cover suspended business costs and research generation, annualized staking accrual, and dividend posting.
- Physical-device interaction and visual QA still need to be performed.
