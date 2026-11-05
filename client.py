"""
KKU IntelSphere API Client
============================
รองรับทั้ง OpenAI SDK และ Anthropic SDK แบบ drop-in
Base URL: https://gen.ai.kku.ac.th/api/v1
"""

import os
import requests
from dotenv import load_dotenv

load_dotenv()

BASE_URL = os.getenv("KKU_BASE_URL", "https://gen.ai.kku.ac.th/api/v1")
API_KEY = os.getenv("KKU_API_KEY", "")


def _headers():
    return {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json",
    }


# ──────────────────── GET /models ────────────────────

def list_models():
    """ดึงรายการโมเดลทั้งหมด (OpenAI format)"""
    r = requests.get(f"{BASE_URL}/models")
    r.raise_for_status()
    return r.json()


def get_model_ids():
    """ดึงเฉพาะ ID ของทุกโมเดลเป็น list"""
    data = list_models()
    return [m["id"] for m in data["data"]]


# ──────────────────── POST /chat/models-list ────────────────────

def list_models_simple():
    """ดึงรายการโมเดลในรูปแบบง่าย (id + display_name)"""
    r = requests.post(
        f"{BASE_URL}/chat/models-list",
        headers=_headers(),
    )
    r.raise_for_status()
    return r.json()


# ──────────────────── POST /chat/completions ────────────────────

def chat(model, messages, stream=False, temperature=None,
         max_tokens=None, tools=None, tool_choice=None, **kwargs):
    """
    สร้างข้อความจาก AI (OpenAI Chat Completions API compatible)
    
    Parameters:
        model:        string ID เช่น "deepseek-v4-pro" หรือ "gemini-3.5-flash"
        messages:     list of dict เช่น [{"role": "user", "content": "สวัสดี"}]
        stream:       เปิด streaming แบบ SSE
        temperature:  ความสุ่ม 0-2
        max_tokens:   จำกัดจำนวน token
        tools:        function tools สำหรับ tool calling
        tool_choice:  auto, none, หรือระบุชื่อ tool
    """
    body = {"model": model, "messages": messages}
    body["stream"] = bool(stream)
    if temperature is not None:
        body["temperature"] = temperature
    if max_tokens is not None:
        body["max_tokens"] = max_tokens
    if tools is not None:
        body["tools"] = tools
    if tool_choice is not None:
        body["tool_choice"] = tool_choice
    body.update(kwargs)

    r = requests.post(
        f"{BASE_URL}/chat/completions",
        headers=_headers(),
        json=body,
        stream=stream,
    )

    if stream:
        return _iter_stream(r)
    r.raise_for_status()
    return r.json()


def completions(model, prompt, **kwargs):
    """
    Text completions (alias POST /completions)
    ใช้ข้อความเป็น prompt โดยตรง ไม่ใช่ messages format
    """
    body = {"model": model, "prompt": prompt, **kwargs}
    r = requests.post(
        f"{BASE_URL}/completions",
        headers=_headers(),
        json=body,
    )
    r.raise_for_status()
    return r.json()


def _iter_stream(response):
    """Iterator สำหรับ streaming response"""
    response.raise_for_status()
    for line in response.iter_lines():
        if line:
            decoded = line.decode("utf-8")
            if decoded.startswith("data:"):
                data = decoded[5:].strip()
                if data == "[DONE]":
                    break
                yield data


# ──────────────────── POST /messages ────────────────────

def messages(model, messages, system=None, max_tokens=4096,
             temperature=None, top_p=None, top_k=None,
             stop_sequences=None, thinking=None,
             tools=None, tool_choice=None, stream=False, **kwargs):
    """
    Anthropic Messages API — เหมาะกับ Claude, รองรับ thinking
    
    Parameters:
        model:          string ID
        messages:       list of dict (Anthropic format)
        system:         system prompt แยกจาก messages
        max_tokens:     จำกัด output (default 4096)
        temperature:    ความสุ่ม
        top_p / top_k:  sampling parameters
        stop_sequences: หยุดเมื่อพบ pattern
        thinking:       เปิด extended thinking (dict เช่น {"type": "enabled", "budget_tokens": 2000})
        tools:          tools สำหรับ tool use
        tool_choice:    auto / any / tool
        stream:         streaming response
    """
    body = {"model": model, "messages": messages}
    if system:
        body["system"] = system
    if max_tokens is not None:
        body["max_tokens"] = max_tokens
    if temperature is not None:
        body["temperature"] = temperature
    if top_p is not None:
        body["top_p"] = top_p
    if top_k is not None:
        body["top_k"] = top_k
    if stop_sequences is not None:
        body["stop_sequences"] = stop_sequences
    if thinking is not None:
        body["thinking"] = thinking
    if tools is not None:
        body["tools"] = tools
    if tool_choice is not None:
        body["tool_choice"] = tool_choice
    body["stream"] = bool(stream)
    body.update(kwargs)

    r = requests.post(
        f"{BASE_URL}/messages",
        headers=_headers(),
        json=body,
        stream=stream,
    )
    if stream:
        return _iter_stream(r)
    r.raise_for_status()
    return r.json()


