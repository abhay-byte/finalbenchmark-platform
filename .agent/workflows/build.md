---
description: 
---



# 🚀 Build & Deploy Workflow

### **Prereqs**

* Android Studio OR JDK 17+
* Android SDK installed
* Android NDK (Native Development Kit) installed
* Rust toolchain with cargo-ndk installed
* Device USB debugging ON (or ADB wireless enabled)

---

### **2️⃣ Sync Dependencies**

```sh
./gradlew clean
./gradlew dependencies
```

---

### **3️⃣ Build APK**

```sh
./gradlew assembleDebug --info
```

APK output should appear here:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

### **4️⃣ Connect Device**

```sh
adb devices
```

If empty → enable USB debugging or pair wireless:

```sh
adb pair <ip>:<port>
adb connect <ip>:<port>
```

---

### **5️⃣ Install APK**

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`-r` = replace existing install.

---

### **6️⃣ Launch App**

```sh
PACKAGE=$(grep "package=" app/src/main/AndroidManifest.xml | cut -d'"' -f2)
adb shell monkey -p $PACKAGE -c android.intent.category.LAUNCHER 1
```

---

### **7️⃣ Monitor Logs**

```sh
adb logcat '*:E'
```

---

If build fails → run:

```sh
./gradlew build --stacktrace
```

Use output to fix errors, then repeat from **Step 3**.
