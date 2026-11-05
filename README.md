# ร้านลูกชิ้นนายเอ็ด — ระบบสั่งอาหาร ร้านค้า และวิเคราะห์ต้นทุน

แอปพลิเคชัน Android (Jetpack Compose) สำหรับร้านลูกชิ้น ครบทั้งฝั่งลูกค้าและฝั่งร้านค้า
พัฒนาจากข้อกำหนดระบบใน [`docs/requirements.md`](docs/requirements.md) และต้นแบบหน้าเว็บใน [`docs/prototype/`](docs/prototype/)

> แอปนี้เป็น **standalone app** เก็บข้อมูลไว้ในเครื่อง การซิงก์ข้ามเครื่องผ่าน Firebase ยังไม่ได้เชื่อมต่อ (ดูหัวข้อข้อจำกัด)

---

## ฟีเจอร์หลัก

### ฝั่งลูกค้า

- เลือกหมวดหมู่เมนู ดูเมนูขายดี และเพิ่ม/ลดสินค้าในตะกร้า
- สั่งแบบ **Guest** (ไม่ใส่เบอร์โทร) หรือสั่งแบบ **สมาชิก** ด้วยเบอร์โทรศัพท์
- สะสมแต้ม แลกรางวัล ดูประวัติการสั่งซื้อ และ **สั่งซ้ำเมนูเดิม** ได้ในคลิกเดียว
- เลือกรับสินค้าแบบ **จัดส่ง** หรือ **มารับเองหน้าร้าน**
  - จัดส่ง: กรอกที่อยู่และกดแนบพิกัด GPS จริงจากอุปกรณ์
  - รับเอง: แสดงปุ่มนำทางไปยังร้าน
- ตัวเลือกเสริมก่อนยืนยัน: เสียบไม้ / ใส่ถุง / รับไม้เสียบแยก, พริกทอด, แตงกวา, น้ำจิ้ม และช่องหมายเหตุถึงร้าน
- คำนวณส่วนลดและค่าส่งอัตโนมัติตามเงื่อนไขที่ตั้งไว้ในหลังร้าน
- สร้าง **PromptPay QR แบบ EMVCo** ตามยอดสุทธิ พร้อมแนบสลิปจากไฟล์ในเครื่อง
- ติดตามสถานะออเดอร์ `รอดำเนินการ → กำลังทำ → เสร็จแล้ว` พร้อมเสียงแจ้งเตือน

### ฝั่งร้านค้า (Admin)

- เข้าสู่โหมดร้านค้าด้วย PIN 4 หลัก (ค่าเริ่มต้น `1234`)
- จัดการออเดอร์: กรองตามสถานะ เปลี่ยนสถานะ และพิมพ์ใบสรุปออเดอร์ผ่านระบบพิมพ์ของ Android
- POS หน้าร้าน: ค้นหา/สร้างสมาชิกด้วยเบอร์โทร เพิ่มแต้ม และแลกรางวัล
- จัดการสินค้าและสต็อก:
  - ตั้งสถานะ `ขายปกติ / สินค้าหมด / ซ่อนเมนู`
  - ตัดสต็อกอัตโนมัติเมื่อมีคำสั่งซื้อ
  - รีเซ็ตสต็อกประจำวันได้ในคลิกเดียว
- ตั้งค่าร้าน: ชื่อ สโลแกน ที่อยู่ โลโก้/อีโมจิ หมายเลข PromptPay ค่าส่ง ส่วนลด เงื่อนไขส่งฟรี แต้มที่ใช้แลกรางวัล และ PIN
- วิเคราะห์ต้นทุน:
  - บันทึกต้นทุนวัตถุดิบตรงต่อหน่วย
  - บันทึกต้นทุนแฝง (แก๊ส ถุง พริก ค่าน้ำ/ไฟ ฯลฯ)
  - คำนวณจุดคุ้มทุนรายเดือน แสดงความคืบหน้า ประมาณการแก๊สคงเหลือ และคำแนะนำอัตโนมัติ

---

