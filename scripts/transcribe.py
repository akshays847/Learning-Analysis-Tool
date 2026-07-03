#!/usr/bin/env python3
"""
Meet recording -> transcript, for the Learning Analysis Tool.

Turns a recorded Google Meet's audio/video into a .txt transcript that you
then upload as the "lesson" input (when there's no teacher Word doc).

Usage:
    pip install openai-whisper
    python scripts/transcribe.py recording.mp4 lesson.txt

Notes:
- "base" model is fast and fine for clear audio. Use "small"/"medium" for
  better accuracy on technical vocabulary (slower).
- whisper handles mp4/mp3/wav/m4a directly (ffmpeg must be installed).
"""
import sys

def main():
    if len(sys.argv) < 3:
        print("Usage: python transcribe.py <recording.mp4> <out.txt>")
        sys.exit(1)
    audio_path, out_path = sys.argv[1], sys.argv[2]

    import whisper
    model = whisper.load_model("base")   # swap to "small"/"medium" for accuracy
    result = model.transcribe(audio_path)

    with open(out_path, "w", encoding="utf-8") as f:
        f.write(result["text"].strip())
    print(f"Transcript written to {out_path} ({len(result['text'])} chars)")

if __name__ == "__main__":
    main()
