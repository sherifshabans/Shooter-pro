# ShooterPro

لعبة إطلاق نار ثنائية الأبعاد (top-down) على Android، مكتوبة بـ Kotlin مع libGDX.

---

## ما هي اللعبة؟

لعبة شوتر من منظور علوي. تتحكم في شخصية دائرية وتقاتل موجات من الأعداء. الهدف إنهاء الموجات الخمس والوصول للبوص.

**الرسومات:** مرسومة بالكامل بكود (دوائر، مستطيلات، خطوط) عبر libGDX ShapeRenderer — لا توجد صور.

**الأصوات:** مولَّدة حسابياً من معادلات رياضية (PCM) — لا توجد ملفات صوتية.

---

## المتطلبات

- Android Studio Hedgehog أو أحدث
- Android SDK 24+
- Kotlin 1.9+

---

## تشغيل المشروع

```bash
# 1. افتح المجلد في Android Studio
File → Open → اختر مجلد ShooterPro

# 2. انتظر Gradle Sync

# 3. شغّل على جهاز أو محاكي
Run → Run 'app'
```

> **ملاحظة:** لازم تضغط "Trust Project" عند الفتح أول مرة.

---

## بنية المشروع

```
app/src/main/
├── java/com/rafeeqi/kids/shooter/
│   ├── MainActivity.kt       ← الشاشة الرئيسية (زر اللعب)
│   ├── ShooterActivity.kt    ← نقطة دخول libGDX
│   ├── ShooterGame.kt        ← إدارة الشاشات + حفظ البيانات
│   ├── IntroScreen.kt        ← قائمة اللعبة (لعب / ملف / سجل)
│   ├── ArenaScreen.kt        ← ساحة القتال الرئيسية
│   ├── SoundManager.kt       ← مولّد الأصوات
│   └── ArabicText.kt         ← عرض النص العربي
└── res/
    ├── layout/activity_main.xml
    └── values/
```

---

## التحكم في اللعبة

| الإجراء | الطريقة |
|---------|---------|
| الحركة | جويستيك ديناميكي (يسار الشاشة) |
| الإطلاق | الضغط في يمين الشاشة |
| Dash | زر في أسفل اليسار |
| تغيير السلاح | زر في أعلى اليمين |
| قنبلة | زر في أسفل اليمين |

---

## عناصر اللعبة

**الأسلحة:** مسدس (لانهائي) · شوتغان · مينيغان · مشرط  
**الأعداء:** Grunt · Heavy · Sniper · Flanker · Boss  
**البيك آبات:** صحة · درع · ذخيرة · قنابل  
**الموجات:** 5 موجات، تزداد صعوبة مع كل موجة  

---

## مشكلة libGDX مع العربية

libGDX لا يدعم النص العربي مباشرة (يظهر كرموز). الحل في `ArabicText.kt`:

```kotlin
// رسم النص على Android Bitmap
val paint = Paint().apply { textSize = size }
Canvas(bitmap).drawText(text, x, y, paint)

// تحويل الـ Bitmap لـ libGDX Texture
val texture = Texture(pixmapFromBitmap(bitmap))
```

---

## الـ Dependencies

```kotlin
// build.gradle.kts
val gdxVersion = "1.12.1"
implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
// + natives لكل architecture
```

---

## الرخصة

MIT — استخدام حر مع ذكر المصدر.