## เทคโนโลยี

- Kotlin + Jetpack Compose (Material 3)
- MVVM + `StateFlow`
- Moshi + SharedPreferences สำหรับ persistence ภายในเครื่อง
- Firebase Realtime Database สำหรับซิงก์ข้ามอุปกรณ์ (project `dinoeater-merchant`)
- ZXing สำหรับสร้าง QR Code
- Android `PrintHelper` สำหรับพิมพ์ใบสรุป
- `ActivityResultContracts.OpenDocument()` สำหรับแนบสลิป
- `LocationManager` สำหรับพิกัด GPS
- AGP 9.1.1 / Gradle 9.6.0 / Kotlin 2.2.10 / minSdk 24 / targetSdk 36

---

## สิ่งที่ต้องมีก่อน build

1. **JDK 17 ขึ้นไป** (แนะนำ JBR ที่มากับ Android Studio)
2. **Android SDK** (compileSdk 36)
3. ไฟล์ `local.properties` ที่ชี้ไปยัง SDK เช่น

   ```properties
   sdk.dir=C:/Users/<ชื่อผู้ใช้>/AppData/Local/Android/Sdk
   ```

4. ไฟล์ `debug.keystore` ที่ root โปรเจกต์ (ไฟล์นี้ถูก ignore ไม่ได้ commit ไว้)
   สร้างใหม่ได้ด้วยคำสั่ง:

   ```bash
   keytool -genkeypair -v -keystore debug.keystore \
     -storepass android -alias androiddebugkey -keypass android \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -dname "CN=Android Debug,O=Android,C=US"
   ```

### ตัวแปรสภาพแวดล้อมสำหรับ release build (ไม่บังคับสำหรับ debug)

```bash
KEYSTORE_PATH=/path/to/my-upload-key.jks
STORE_PASSWORD=...
KEY_PASSWORD=...
```

---

## วิธี build และรัน

```bash
# Windows
.\gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

ติดตั้งลงอุปกรณ์/อีมูเลเตอร์:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

เปิดแอปครั้งแรกจะเห็นหน้าลูกค้า กดปุ่มเข้าสู่โหมดร้านค้าแล้วใส่ PIN `1234`

### รัน tests

```bash
.\gradlew.bat :app:testDebugUnitTest
```

ชุดทดสอบครอบคลุม PromptPay payload/CRC, การคำนวณ QR, persistence และ Robolectric smoke test

---

## การตั้งค่าเริ่มต้น

| รายการ | ค่าเริ่มต้น |
| --- | --- |
| PIN เข้าหลังร้าน | `1234` |
| หมายเลข PromptPay | `081-234-5678` |
| ส่วนลด | ครบ 50 บาท ลด 5 บาท |
| ส่งฟรี | ครบ 120 บาท |
| ค่าส่ง | 15 บาท |
| แต้มแลกรางวัล | 10 แต้ม |

> **สำคัญ:** หมายเลข PromptPay ชื่อบัญชี และข้อมูลร้านเป็นข้อมูลตัวอย่าง
> ต้องแก้ในหน้า “ตั้งค่าร้าน” ก่อนใช้งานจริง มิฉะนั้น QR จะชี้ไปยังบัญชีตัวอย่าง
> และควรเปลี่ยน PIN จาก `1234` ทันที

ข้อมูลตัวอย่าง (เมนู สมาชิก และออเดอร์ตัวอย่าง) จะถูก seed เมื่อเปิดแอปครั้งแรก

---

## การเก็บข้อมูล

- ข้อมูลทั้งหมดถูกเก็บใน `SharedPreferences` ชื่อ `meatball_shop_state` (JSON ผ่าน Moshi)
- ข้อมูลร้าน เมนู สต็อก ออเดอร์ สมาชิก และต้นทุนแฝงจะ **ซิงก์ข้ามอุปกรณ์ผ่าน Firebase Realtime Database** โดยอัตโนมัติ
  - Firebase project: `dinoeater-merchant`
  - Database URL: `https://dinoeater-merchant-default-rtdb.asia-southeast1.firebasedatabase.app`
  - Path: `shops/dinoeater-merchant/state`
