"""
ตัวอย่างการใช้ Anthropic SDK กับ KKU IntelSphere
==================================================
รองรับ feature พิเศษของ Claude: thinking, system prompt แยก
"""

import os
from dotenv import load_dotenv
import anthropic

load_dotenv()

client = anthropic.Anthropic(
    base_url="https://gen.ai.kku.ac.th/api",
    api_key=os.getenv("KKU_API_KEY"),
)

# ── Messages API ──
print("💬 Anthropic Messages:")
response = client.messages.create(
    model="gemini-3.8-flash",
    max_tokens=512,
    stream=False,
    system="คุณคือผู้ช่วยอัจฉริยะของ มข.",
    messages=[
        {"role": "user", "content": "อธิบายว่า RAG คืออะไร สั้นๆ"}
    ],
)
print(response.content[0].text)

# ── ดู quota ──
q = response.model_extra.get("model_quota", {})
print(f"\n📊 Quota: {q.get('daily_remaining_tokens', 'N/A'):,} tokens คงเหลือ")