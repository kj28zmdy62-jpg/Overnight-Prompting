"""Call Center. Cubicle rows under fluorescent panels, and every phone on the floor ringing for you."""
import numpy as np
from assets import cube, cube_faces, tex, flicker_light, shaped, pane, custom, level, sound, BLOCK_MODELS, BLOCKSTATES
import tex as T
import synth as Y
from content.eb_kit import centered, ink, brushed, wood, voice, speaker, chime

S = 32


def carpet():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("ccc", sigma=0.5) * 0.6 + T.fbm("ccc2", base=4) * 0.4, "#3a4250", "#465060")
    # a corporate fleck pattern and tile seams
    fleck = ((xx * 7 + yy * 3) % 11 == 0) & ((xx + yy * 5) % 7 == 0)
    img = T.blend(img, T.solid("#7a3a48"), fleck.astype(float) * 0.7)
    img = T.shade(img, 1 - ((xx == 0) | (yy == 0)) * 0.18)
    return T.grain(T.stains(img, "cccs", "#2a303a", count=1, strength=0.3, size=7), "ccc", 0.04)


def ceiling():
    img = T.ramp(T.fbm("cce", base=4), "#c8ccd0", "#d4d8dc")
    pits = (T.rng("ccep").random((S, S)) > 0.88).astype(float)
    img = T.shade(img, 1 - pits * 0.1)
    img = T.border(img, "#9aa0a6", 1)
    return T.grain(img, "cce", 0.02)


def panel(on=True):
    yy, xx = np.mgrid[0:S, 0:S]
    if on:
        img = T.solid("#f4faff")
        louver = ((xx % 4) == 0) | ((yy % 4) == 0)
        img = T.shade(img, 1 - louver * 0.12)
    else:
        img = T.ramp(T.noise("ccpo", sigma=2), "#7a8088", "#8a9098")
        louver = ((xx % 4) == 0) | ((yy % 4) == 0)
        img = T.shade(img, 1 - louver * 0.2)
    return T.border(T.border(img, "#a8aeb4", 2), "#7a8086", 1)


def partition():
    rgb = T.ramp(T.noise("ccp", sigma=0.5), "#5a6070", "#666c7c")
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.blend(rgb, T.solid("#a8acb0"), (yy < 3).astype(float))
    # pinned notes on some panels
    return T.grain(rgb, "ccp", 0.05)


def partition_notes():
    img = partition()
    r = T.rng("ccn")
    for col in ("#f0e070", "#f0a0c0", "#a0e0f0"):
        x, y = 3 + int(r.random() * 20), 6 + int(r.random() * 16)
        img = T.rect(img, x, y, x + 7, y + 7, col)
        for yy in range(y + 2, y + 6, 2):
            img[yy, x + 1:x + 6] *= 0.6
    return img


def desk_top():
    img = T.ramp(T.noise("ccd", sigma=2), "#c8c0b0", "#d4ccbc")
    return T.grain(T.border(img, "#5a5650", 1), "ccd", 0.02)


def desk_side():
    return T.grain(T.ramp(T.noise("ccds", sigma=1.5), "#4a4c50", "#565860"), "ccds", 0.03)


def plastic(seed, lo, hi):
    return T.grain(T.ramp(T.noise(seed, sigma=1.2), lo, hi), seed, 0.03)


def phone_top(ringing):
    img = plastic("ccph" + str(ringing), "#2a2a2c", "#36363a")
    yy, xx = np.mgrid[0:S, 0:S]
    # handset across the top, keypad below, line light
    hand = (yy >= 3) & (yy <= 10) & (xx >= 3) & (xx <= 28)
    img = T.blend(img, plastic("cchs", "#1a1a1c", "#222226"), hand.astype(float))
    keys = (yy >= 14) & (yy <= 27) & (xx >= 6) & (xx <= 20) & ((xx - 6) % 5 < 3) & ((yy - 14) % 4 < 3)
    img = T.blend(img, T.solid("#9a9aa0"), keys.astype(float))
    led = (np.abs(xx - 25) <= 1) & (np.abs(yy - 20) <= 1)
    img = T.blend(img, T.solid("#ff3020" if ringing else "#3a1410"), led.astype(float))
    return img


