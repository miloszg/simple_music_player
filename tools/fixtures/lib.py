# A believable lofi / Japanese-hip-hop library.
#
# Names are original, written in the idiom rather than lifted from real
# releases: shipping actual artist or album names in Play Store screenshots
# implies a licensing relationship that does not exist, and Play's misleading
# -content policy is enforced on exactly that.
LIBRARY = [
  dict(dir="Hanami Tape", album="Hanami Tape", artist="Yuki Sōma", year=2019,
       art="hanami", freq=196, tracks=[
        ("Komorebi", 214), ("Paper Lanterns", 187), ("Third Cup", 252),
        ("Sakura Static", 168), ("Tatami Hours", 231), ("Petal, Falling Slow", 205)]),

  dict(dir="Shibuya 4AM", album="Shibuya 4AM", artist="Kenji Aoyama", year=2021,
       art="shibuya", freq=220, tracks=[
        ("Last Train, Yamanote", 243), ("Rain on the Crossing", 199),
        ("Konbini Light", 176), ("Nakameguro Rooftop", 268), ("4AM, Still Up", 302)]),

  dict(dir="Kissaten", album="Kissaten", artist="The Ochre Set", year=2017,
       art="kissaten", freq=174, tracks=[
        ("Siphon", 228), ("Second Pour", 191), ("Vinyl, B-Side", 264),
        ("Smoke and Saucers", 217), ("Closing Time, 11pm", 246)]),

  dict(dir="Winter Kaido", album="Winter Kaidō", artist="Sora & Ash", year=2023,
       art="kaido", freq=147, tracks=[
        ("Snow on the Old Road", 289), ("Kotatsu", 203),
        ("Hokkaidō Local", 254), ("Breath, Visible", 178)]),

  # Deliberately artwork-free — these are what exercise the fallback plates.
  dict(dir="Field Recordings", album="Field Recordings, Vol. 1", artist="Miyako Fields",
       year=2020, art=None, freq=165, tracks=[
        ("Cicada, Late August", 312), ("Shrine Steps", 224),
        ("Rain, Porch, Nothing", 275), ("Ferry to Enoshima", 198)]),

  dict(dir="Untagged Rips", album="", artist="", year=None, art=None, freq=138, tracks=[
        ("track01", 201), ("track02", 243), ("track03", 187)]),
]
