import subprocess
from PIL import Image, ImageDraw

TEMP_DEVICE_PATH = "/sdcard/screen.png"
LOCAL_PATH = "screenshots/watchface_raw.png"
OUTPUT_PATH = "screenshots/watchface_512_round.png"
SIZE = (512, 512)


def adb_screencap():
    print("📸 Taking screenshot via ADB...")
    subprocess.run(["adb", "shell", f"screencap -p {TEMP_DEVICE_PATH}"], check=True)
    subprocess.run(["adb", "pull", TEMP_DEVICE_PATH, LOCAL_PATH], check=True)
    print("✅ Screenshot pulled:", LOCAL_PATH)


def process_image():
    print("🎨 Processing image...")
    img = Image.open(LOCAL_PATH).convert("RGBA")
    img = img.resize(SIZE, Image.LANCZOS)

    # Центрированный круглый маск
    mask = Image.new("L", SIZE, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse([(0, 0), SIZE], fill=255)

    # Применяем маску
    img.putalpha(mask)

    img.save(OUTPUT_PATH, format="PNG")
    print("✅ Saved cleaned image:", OUTPUT_PATH)


def main():
    adb_screencap()
    process_image()


if __name__ == "__main__":
    main()