def monitor(on):
    yy, xx = np.mgrid[0:S, 0:S]
    img = plastic("ccm", "#1e1e20", "#2a2a2e")
    scr = (xx >= 3) & (xx <= 28) & (yy >= 3) & (yy <= 24)
    if on:
        pic = T.ramp(np.clip(1 - np.abs(yy - 12) / 20, 0, 1), "#0a2a4a", "#2a5a8a")
        rows = ((yy - 5) % 3 == 0) & (xx > 5) & (xx < 5 + (yy * 7 % 19) + 4)
        pic = T.blend(pic, T.solid("#c8e0f8"), rows.astype(float) * 0.8)
        pic = ink(pic, centered("CALL", y=16, scale=1) * (yy > 15), "#ff4a3a")
    else:
        pic = T.ramp(np.clip((xx + yy) / 64, 0, 1), "#0a0a0c", "#1a1c20")
    img = T.blend(img, pic, scr.astype(float))
    return img


def red_phone():
    img = plastic("ccrp", "#8a1410", "#a41c16")
    yy, xx = np.mgrid[0:S, 0:S]
    hand = (yy >= 3) & (yy <= 10) & (xx >= 3) & (xx <= 28)
    img = T.blend(img, plastic("ccrh", "#6a0e0a", "#7a120e"), hand.astype(float))
    dial = np.sqrt((xx - 16) ** 2 + (yy - 21) ** 2)
    img = T.blend(img, T.solid("#e8e0d0"), ((dial < 7) & (dial > 3)).astype(float))
    return img


def glass():
    rgb = T.solid("#c8dce0")
    yy, xx = np.mgrid[0:S, 0:S]
    a = np.ones((S, S)) * 0.18 + (np.abs(((xx + yy) % 32) - 9) < 2) * 0.15
    frost = (yy > 12) & (yy < 18)
    a = np.where(frost, 0.7, a)
    rgb = T.blend(rgb, T.solid("#e8eef0"), frost.astype(float))
    edge = (xx == 0) | (xx == 31)
    rgb = T.blend(rgb, T.solid("#6a7074"), edge.astype(float))
    a = np.where(edge, 1.0, a)
    return rgb, a


def board(text, color="#ff3020"):
    def f():
        img = T.solid("#0a0a0a")
        yy, xx = np.mgrid[0:S, 0:S]
        img = T.blend(img, T.solid("#1a0806"), (((xx % 2) == 0) & ((yy % 2) == 0)).astype(float))
        img = T.blend(img, T.solid(color), centered(text, y=12, scale=1, spacing=1 if len(text) <= 5 else 0) * 0.95)
        return T.border(img, "#3a3a3a", 2)
    return f


def column():
    img = T.ramp(T.fbm("cco", base=5), "#a8acb0", "#b4b8bc")
    img[26:] = T.hexc("#3a3c40")
    return T.grain(img, "cco", 0.02)


def whiteboard():
    img = T.ramp(T.noise("ccw", sigma=3), "#e8eaec", "#f4f6f8")
    img = T.border(img, "#8a8e92", 2)
    yy, xx = np.mgrid[0:S, 0:S]
    # someone has been counting
    for k in range(6):
        x0 = 4 + k * 4
        img[6:14, x0] = T.hexc("#2a3a8a")
    img[10, 3:28] = T.hexc("#2a3a8a")
    img = ink(img, centered("WHO", y=18, scale=1), "#c02020")
    return img


def cooler():
    img = plastic("ccq", "#d8d8d4", "#e4e4e0")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.blend(img, T.solid("#4a7ab8"), ((yy > 4) & (yy < 9) & (xx > 10) & (xx < 22)).astype(float))
    return img


def bottle_tex():
    return T.ramp(T.noise("ccbt", sigma=2), "#a8c8e0", "#c8e0f0"), np.ones((S, S)) * 0.7


