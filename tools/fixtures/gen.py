import subprocess, pathlib
from lib import LIBRARY

root = pathlib.Path("music"); 
for a in LIBRARY:
    d = root / a["dir"]; d.mkdir(parents=True, exist_ok=True)
    for i, (title, dur) in enumerate(a["tracks"], 1):
        f = d / f"{i:02d}.mp3"
        args = ["ffmpeg", "-loglevel", "error",
                "-f", "lavfi", "-i", f"sine=frequency={a['freq'] + i*7}:duration={dur}"]
        if a["art"]:
            args += ["-i", f"art/{a['art']}.png", "-map", "0:a", "-map", "1:v",
                     "-c:v", "copy", "-disposition:v", "attached_pic",
                     "-metadata:s:v", "title=Album cover",
                     "-metadata:s:v", "comment=Cover (front)"]
        args += ["-metadata", f"title={title}", "-metadata", f"track={i}"]
        if a["artist"]:
            args += ["-metadata", f"artist={a['artist']}",
                     "-metadata", f"album_artist={a['artist']}"]
        if a["album"]:
            args += ["-metadata", f"album={a['album']}"]
        if a["year"]:
            args += ["-metadata", f"date={a['year']}"]
        args += ["-codec:a", "libmp3lame", "-b:a", "112k", str(f), "-y"]
        subprocess.run(args, check=True)
    print(f"  {a['dir']:<20} {len(a['tracks'])} tracks  art={'yes' if a['art'] else 'no '}")

n = len(list(root.rglob("*.mp3")))
total = sum(d for a in LIBRARY for _, d in a["tracks"])
print(f"\n{n} tracks, {total//60}m{total%60:02d}s total")
