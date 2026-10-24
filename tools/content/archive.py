"""The Archive. Endless stacks of VHS tapes under hanging lamps, and viewing booths with CRTs.
Some of the tapes are of you."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, pane, exit_block, custom, level, sound, BLOCK_MODELS, BLOCKSTATES
import tex as T
import synth as Y
from content.eb_kit import centered, text_mask, ink, brushed, wood, voice, speaker

S = 32


def spines(seed, gap=0.0, labels=True, colourful=0.15):
    """Two shelves of tape spines per block, with the board between them."""
    r = T.rng("spine", seed)
    img = np.zeros((S, S, 3))
    board = wood(seed + "b", "#3a2a1c", "#4a3626", plank=32)
    for shelf_y0 in (0, 16):
        y0, y1 = shelf_y0 + 2, shelf_y0 + 15
        img[shelf_y0:shelf_y0 + 2] = board[shelf_y0:shelf_y0 + 2] * 0.8
        img[y1:shelf_y0 + 16] = board[y1:shelf_y0 + 16]
        x = 0
        while x < S:
            w = 3 + int(r.random() * 2)
            if r.random() < gap:
                img[y0:y1, x:x + w] = T.hexc("#0c0a08")
                x += w
                continue
            c = r.random()
            if c < colourful * 0.5:
                base = T.hexc("#2a3a6a")
            elif c < colourful:
                base = T.hexc("#6a2a24")
            elif c < colourful + 0.1:
                base = T.hexc("#d8d4c8")
            else:
                base = T.hexc("#141414") * (0.8 + r.random() * 0.5)
            h = y1 - y0 - (1 if r.random() < 0.3 else 0)
            img[y1 - h:y1, x:min(S, x + w)] = base
            img[y1 - h:y1, x:min(S, x + 1)] *= 1.4
            if labels and r.random() < 0.85:
                ly0 = y1 - h + 2 + int(r.random() * 3)
                ll = 4 + int(r.random() * 5)
                lab = T.hexc("#e8e2cc") * (0.85 + r.random() * 0.15)
                img[ly0:ly0 + ll, x + 1:min(S, x + w - 1)] = lab
                # scribbled title
                for yy in range(ly0 + 1, ly0 + ll - 1, 2):
                    if r.random() < 0.8:
                        img[yy, x + 1:min(S, x + w - 1)] = T.hexc("#3a3a6a")
            x += w
    img = np.clip(img, 0, 1)
    return T.grain(img, seed, 0.03)


def shelf_top():
    img = wood("ast", "#3a2a1c", "#4a3626", plank=32)
    dust = T.noise("astd", sigma=1.5)
    return T.grain(T.blend(img, T.solid("#8a8478"), dust * 0.35), "ast", 0.04)


def shelf_end(num):
    def f():
        img = brushed("ase" + num, "#4a4c48", "#5c5e58", vertical=True)
        img = T.border(img, "#2a2c28", 1)
        img = T.rect(img, 4, 6, 28, 18, "#e0dac4")
        img = ink(img, centered(num, y=8, scale=1, spacing=1), "#1a1a1a")
        return T.grain(img, "ase" + num, 0.03)
    return f


def carpet():
    img = T.ramp(T.noise("acp", sigma=0.5) * 0.5 + T.fbm("acp2", base=5) * 0.5, "#23262c", "#2e323a")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx == 0) | (yy == 0)) * 0.25)
    img = T.shade(img, 1 + ((((xx // 4) + (yy // 4)) % 2) * 0.04))
    return T.grain(T.stains(img, "acps", "#1a1c20", count=1, strength=0.25, size=8), "acp", 0.04)


def ceiling():
    img = T.ramp(T.fbm("acl", base=4), "#2c2a26", "#36342e")
    pits = (T.rng("aclp").random((S, S)) > 0.85).astype(float)
    img = T.shade(img, 1 - pits * 0.15)
    return T.grain(T.border(img, "#1e1c1a", 1), "acl", 0.03)


def shade_green():
    img = T.ramp(T.noise("asg", sigma=1.5), "#1e3a2a", "#2a4a36")
    return T.grain(img, "asg", 0.04)


def bulb():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    return T.ramp(np.clip(1 - d / 18, 0, 1), "#e8b860", "#fff4d0")


def crt_body():
    img = T.ramp(T.noise("acb", sigma=1.5), "#8a8270", "#9a927e")
    yy, xx = np.mgrid[0:S, 0:S]
    vents = ((yy % 3) == 0) & (xx > 6) & (xx < 26) & (yy > 4) & (yy < 14)
    return T.grain(T.shade(img, 1 - vents * 0.3), "acb", 0.03)


def crt_screen(mode):
    def f():
        yy, xx = np.mgrid[0:S, 0:S]
        bezel = T.ramp(T.noise("acsb", sigma=1), "#2a2824", "#34322c")
        img = bezel.copy()
        scr = (xx >= 4) & (xx <= 27) & (yy >= 4) & (yy <= 23)
        cx, cy = 15.5, 13.5
        d = np.sqrt(((xx - cx) / 12) ** 2 + ((yy - cy) / 10) ** 2)
        if mode == "idle":
            pic = T.ramp(np.clip(1 - d, 0, 1), "#0a1430", "#2a4a9a")
            pic = ink(pic, text_mask("PLAY", S, S, 7, 10, 1, 0), "#d8e0ff")
            tri = (np.abs(yy - 13.5) < (25 - xx) * 0.6) & (xx >= 22) & (xx <= 25)
            pic = T.blend(pic, T.solid("#d8e0ff"), tri.astype(float))
        elif mode == "static":
            n = T.rng("acss").random((S, S))
            pic = T.ramp(n, "#2a2a2a", "#e8e8e8")
            rows = ((yy // 2) % 7 == 3)
            pic = T.shade(pic, 1 - rows * 0.4)
        else:
            # a figure in a corridor, seen from behind and above
            pic = T.ramp(np.clip(1 - d, 0, 1), "#3a3220", "#a89870")
            corridor = (np.abs(xx - 15.5) < (yy - 6) * 0.6)
            pic = T.shade(pic, 1 - corridor * 0.25)
            fig = (((xx - 15.5) ** 2 + (yy - 13) ** 2) < 2.5) | ((np.abs(xx - 15.5) < 1.6) & (yy > 14) & (yy < 20))
            pic = T.blend(pic, T.solid("#141008"), fig.astype(float))
            pic = T.shade(pic, 1 - ((yy % 2) == 0) * 0.15)
            pic = ink(pic, text_mask("REC", S, S, 5, 5, 1, 0), "#e83020")
        img = T.blend(img, pic, scr.astype(float))
        # curvature vignette on the glass
        img = T.shade(img, 1 - (scr * np.clip(d - 0.7, 0, 1) * 0.6))
        img = T.rect(img, 22, 26, 25, 28, "#5a5a50")
        img = T.rect(img, 26, 26, 28, 28, "#c02818" if mode != "idle" else "#3a1a14")
        return img
    return f


def partition():
    rgb = T.ramp(T.noise("apt", sigma=0.6), "#3a3e48", "#464a56")
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.blend(rgb, T.solid("#6a6a64"), ((yy < 2)).astype(float))
    return T.grain(rgb, "apt", 0.04)


def desk():
    return wood("ade", "#6a5a44", "#7a6a50", plank=32)


def vault_door():
    img = brushed("avd", "#5a5c5a", "#6e706c", vertical=True)
    img = T.border(img, "#2a2c2a", 2)
    img = T.rect(img, 3, 3, 29, 15, "#d8c8a0")
    img = ink(img, centered("FILM", y=4, scale=1), "#6a1a14")
    img = ink(img, centered("VAULT", y=10 - 2, scale=1, spacing=0) * 0, "#6a1a14")
    img = T.rect(img, 24, 20, 28, 24, "#a8a8a0")
    return T.grain(img, "avd", 0.04)


def vault_door_top():
    img = brushed("avdt", "#5a5c5a", "#6e706c", vertical=True)
    img = T.border(img, "#2a2c2a", 2)
    img = T.rect(img, 3, 12, 29, 26, "#d8c8a0")
    img = ink(img, centered("STAFF", y=13, scale=1, spacing=0), "#6a1a14")
    img = ink(img, centered("ONLY", y=20 - 1, scale=1), "#6a1a14") * 0 + ink(img, centered("ONLY", y=19, scale=1), "#6a1a14")
    return T.grain(img, "avdt", 0.04)


def tape_pile():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    r = T.rng("atp")
    for _ in range(14):
        x, y = int(r.random() * 26), int(r.random() * 26)
        w, h = (6, 3) if r.random() < 0.5 else (3, 6)
        rgb[y:y + h, x:x + w] = T.hexc("#141414") * (0.8 + r.random() * 0.4)
        a[y:y + h, x:x + w] = 1
        if r.random() < 0.7:
            rgb[y + h // 2, x:x + w] = T.hexc("#e0dac4")
    # unspooled tape ribbon
    for t in np.linspace(0, 1, 120):
        x = int(4 + t * 24 + np.sin(t * 12) * 3) % S
        y = int(6 + t * 20 + np.cos(t * 9) * 4) % S
        rgb[y, x] = T.hexc("#3a2a18")
        a[y, x] = 1
    return rgb, a


def cart_metal():
    return brushed("acm", "#5a5c60", "#72746a")


tex("arc_tapes_a", lambda: spines("ata", 0.03))
tex("arc_tapes_b", lambda: spines("atb", 0.08, colourful=0.25))
tex("arc_tapes_c", lambda: spines("atc", 0.0, colourful=0.05))
tex("arc_tapes_gap", lambda: spines("atg", 0.45))
tex("arc_shelf_top", shelf_top)
for n in ("A 13", "C 77", "F 02", "Y 0U", "K 40"):
    tex("arc_shelf_end_" + n.replace(" ", "").lower(), shelf_end(n))
tex("arc_carpet", carpet)
tex("arc_ceiling", ceiling)
tex("arc_shade", shade_green)
tex("arc_bulb", bulb)
tex("arc_crt_body", crt_body)
tex("arc_crt_idle", crt_screen("idle"))
tex("arc_crt_static", crt_screen("static"))
tex("arc_crt_you", crt_screen("you"))
tex("arc_partition", partition)
tex("arc_desk", desk)
tex("arc_vault_door", vault_door)
tex("arc_vault_door_top", vault_door_top)
tex("arc_tape_pile", tape_pile)
tex("arc_cart", cart_metal)

for nm in ("a", "b", "c", "gap"):
    cube_faces(f"arc_shelf_{nm}", top="arc_shelf_top", bottom="arc_shelf_top", side=f"arc_tapes_{nm}",
               display="Tape Shelving", sound="wood")
for n in ("a13", "c77", "f02", "y0u", "k40"):
    cube_faces(f"arc_shelf_end_{n}", top="arc_shelf_top", bottom="arc_shelf_top", side=f"arc_shelf_end_{n}",
               display="Shelf End Panel", sound="metal")
cube("arc_carpet", "arc_carpet", display="Archive Carpet Tile", sound="wool")
cube("arc_ceiling", "arc_ceiling", display="Archive Ceiling", flat=True)
shaped("arc_lamp", [(7.5, 8, 7.5, 8.5, 16, 8.5, "shade"),
                    (4, 3, 4, 12, 8, 12, {"all": "shade", "down": "bulb"}),
                    (6, 2, 6, 10, 3, 10, "bulb")],
       {"shade": "arc_shade", "bulb": "arc_bulb"}, display="Pendant Lamp", light=11, emissive=True,
       collides=False, unbreakable=True, sound="glass")
shaped("arc_lamp_dead", [(7.5, 8, 7.5, 8.5, 16, 8.5, "shade"), (4, 3, 4, 12, 8, 12, "shade")],
       {"shade": "arc_shade"}, display="Dead Pendant Lamp", collides=False, unbreakable=True, sound="glass")
pane("arc_partition", "arc_partition", display="Booth Partition", sound="wool")
shaped("arc_desk", [(0, 11, 0, 16, 12, 16, "d"), (1, 0, 1, 2, 11, 2, "d"), (14, 0, 1, 15, 11, 2, "d"),
                    (1, 0, 14, 2, 11, 15, "d"), (14, 0, 14, 15, 11, 15, "d")],
       {"d": "arc_desk"}, display="Viewing Desk", unbreakable=True)
shaped("arc_tape_pile", [(0, 0, 0, 16, 1, 16, {"all": None, "up": "t"})], {"t": "arc_tape_pile"},
       display="Spilled Tapes", collides=False, sound="wool", unbreakable=True)
shaped("arc_cart", [(2, 2, 2, 14, 3, 14, "m"), (2, 9, 2, 14, 10, 14, "m"), (2, 0, 2, 3, 12, 3, "m"),
                    (13, 0, 2, 14, 12, 3, "m"), (2, 0, 13, 3, 12, 14, "m"), (13, 0, 13, 14, 12, 14, "m"),
                    (3, 10, 3, 13, 14, 13, {"all": "tapes", "up": "top"})],
       {"m": "arc_cart", "tapes": "arc_tapes_a", "top": "arc_shelf_top"}, display="Returns Cart", unbreakable=True,
       sound="metal")
exit_block("arc_vault_door", "arc_vault_door", "DOOR", trigger="USE", solid=True, display="Film Vault Door", sound="metal")
cube("arc_vault_door_top", "arc_vault_door_top", display="Film Vault Door", sound="metal")


# the CRT: idle, playing static, or playing you
def crt_model(screen):
    return {"parent": "minecraft:block/block",
            "textures": {"b": "noclip:block/arc_crt_body", "s": f"noclip:block/{screen}", "particle": "noclip:block/arc_crt_body"},
            "elements": [{"from": [2, -4, 3], "to": [14, 9, 14], "faces": {
                "north": {"texture": "#s", "uv": [0, 0, 16, 16]}, "south": {"texture": "#b"}, "east": {"texture": "#b"},
                "west": {"texture": "#b"}, "up": {"texture": "#b"}, "down": {"texture": "#b"}}},
                {"from": [4, -2, 14], "to": [12, 7, 16], "faces": {f: {"texture": "#b"} for f in
                                                                   ("north", "south", "east", "west", "up", "down")}}]}


custom("arc_crt", "com.noclip.block.ArchiveCrtBlock", crt_model("arc_crt_idle"), display="Viewing Booth CRT",
       unbreakable=True, sound="glass", light=5, emissive=True, render="cutout")
BLOCK_MODELS["arc_crt_static"] = crt_model("arc_crt_static")
BLOCK_MODELS["arc_crt_you"] = crt_model("arc_crt_you")
_v = {}
for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
    for tape, model in (("0", "arc_crt"), ("1", "arc_crt_static"), ("2", "arc_crt_you")):
        m = {"model": f"noclip:block/{model}"}
        if r:
            m["y"] = r
        _v[f"facing={f},tape={tape}"] = m
BLOCKSTATES["arc_crt"] = {"variants": _v}

# ------------------------------------------------------------------ sound


def room_tone():
    d = 24.0
    t = Y.t_axis(d)
    hvac = Y.lp(Y.pink(d, "arh"), 500) * 0.6
    whine = np.sin(2 * np.pi * (round(7812 * d) / d) * t) * 0.006
    motors = Y.hum(d, 30.0, "arm", buzz=0.0, harmonics=(1, 2, 3)) * 0.06 * (0.6 + 0.4 * np.sin(2 * np.pi * t / 6))
    x = Y.reverb(hvac + motors, 2.0, 0.4, "arrv")[: len(t)] + whine
    return Y.stereoize(Y.loopable(Y.norm(x, 0.4), 1.0), 0.001), "stereo"


def vcr_clunk(seed="vc"):
    out = np.zeros(int(1.6 * Y.SR))
    for k, at in enumerate((0.0, 0.35, 0.55)):
        Y.place(out, Y.impact(f"{seed}{k}", 0.25), at, 0.8 - k * 0.2)
    t = Y.t_axis(0.9)
    motor = np.sin(2 * np.pi * np.cumsum(180 + 60 * t) / Y.SR) * 0.15 * np.sin(np.pi * t / 0.9)
    Y.place(out, motor, 0.6)
    return Y.norm(Y.lp(out, 4000), 0.7)


def rewind(dur=3.0, seed="rw"):
    t = Y.t_axis(dur)
    f0 = 300 + 900 * (t / dur) ** 0.5
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * 0.3 + Y.bp(Y.white(dur, seed), 2000, 6000) * 0.2
    x *= Y.env(len(t), 0.1, 0.2) * (0.8 + 0.2 * np.sin(2 * np.pi * 31 * t))
    return Y.norm(x, 0.6)


def static_burst(dur=1.4, seed="sb"):
    t = Y.t_axis(dur)
    x = Y.hp(Y.white(dur, seed), 800) * Y.env(len(t), 0.005, 0.3)
    x *= (Y.rng(seed + "g").random(len(t)) > 0.05)
    return Y.norm(x, 0.5)


def tape_play():
    """A tape playing: hiss, hum, a voice too far under the noise to understand."""
    d = 6.0
    t = Y.t_axis(d)
    hiss = Y.hp(Y.white(d, "tph"), 3000) * 0.12
    hum = Y.hum(d, 60.0, "tphum", buzz=0.1, harmonics=(1, 3)) * 0.05
    v = speaker(voice("tpv", d - 1.0, pitch=118, breath=0.2), 500, 2600) * 0.5
    x = hiss + hum
    Y.place(x, v, 0.5, 0.6)
    x *= 1 - 0.3 * (np.sin(2 * np.pi * 0.5 * t) > 0.95)
    return Y.norm(x, 0.6)


def step(i):
    def f():
        x = Y.footstep(f"arst{i}", "carpet")
        return Y.norm(Y.reverb(Y.lp(x, 1400), 1.2, 0.35, f"arstr{i}"), 0.7)
    return f


def cart_squeak():
    d = 2.2
    t = Y.t_axis(d)
    sq = np.sin(2 * np.pi * np.cumsum(1800 + 300 * np.sin(2 * np.pi * 3 * t)) / Y.SR) * (np.sin(2 * np.pi * 1.6 * t) > 0.6)
    return Y.norm(Y.reverb(sq * 0.3 + Y.lp(Y.white(d, "csq"), 200) * 0.2, 2.5, 0.6, "csqr"), 0.35)


sound("ambient.archive", [("ambient/archive", room_tone)], stream=True)
sound("ambient.archive.additions",
      [("ambient/archive_clunk", lambda: Y.norm(Y.reverb(vcr_clunk("avc"), 2.5, 0.7, "avcr"), 0.35)),
       ("ambient/archive_rewind", lambda: Y.norm(Y.reverb(Y.lp(rewind(4.0, "arw"), 2500), 2.5, 0.7, "arwr"), 0.3)),
       ("ambient/archive_cart", cart_squeak),
       ("ambient/archive_static", lambda: Y.norm(Y.reverb(Y.lp(static_burst(1.2, "ast"), 3000), 2.0, 0.7, "astr"), 0.25))],
      subtitle="Machines in the stacks")
sound("event.archive_insert", [("event/archive_insert", vcr_clunk)], subtitle="A tape goes in")
sound("event.archive_play", [("event/archive_play", tape_play)], subtitle="The tape plays")
sound("event.archive_rewind", [("event/archive_rewind", rewind)], subtitle="Rewinding")
sound("event.archive_static", [("event/archive_static", static_burst)], subtitle="Static")
sound("event.archive_step", [(f"event/archive_step{i}", step(i)) for i in range(3)], subtitle="Your own footsteps")

level("archive", fog="#16120e", sky="#16120e", ambient="ambient.archive", additions="ambient.archive.additions",
      additions_chance=0.004, fixed_time=18000, height=48)