tex("cc_carpet", carpet)
tex("cc_ceiling", ceiling)
tex("cc_panel", lambda: panel(True))
tex("cc_panel_off", lambda: panel(False))
tex("cc_partition", partition)
tex("cc_partition_notes", partition_notes)
tex("cc_desk_top", desk_top)
tex("cc_desk_side", desk_side)
tex("cc_phone", lambda: phone_top(False))
tex("cc_phone_ringing", lambda: phone_top(True))
tex("cc_phone_body", lambda: plastic("ccpb", "#2a2a2c", "#36363a"))
tex("cc_monitor_on", lambda: monitor(True))
tex("cc_monitor_off", lambda: monitor(False))
tex("cc_monitor_body", lambda: plastic("ccmb", "#2a2a2e", "#34343a"))
tex("cc_red_phone", red_phone)
tex("cc_glass", glass)
tex("cc_board_calls", board("CALLS"))
tex("cc_board_999", board("999"))
tex("cc_board_hold", board("HOLD"))
tex("cc_board_you", board("YOU", "#ffb020"))
tex("cc_column", column)
tex("cc_whiteboard", whiteboard)
tex("cc_cooler", cooler)
tex("cc_bottle", bottle_tex)

cube("cc_carpet", "cc_carpet", display="Office Carpet Tile", sound="wool")
cube("cc_ceiling", "cc_ceiling", display="Drop Ceiling", sound="wool", flat=True)
flicker_light("cc_panel", "cc_panel", "cc_panel_off", "cc_ceiling", light=15, flicker=0.0, display="Office Light Panel")
flicker_light("cc_panel_flicker", "cc_panel", "cc_panel_off", "cc_ceiling", light=15, flicker=0.6,
              display="Failing Office Light Panel")
cube_faces("cc_panel_dead", bottom="cc_panel_off", top="cc_ceiling", side="cc_ceiling", flat=True,
           display="Dead Office Light Panel", sound="glass")
pane("cc_partition", "cc_partition", display="Cubicle Partition", sound="wool")
pane("cc_partition_notes", "cc_partition_notes", "cc_partition", display="Cubicle Partition", sound="wool")
pane("cc_glass", "cc_glass", display="Office Glass", sound="glass", render="translucent")
shaped("cc_desk", [(0, 11, 0, 16, 12, 16, {"all": "side", "up": "top"}), (0, 0, 13, 16, 11, 15, "side"),
                   (0, 0, 0, 1, 11, 13, "side"), (15, 0, 0, 16, 11, 13, "side")],
       {"top": "cc_desk_top", "side": "cc_desk_side"}, display="Workstation", unbreakable=True)
shaped("cc_monitor", [(3, -4, 6, 13, 4, 7, {"all": "body", "north": "scr"}), (7, -4, 7, 9, -1, 10, "body"),
                      (5, -4, 7, 11, -3.5, 11, "body")],
       {"body": "cc_monitor_body", "scr": "cc_monitor_on"}, display="Monitor", unbreakable=True, emissive=True,
       collides=False)
shaped("cc_monitor_off", [(3, -4, 6, 13, 4, 7, {"all": "body", "north": "scr"}), (7, -4, 7, 9, -1, 10, "body"),
                          (5, -4, 7, 11, -3.5, 11, "body")],
       {"body": "cc_monitor_body", "scr": "cc_monitor_off"}, display="Monitor", unbreakable=True, collides=False)
shaped("cc_board", [(0, 0, 7, 16, 8, 9, {"all": "f", "north": "a", "south": "b"}), (7, 8, 7.5, 9, 16, 8.5, "f")],
       {"a": "cc_board_calls", "b": "cc_board_999", "f": "cc_monitor_body"}, display="Queue Board", emissive=True,
       collides=False, unbreakable=True)
shaped("cc_board2", [(0, 0, 7, 16, 8, 9, {"all": "f", "north": "a", "south": "b"}), (7, 8, 7.5, 9, 16, 8.5, "f")],
       {"a": "cc_board_999", "b": "cc_board_calls", "f": "cc_monitor_body"}, display="Queue Board", emissive=True,
       collides=False, unbreakable=True)
shaped("cc_board_youhold", [(0, 0, 7, 16, 8, 9, {"all": "f", "north": "a", "south": "a"}), (7, 8, 7.5, 9, 16, 8.5, "f")],
       {"a": "cc_board_hold", "f": "cc_monitor_body"}, display="Queue Board", emissive=True, collides=False,
       unbreakable=True)