- การล้างข้อมูลแอป (Clear storage) หรือถอนการติดตั้งจะลบข้อมูลในเครื่อง แต่ข้อมูลบน Firebase ยังอยู่ และจะถูกดึงกลับมาเมื่อเปิดแอปใหม่
- URI ของสลิปที่แนบจะขอสิทธิ์อ่านแบบถาวรผ่าน `takePersistableUriPermission` (เป็นไฟล์ในเครื่อง ไม่ได้ซิงก์ข้ามอุปกรณ์)
- ข้อมูลที่อยู่เฉพาะเครื่อง: PIN หลังร้าน, เบอร์โทรลูกค้าปัจจุบัน, ออเดอร์ที่กำลังติดตาม, รูปโลโก้/แสตมป์แบบอัปโหลด และ URI สลิป

---

## โครงสร้างโปรเจกต์

```text
app/src/main/java/com/example/
├── MainActivity.kt
├── data/
│   ├── model/ShopModels.kt          # โมเดลร้าน เมนู ออเดอร์ สมาชิก ต้นทุน
│   └── repository/MeatballRepository.kt  # seed, persistence, mutations, break-even
├── ui/
│   ├── MainViewModel.kt             # state, ตะกร้า, ออเดอร์, PIN, สลิป, GPS
│   ├── customer/                    # หน้าลูกค้า ตะกร้า PromptPay ติดตามออเดอร์
│   ├── admin/                       # ออเดอร์ POS สต็อก วิเคราะห์ ตั้งค่า พิมพ์
│   ├── components/                  # คอมโพเนนต์แบรนด์ร้าน
│   └── theme/
└── util/                            # PromptPay, GPS, เสียง, ข้อความใบสรุป
docs/
├── requirements.md                  # ข้อกำหนดระบบต้นฉบับ
└── prototype/                       # ต้นแบบ HTML (index.html, admin.html)
```

---

## ข้อจำกัดที่ทราบ

- **ซิงก์ผ่าน Firebase Realtime Database:** ข้อมูลร้าน/เมนู/ออเดอร์/สมาชิก/ต้นทุนแฝง แชร์ข้ามอุปกรณ์แบบ real-time ด้วย last-write-wins
- **กฎความปลอดภัยปัจจุบันเปิดกว้าง (demo):** `database.rules.json` อนุญาต read/write ทุกคน — ต้องปรับเป็น authenticated rules ก่อนใช้งานจริง
- **ไม่มีการยืนยันตัวตน:** ยังไม่ได้เปิด Firebase Authentication; ทุก client สามารถเขียนข้อมูลได้
- **Real-time ข้ามเครื่อง:** สถานะออเดอร์จะอัปเดตบนอุปกรณ์ทุกเครื่องที่ใช้โปรเจกต์เดียวกัน
- **สลิปไม่ถูกตรวจสอบอัตโนมัติ:** ระบบเพียงแนบไฟล์และบันทึกว่ามีสลิป ร้านต้องตรวจสอบยอดเงินเอง
- **PromptPay QR** สร้างตามหมายเลขที่ตั้งไว้ แต่ไม่มีการยืนยันการชำระเงินกับธนาคาร
- `applicationId` ยังเป็น `com.aistudio.applet.csmhfz` จากโปรเจกต์ต้นทาง ควรเปลี่ยนก่อนเผยแพร่บน Play Store
- ข้อมูล analytics บางส่วน (เช่น ประมาณการแก๊สคงเหลือ) เป็นการประมาณจากจำนวนออเดอร์ ไม่ได้วัดจากเซ็นเซอร์จริง

---

## เอกสารเพิ่มเติม

- [ข้อกำหนดระบบฉบับเต็ม](docs/requirements.md)
- [ต้นแบบหน้าลูกค้า](docs/prototype/index.html)
- [ต้นแบบหน้าหลังร้าน](docs/prototype/admin.html)
