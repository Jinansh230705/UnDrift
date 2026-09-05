import urllib.request
import json

url = "https://undriftapis.jinansh.workers.dev/v1/chat/completions"
data = json.dumps({
    "messages": [{"role": "user", "content": "Write a short paragraph about the ocean."}],
    "stream": True
}).encode('utf-8')

req = urllib.request.Request(url, data=data, headers={
    'Content-Type': 'application/json',
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
})

try:
    with urllib.request.urlopen(req) as response:
        print("Response status:", response.status)
        for line in response:
            line = line.decode('utf-8').strip()
            if line:
                print("Received chunk:", line)
except urllib.error.URLError as e:
    print(f"Failed to reach API: {e}")