cube("cc_column", "cc_column", display="Office Column")
shaped("cc_whiteboard", [(0, 0, 0, 16, 16, 1, {"all": "f", "south": "w"})], {"w": "cc_whiteboard", "f": "cc_desk_side"},
       display="Whiteboard", collides=False, unbreakable=True)
shaped("cc_cooler", [(4, 0, 4, 12, 13, 12, "c"), (5, 13, 5, 11, 16, 11, "b")], {"c": "cc_cooler", "b": "cc_bottle"},
       display="Water Cooler", kind="pickup", item="noclip:almond_water*2", render="translucent", strength=0.5)


# desk phones: a Java block that rings
def phone_model(top):
    return {"parent": "minecraft:block/block",
            "textures": {"t": f"noclip:block/{top}", "b": "noclip:block/cc_phone_body", "particle": "noclip:block/cc_phone_body"},
            "elements": [{"from": [3, -4, 3], "to": [13, -1, 13], "faces": {
                "up": {"texture": "#t", "uv": [0, 0, 16, 16]}, "north": {"texture": "#b"}, "south": {"texture": "#b"},
                "east": {"texture": "#b"}, "west": {"texture": "#b"}, "down": {"texture": "#b"}}}]}


custom("cc_phone", "com.noclip.block.CallPhoneBlock", phone_model("cc_phone"), display="Desk Phone",
       unbreakable=True, sound="metal", render="cutout")
BLOCK_MODELS["cc_phone_ringing"] = phone_model("cc_phone_ringing")
custom("cc_red_phone", "com.noclip.block.CallPhoneBlock", phone_model("cc_red_phone"), display="Supervisor's Phone",
       unbreakable=True, sound="metal", render="cutout")
BLOCK_MODELS["cc_red_phone_ringing"] = phone_model("cc_red_phone")
for nm in ("cc_phone", "cc_red_phone"):
    v = {}
    for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for ring in ("false", "true"):
            m = {"model": f"noclip:block/{nm}{'_ringing' if ring == 'true' else ''}"}
            if r:
                m["y"] = r
            v[f"facing={f},ringing={ring}"] = m
    BLOCKSTATES[nm] = {"variants": v}

# ------------------------------------------------------------------ sound


def ring_burst(t0, out, f1=1020, f2=1300, gain=1.0):
    d = 0.4
    t = Y.t_axis(d)
    trill = np.sign(np.sin(2 * np.pi * 20 * t))
    tone = np.where(trill > 0, np.sin(2 * np.pi * f1 * t), np.sin(2 * np.pi * f2 * t))
    Y.place(out, tone * Y.env(len(t), 0.005, 0.02) * gain, t0)


def ring():
    out = np.zeros(int(2.0 * Y.SR))
    ring_burst(0.0, out)
    ring_burst(0.6, out)
    out = Y.lp(np.tanh(out * 1.5), 5000)
    return Y.norm(Y.reverb(out, 1.2, 0.3, "ccring"), 0.8)


def far_phones():
    d = 24.0
    t = Y.t_axis(d)
    out = np.zeros(len(t))
    r = Y.rng("ccfar")
    for k in range(9):
        start = r.random() * d
        f1 = 900 + r.random() * 400
        for rep in range(4):
            at = (start + rep * 3.0) % (d - 1.5)
            ring_burst(at, out, f1, f1 * 1.25, 0.15 + r.random() * 0.15)
            ring_burst(at + 0.6, out, f1, f1 * 1.25, 0.15 + r.random() * 0.15)
    out = Y.lp(out, 1800)
    hvac = Y.lp(Y.pink(d, "cchv"), 400) * 0.5
    buzz = Y.hum(d, 60.0, "ccbz", buzz=0.25, harmonics=(2, 4)) * 0.04
    x = Y.reverb(out * 0.5 + hvac, 3.0, 0.65, "ccrv")[: len(t)] + Y.hp(buzz, 200)
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.5), 0.002), "stereo"


def pickup():
    out = np.zeros(int(0.8 * Y.SR))
    Y.place(out, Y.impact("ccpk", 0.12), 0.0, 0.8)
    Y.place(out, Y.bp(Y.white(0.5, "ccln"), 300, 3400) * 0.06, 0.15)
    return Y.norm(out, 0.7)


