# Grocery Predictor & Quick-Commerce Auto-Replenishment

A predictive grocery replenishment engine and quick-commerce price aggregator for **Zepto**, **Blinkit**, **Swiggy Instamart**, and **Amazon Fresh**.

---

## 🚀 Instant Deployment to Vercel

This repository includes a ready-to-run web version (`index.html` + `vercel.json`) that deploys directly to Vercel with **zero build steps** and **zero 404 errors**:

1. **Import this repository** in your [Vercel Dashboard](https://vercel.com/new).
2. Leave **Framework Preset** as **Other** (Root Directory `./`, Output Directory `./`).
3. Click **Deploy**.
4. Your web app is live immediately with full ML consumption velocity predictions, smart basket arbitrage, Gemini AI prompts, and local storage persistence.

---

## 📱 Running the Native Android App

This repository also contains the complete native Android Jetpack Compose app:

1. Open the project folder in **Android Studio**.
2. Let Gradle sync dependencies.
3. Select an emulator or connected physical Android device and click **Run** (or execute `gradle assembleDebug` to produce an APK in `app/build/outputs/apk/debug/`).

---

## 🌟 Key Features

- **Bayesian ML Depletion Forecasts**: Real-time stock meters calculating remaining days and hours with urgency badges (🚨 Critical <24h, ⏳ Depleting Soon, ⚠️ Low, ✅ In Stock).
- **Household & Weekend Calibration**: Dynamic multipliers (1 Person, 2 Persons, Family 4+) and weekend breakfast surge (+35% for milk, eggs, bread).
- **Multi-Platform Price & Speed Matching**: Side-by-side live comparison across Zepto (9 mins), Blinkit (11 mins), Swiggy Instamart (14 mins), and Amazon Fresh (Same Day 2h).
- **One-Tap Smart Basket Auto-Fill**: Queues critical pantry shortages in one tap and resets pantry stock levels upon order dispatch.
- **Gemini AI Strategist**: Scenario analysis for brunch planning, store price-splitting, freshness guidelines, and monthly budget optimization.