# ──────────────────── ฟังก์ชันเสริม ────────────────────

def show_quota(response):
    """แสดง token quota จาก response"""
    q = response.get("model_quota", {})
    if q:
        print(f"  โควต้าวันนี้:   {q.get('daily_quota_tokens', 'N/A'):,} tokens")
        print(f"  ใช้ไปแล้ว:      {q.get('daily_usage_tokens', 'N/A'):,} tokens")
        print(f"  คงเหลือ:        {q.get('daily_remaining_tokens', 'N/A'):,} tokens")


def chat_stream(model, messages, **kwargs):
    """Chat พร้อม streaming แสดงผลทีละ chunk บน console"""
    print("[AI] ", end="", flush=True)
    for data in chat(model, messages, stream=True, **kwargs):
        import json
        chunk = json.loads(data)
        delta = chunk.get("choices", [{}])[0].get("delta", {}).get("content", "")
        if delta:
            print(delta, end="", flush=True)
    print()


# ──────────────────── CLI ────────────────────

if __name__ == "__main__":
    import sys

    # ── แสดงโมเดล (ไม่ต้องใช้ API key) ──
    print("📋 กำลังดึงรายชื่อโมเดล...")
    try:
        data = list_models()
        models = data["data"]
    except Exception as e:
        print(f"❌ ไม่สามารถดึงรายชื่อโมเดลได้: {e}")
        exit(1)

    providers = {}
    for m in models:
        p = m.get("owned_by", "Unknown")
        providers.setdefault(p, []).append(m["id"])

    print(f"\n✅  {len(models)} โมเดล จาก {len(providers)} ผู้ให้บริการ\n")
    for provider, ids in providers.items():
        print(f"  [{provider}]")
        for mid in ids:
            print(f"    • {mid}")

    # ── ตรวจสอบ API key ──
    if not API_KEY:
        print(f"\n{'─'*50}")
        print("⚠️  ยังไม่มี API Key — ต้องมี key ก่อนถึงคุยกับโมเดลได้")
        print("   1. cp .env.example .env")
        print("   2. แก้ .env ใส่ key จาก https://gen.ai.kku.ac.th")
        print(f"{'─'*50}")
        exit(0)

    # ── Non-interactive mode (python client.py model "message") ──
    args = sys.argv[1:]
    if args:
        model = args[0]
        msg = " ".join(args[1:]) if len(args) > 1 else input("ข้อความ: ").strip()

        if not msg:
            print("❌ กรุณาระบุข้อความ")
            exit(1)

        if model not in [m["id"] for m in models]:
            print(f"⚠️  ไม่พบโมเดล '{model}' — โมเดลที่ใช้ได้:")
            for m_id in sorted(m["id"] for m in models):
                print(f"    • {m_id}")
            exit(1)

        print(f"\n🤖 [{model}]\n{'─'*50}")
        try:
            result = chat(model, [{"role": "user", "content": msg}])
            print(result["choices"][0]["message"]["content"])
            print("─" * 50)
            show_quota(result)
        except Exception as e:
            print(f"❌ Error: {e}")
        exit(0)

    # ── Interactive chat ──
    print("\n💬 พิมพ์ 'exit' เพื่อออก\n")

    model = input("เลือกโมเดล: ").strip()
    if model not in [m["id"] for m in models]:
        print(f"⚠️  ไม่พบโมเดล '{model}' — จะลองใช้ต่อไป")

    print(f"✅ ใช้โมเดล: {model}")
    print("─" * 50)

    while True:
        msg = input("\nคุณ: ").strip()
        if msg.lower() == "exit":
            print("👋 บาย!")
            break
        try:
            result = chat(model, [{"role": "user", "content": msg}])
            print(f"🤖: {result['choices'][0]['message']['content']}")
            show_quota(result)
        except Exception as e:
            print(f"❌ Error: {e}")