def caller(i):
    """Your own voice, badly, down a bad line."""
    def f():
        d = 3.2 + i * 0.6
        v = voice(f"ccv{i}", d, pitch=112 + i * 9, breath=0.25)
        v = Y.reverb(v, 0.4, 0.2, f"ccvr{i}")[: len(v)]
        # dropouts, then a wrong-speed patch
        n = len(v)
        gate = np.ones(n)
        r = Y.rng(f"ccg{i}")
        for _ in range(4):
            a = int(r.random() * n)
            gate[a:a + int(0.12 * Y.SR)] = 0.1
        v = v * gate
        half = n // 2
        slow = np.interp(np.arange(half, n) * 0.8, np.arange(n), v)
        v = np.concatenate([v[:half], slow[: n - half]])
        line = Y.bp(Y.white(d, f"cch{i}"), 400, 3000) * 0.05
        return Y.norm(speaker(v + line, 450, 2800), 0.75)
    return f


def hold_music():
    d = 6.0
    out = np.zeros(int(d * Y.SR))
    notes = [523.3, 659.3, 784.0, 659.3, 698.5, 587.3, 523.3, 493.9]
    for k, f in enumerate(notes):
        t = Y.t_axis(0.7)
        tone = np.sign(np.sin(2 * np.pi * f * t)) * 0.3 * np.exp(-t * 2)
        Y.place(out, tone, k * 0.72)
    return Y.norm(speaker(Y.lp(out, 2400), 400, 3000), 0.5)


def dial_tone():
    d = 2.0
    t = Y.t_axis(d)
    x = (np.sin(2 * np.pi * 350 * t) + np.sin(2 * np.pi * 440 * t)) * 0.4 * Y.env(len(t), 0.01, 0.05)
    return Y.norm(speaker(x), 0.5)


def fax():
    d = 3.0
    t = Y.t_axis(d)
    f0 = np.where((t * 4).astype(int) % 2 == 0, 1100, 2100) + 300 * np.sin(2 * np.pi * 37 * t)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * Y.env(len(t), 0.02, 0.2)
    return Y.norm(Y.reverb(speaker(x), 3.0, 0.7, "ccfx"), 0.3)


def chair_roll():
    d = 1.8
    t = Y.t_axis(d)
    x = Y.lp(Y.white(d, "ccch"), 600) * np.sin(np.pi * t / d) * (0.7 + 0.3 * np.sin(2 * np.pi * 9 * t))
    return Y.norm(Y.reverb(x, 2.5, 0.6, "ccchr"), 0.35)


def all_lines():
    d = 5.0
    out = np.zeros(int(d * Y.SR))
    r = Y.rng("ccall")
    for k in range(30):
        f1 = 800 + r.random() * 700
        at = r.random() * 4.0
        ring_burst(at, out, f1, f1 * 1.27, 0.5)
    return Y.norm(Y.reverb(np.tanh(out), 2.0, 0.5, "ccallr"), 0.9)


sound("ambient.call_center", [("ambient/call_center", far_phones)], stream=True)
sound("ambient.call_center.additions", [("ambient/call_center_fax", fax), ("ambient/call_center_chair", chair_roll),
                                        ("ambient/call_center_dial", lambda: Y.norm(Y.reverb(dial_tone(), 2.0, 0.7, "ccdt"), 0.3))],
      subtitle="Somewhere on the floor")
sound("event.call_center_ring", [("event/call_center_ring", ring)], subtitle="A phone rings")
sound("event.call_center_pickup", [("event/call_center_pickup", pickup)], subtitle="You pick up")
sound("event.call_center_voice", [(f"event/call_center_voice{i}", caller(i)) for i in range(3)], subtitle="Your voice, on the line")
sound("event.call_center_hold", [("event/call_center_hold", hold_music)], subtitle="Hold music")
sound("event.call_center_all", [("event/call_center_all", all_lines)], subtitle="Every phone at once")

level("call_center", fog="#8a9098", sky="#8a9098", ambient="ambient.call_center",
      additions="ambient.call_center.additions", additions_chance=0.003, fixed_time=6000, height=32)
