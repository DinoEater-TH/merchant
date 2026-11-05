# merchant

เชื่อมต่อ AI ทุกโมเดลจาก **KKU IntelSphere** ผ่าน API เดียว

🔗 Base URL: `https://gen.ai.kku.ac.th/api/v1`

## 🚀 เริ่มต้นใช้งาน

```bash
git clone https://github.com/DinoEater-TH/merchant.git
cd merchant
pip install -r requirements.txt
cp .env.example .env
# แก้ไข .env ใส่ KKU_API_KEY ของคุณ → https://gen.ai.kku.ac.th
python client.py
```

## 📡 API Endpoints

| Method | Endpoint | คำอธิบาย |
|---|---|---|
| `GET` | `/models` | รายการโมเดลทั้งหมด (OpenAI format) |
| `POST` | `/chat/models-list` | รายการโมเดลแบบง่าย (id + name) |
| `POST` | `/chat/completions` | Chat Completions API (OpenAI compatible) |
| `POST` | `/messages` | Messages API (Anthropic compatible) |

### Authentication
```
Authorization: Bearer YOUR_API_KEY
x-api-key: YOUR_API_KEY
```

## 🤖 10 โมเดลที่ให้บริการ

| Provider | Models |
|---|---|
| **Deepseek** | deepseek-v4.1-flash-maas |
| **Gemini** | gemini-4-argon, gemini-3.8-flash |
| **KKU Local LLM** | gemma4-e4b, gemma4-12b, gemma4-26b, qwen3.8-27b, medgemma1.5-4b |
| **xAI** | grok-4.7 |
| **Z.ai** | glm-5.2-maas |

## 📝 วิธีใช้

### ผ่าน client.py (auto-detect)
```python
from client import chat, messages, list_models, show_quota

models = list_models()
result = chat("gemini-3.8-flash", [
    {"role": "user", "content": "สวัสดี"}
])
print(result["choices"][0]["message"]["content"])
show_quota(result)
```

### ผ่าน OpenAI SDK (drop-in)
```python
from openai import OpenAI

client = OpenAI(
    base_url="https://gen.ai.kku.ac.th/api/v1",
    api_key="YOUR_API_KEY",
)

response = client.chat.completions.create(
    model="gemini-3.8-flash",
    messages=[{"role": "user", "content": "Hello"}],
    stream=True,  # รองรับ streaming
)
```

### ผ่าน Anthropic SDK
```python
import anthropic

client = anthropic.Anthropic(
    base_url="https://gen.ai.kku.ac.th/api",
    api_key="YOUR_API_KEY",
)

response = client.messages.create(
    model="gemini-3.8-flash",
    max_tokens=1024,
    stream=False,
    system="คุณคือผู้ช่วยอัจฉริยะ",
    messages=[{"role": "user", "content": "อธิบาย RAG"}],
)
```

## ⚙️ Features
- ✅ Streaming (SSE)
- ✅ Tool calling / Function calling
- ✅ Extended thinking (Claude)
- ✅ Token quota ติดตามการใช้งานเรียลไทม์
- ✅ OpenAI SDK & Anthropic SDK compatible