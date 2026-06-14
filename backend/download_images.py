import os
import urllib.request
import concurrent.futures

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
STATIC_DIR = os.path.join(BASE_DIR, "static")
SERVICE_DIR = os.path.join(STATIC_DIR, "services")
AVATAR_DIR = os.path.join(STATIC_DIR, "avatars")

os.makedirs(SERVICE_DIR, exist_ok=True)
os.makedirs(AVATAR_DIR, exist_ok=True)

def download_image(url, filepath):
    if os.path.exists(filepath):
        # print(f"Already exists: {filepath}")
        return filepath
    print(f"Downloading {filepath}...")
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)'})
        with urllib.request.urlopen(req, timeout=15) as response, open(filepath, 'wb') as out_file:
            out_file.write(response.read())
    except Exception as e:
        print(f"Failed to download {filepath}: {e}")
    return filepath

tasks = []
with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
    print("Submitting 500 service images for download...")
    for i in range(1, 501):
        filepath = os.path.join(SERVICE_DIR, f"sanya_service_{i:03d}.jpg")
        url = f"https://picsum.photos/seed/sanya_svc_v2_{i}/720/960"
        tasks.append(executor.submit(download_image, url, filepath))
        
    print("Submitting 20 avatar images for download...")
    for i in range(1, 21):
        filepath = os.path.join(AVATAR_DIR, f"avatar_{i:02d}.jpg")
        url = f"https://picsum.photos/seed/sanya_avatar_v2_{i}/400/400"
        tasks.append(executor.submit(download_image, url, filepath))
        
    concurrent.futures.wait(tasks)

print("Done downloading 100 service images and 20 avatars!")