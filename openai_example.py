"""
ตัวอย่างการใช้ OpenAI SDK กับ KKU IntelSphere
===============================================
แค่เปลี่ยน base_url และ api_key — โค้ดเดิมใช้ได้ทันที
"""

import os
from dotenv import load_dotenv
from openai import OpenAI

load_dotenv()

client = OpenAI(
    base_url="https://gen.ai.kku.ac.th/api/v1",
    api_key=os.getenv("KKU_API_KEY"),
)

# ── List models ──
print("📋 โมเดลทั้งหมด:")
models = client.models.list()
for m in models.data:
    print(f"  • {m.id} ({m.owned_by})")

# ── Chat (ไม่ streaming) ──
print("\n💬 Chat:")
response = client.chat.completions.create(
    model="gemini-3.8-flash",
    messages=[
        {"role": "system", "content": "คุณคือผู้ช่วยอัจฉริยะของ มข."},
        {"role": "user", "content": "สวัสดีครับ"},
    ],
    temperature=0.7,
    max_tokens=512,
)
print(response.choices[0].message.content)

# ── ดู quota ──
q = response.model_extra.get("model_quota", {})
print(f"\n📊 Quota: {q.get('daily_remaining_tokens', 'N/A'):,} tokens คงเหลือ")

# ── Chat (streaming) ──
print("\n💬 Streaming:")
stream = client.chat.completions.create(
    model="deepseek-v4.1-flash-maas",
    messages=[{"role": "user", "content": "เล่านิทานสั้นๆ 1 ย่อหน้า"}],
    stream=True,
)
for chunk in stream:
    delta = chunk.choices[0].delta.content or ""
    print(delta, end="", flush=True)
print()