import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

c_match = re.search(r'versionCode\s*=\s*(\d+)', content)
n_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)

print(f"c_match: {c_match.groups() if c_match else None}")
print(f"n_match: {n_match.groups() if n_match else None}")
