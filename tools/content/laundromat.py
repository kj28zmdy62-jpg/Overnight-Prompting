"""Laundromat - open all night. Rows of washers churning with nothing in them under hard fluorescent
light, folding tables, plastic chairs. One dryer is tumbling clothes that look a lot like yours."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, custom, flicker_light, level, sound, exit_block
import tex as T
import synth as Y
from content import afterhours_kit as K

S = 32


def lino():
    yy, xx = np.mgrid[0:S, 0:S]
    chk = ((xx // 16) + (yy // 16)) % 2 == 0
    img = np.where(chk[..., None], T.hexc("#d8dcc8"), T.hexc("#4a8a6a"))
    img = T.shade(img, 0.9 + T.noise("lmln", sigma=1.5) * 0.15)
    img = T.shade(img, 1 + np.clip(T.noise("lmlns", sigma=4) - 0.6, 0, 1) * 0.35)
    img = T.stains(img, "lmlnst", "#8a8a6a", count=1, strength=0.2, size=6)
    return T.grain(img, "lmln", 0.025)


def wall():
    img = T.ramp(T.fbm("lmw", octaves=3, base=4), "#d8d8c4", "#e2e2d0")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.08)
    img[26:] = T.ramp(T.noise("lmwk", sigma=1, shape=(6, S)), "#3a7a5a", "#428262")
    return K.grime_bottom(T.grain(img, "lmw", 0.03), "lmwg", 8, 0.2)


def ceiling():
    img = T.ramp(T.fbm("lmc", octaves=2, base=4), "#dcdcd0", "#e6e6da")
    return T.grain(T.border(img, "#b0b0a4", 1), "lmc", 0.02)


def tube(on):
    img = T.solid("#f4fcff") if on else T.ramp(T.noise("lmto", sigma=2), "#6a7074", "#7a8084")
    img = T.rect(img, 0, 0, 32, 3, "#c8ccd0")
    img = T.rect(img, 0, 29, 32, 32, "#c8ccd0")
    return img


def enamel():
    img = T.ramp(T.noise("lmen", sigma=2), "#e8eaea", "#f4f6f6")
    img = T.shade(img, 1 + np.clip(T.noise("lmens", sigma=3) - 0.6, 0, 1) * 0.3)
    return T.grain(K.grime_bottom(img, "lmeng", 6, 0.15), "lmen", 0.015)


def panel(timer):
    def f():
        img = T.ramp(T.noise("lmpn" + timer, sigma=1.5), "#c8ccd0", "#d8dce0")
        img = T.rect(img, 3, 8, 15, 18, "#0a0e0a")
        img = T.blend(img, T.solid("#ff5a2a"), K.text_mask(timer, S, S, 4, 11, font=K.F35, spacing=0))
        img = T.blend(img, T.solid("#3a3a3a"), K.disc(S, S, 23, 13, 4).astype(float))
        img = T.blend(img, T.solid("#c8c8c8"), K.disc(S, S, 23, 13, 1.5).astype(float))
        img = T.rect(img, 4, 22, 28, 26, "#3a3a40")
        img = T.rect(img, 14, 23, 18, 25, "#c0a040")  # coin slot
        return img
    return f


def drum_frame(i, clothes=None, door_open=False):
    """Front of a machine: round porthole with the drum turning behind it."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = enamel()
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    ang = np.arctan2(yy - 15.5, xx - 15.5) + i * (2 * np.pi / 8)
    if door_open:
        drum = T.ramp(T.noise("lmdo", sigma=2), "#020203", "#0a0a0c")
    else:
        holes = ((np.sin(ang * 6) > 0.6) & ((d % 3) < 1.2)).astype(float)
        drum = T.ramp(np.clip(d / 12, 0, 1), "#7a8288", "#3a4044")
        drum = T.shade(drum, 1 - holes * 0.5)
        fins = (np.abs(np.sin(ang * 1.5)) < 0.12) & (d > 6)
        drum = T.blend(drum, T.solid("#9aa2a8"), fins.astype(float))
        if clothes:
            # a striped shirt and dark trousers tumbling
            tumble = np.sin(ang * 2 + 1) > 0.2
            stripe = ((yy + i * 2) % 4 < 2)
            c1, c2 = clothes
            cloth = np.where(stripe[..., None], T.hexc(c1), T.hexc(c2))
            drum = T.blend(drum, cloth, (tumble & (d > 4) & (d < 11) & (yy > 12 + 4 * np.sin(i))).astype(float))
        glass = np.clip(1 - np.abs((xx + yy) - 26) / 3, 0, 1) * 0.35
        drum = T.blend(drum, T.solid("#e8f4ff"), glass)
    img = T.blend(img, drum, (d < 11.5).astype(float))
    img = T.blend(img, T.solid("#9aa0a4"), ((d >= 11.5) & (d < 13.5)).astype(float))
    img = T.blend(img, T.solid("#5a6066"), ((d >= 13.5) & (d < 14.2)).astype(float))
    return img


def out_of_order():
    img = drum_frame(0, door_open=True)
    img = T.rect(img, 4, 2, 28, 10, "#f0e8a0")
    img = T.blend(img, T.solid("#c82020"), K.centered("BROKEN", S, S, 4, font=K.F35))
    return img


def folding_top():
    img = T.ramp(T.noise("lmft", sigma=2), "#e8e0c8", "#f0e8d4")
    img = T.stains(img, "lmfts", "#b8a878", count=2, strength=0.25, size=5)
    return T.grain(img, "lmft", 0.02)


def chrome():
    yy, xx = np.mgrid[0:S, 0:S]
    return T.grain(T.ramp((np.sin(yy / 3.0) * 0.5 + 0.5) * 0.6 + T.noise("lmch", sigma=1) * 0.4, "#7a7e82", "#d0d4d8"), "lmch", 0.02)


def chair_blue():
    return T.grain(T.ramp(T.noise("lmcb", sigma=2), "#2a6a9a", "#3478aa"), "lmcb", 0.02)


def basket():
    yy, xx = np.mgrid[0:S, 0:S]
    a = (((xx % 4) < 2) | ((yy % 6) < 2)).astype(float)
    rgb = T.ramp(T.noise("lmbk", sigma=1), "#e8e8e0", "#f8f8f0")
    return rgb, a


def change_machine():
    img = T.ramp(T.noise("lmcm", sigma=1.5), "#5a6a7a", "#667888")
    img = T.blend(img, T.solid("#f0f0f0"), K.centered("CHANGE", S, S, 3, font=K.F35))
    img = T.rect(img, 8, 11, 24, 18, "#1a1a1a")
    img = T.blend(img, T.solid("#3aff6a"), K.centered("$1.00", S, S, 12, font=K.F35) * 0.9)
    img = T.rect(img, 12, 22, 20, 28, "#2a2a2a")
    return img


def board():
    img = T.ramp(T.noise("lmbd", sigma=1), "#a87a4a", "#b88a5a")
    r = T.rng("lmbdf")
    flyers = [("LOST", "#f0f0e0"), ("CAT", "#f0f0e0"), ("ROOM", "#e8f0a0"), ("HELP", "#f0c0c0"), ("6", "#e0e8f8")]
    for k, (t, c) in enumerate(flyers):
        x, y = int(r.random() * 20), int(r.random() * 20)
        img = T.rect(img, x, y, x + 12, y + 10, c)
        img = T.blend(img, T.solid("#2a2a2a"), K.text_mask(t, S, S, x + 1, y + 2, font=K.F35) * 0.8)
        img = T.rect(img, x + 5, y, x + 7, y + 1, "#d02020")
        for tear in range(x + 1, x + 11, 2):
            img[min(S - 1, y + 9), tear] = T.hexc("#8a8070")
    return img


def window_night():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("lmwn", sigma=3), "#04060a", "#0a0e16")
    img = T.blend(img, T.solid("#e83a6a"), K.centered("YRDNUAL", S, S, 12, font=K.F35) * 0.85)  # the neon, backwards
    img = T.blend(img, T.solid("#e83a6a"), K.glow(K.centered("YRDNUAL", S, S, 12, font=K.F35), 1.5) * 0.4)
    img[:, :1] = T.hexc("#9aa0a4")
    img[:1] = T.hexc("#9aa0a4")
    return img


def window_plain():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("lmwp", sigma=3), "#04060a", "#0a0e16")
    img = T.blend(img, T.solid("#5a6a7a"), np.clip(1 - np.abs((xx + yy * 0.5) % 28 - 8) / 2.5, 0, 1) * 0.2)
    img[:, :1] = T.hexc("#9aa0a4")
    img[:1] = T.hexc("#9aa0a4")
    return img


def detergent():
    img = T.solid("#1a1a20")
    for k in range(4):
        col = ["#e86a1a", "#2a8ae8", "#e8e030", "#30c060"][k]
        img = T.rect(img, 2 + k * 7, 8, 7 + k * 7, 24, col)
        img = T.rect(img, 3 + k * 7, 12, 6 + k * 7, 15, "#f0f0f0")
    img = T.rect(img, 0, 26, 32, 32, "#3a3a40")
    return T.border(img, "#8a8a90", 1)


tex("lm_floor", lino)
tex("lm_wall", wall)
tex("lm_ceiling", ceiling)
tex("lm_tube", lambda: tube(True))
tex("lm_tube_off", lambda: tube(False))
tex("lm_enamel", enamel)
for t in ("0:12", "0:00", "6:66", "1:07"):
    tex(f"lm_panel_{t.replace(':', '')}", panel(t))
K.animated("lm_drum", lambda i: drum_frame(i), 8, 2)
K.animated("lm_drum_fast", lambda i: drum_frame(i * 3), 8, 1)
K.animated("lm_drum_clothes", lambda i: drum_frame(i, ("#3a5ac8", "#e8e8e0")), 8, 2)
tex("lm_drum_open", lambda: drum_frame(0, door_open=True))
tex("lm_drum_still", lambda: drum_frame(0))
tex("lm_out_of_order", out_of_order)
tex("lm_folding_top", folding_top)
tex("lm_chrome", chrome)
tex("lm_chair_blue", chair_blue)
tex("lm_basket", basket)
tex("lm_change", change_machine)
tex("lm_board", board)
tex("lm_window_neon", window_night)
tex("lm_window", window_plain)
tex("lm_detergent", detergent)

cube("lm_floor", "lm_floor", display="Linoleum", sound="stone")
cube("lm_wall", "lm_wall", display="Laundromat Wall", sound="stone")
cube("lm_ceiling", "lm_ceiling", display="Laundromat Ceiling", sound="wool", flat=True)
flicker_light("lm_light", "lm_tube", "lm_tube_off", "lm_ceiling", light=15, flicker=0.02, display="Fluorescent Tube")
flicker_light("lm_light_flicker", "lm_tube", "lm_tube_off", "lm_ceiling", light=15, flicker=0.5, display="Failing Tube")
cube("lm_board", "lm_board", display="Bulletin Board", sound="wood")
cube("lm_window_neon", "lm_window_neon", display="Window", sound="glass", light=4, emissive=True)
cube("lm_window", "lm_window", display="Window", sound="glass")

WASHER = [(0, 0, 1, 16, 13, 16, {"all": "body", "north": "drum"}), (0, 13, 4, 16, 16, 16, {"all": "body", "north": "panel"})]
for name, drum, panel_t in (("lm_washer", "lm_drum", "lm_panel_012"), ("lm_washer_fast", "lm_drum_fast", "lm_panel_666"),
                            ("lm_washer_done", "lm_drum_still", "lm_panel_000"), ("lm_washer_open", "lm_drum_open", "lm_panel_000")):
    shaped(name, WASHER, {"body": "lm_enamel", "drum": drum, "panel": panel_t}, display="Washer", unbreakable=True,
           sound="metal")
exit_block("lm_washer_broken", "lm_out_of_order", "SECRET", trigger="USE", display="Washer (Out of Order)",
           solid=True, sound="metal")
DRYER = [(0, 0, 1, 16, 16, 16, {"all": "body", "north": "drum"})]
shaped("lm_dryer", DRYER, {"body": "lm_enamel", "drum": "lm_drum"}, display="Dryer", unbreakable=True, sound="metal")
shaped("lm_dryer_empty", DRYER, {"body": "lm_enamel", "drum": "lm_drum_open"}, display="Dryer", unbreakable=True,
       sound="metal")
shaped("lm_dryer_clothes", DRYER, {"body": "lm_enamel", "drum": "lm_drum_clothes"}, display="Dryer (Tumbling)",
       unbreakable=True, sound="metal")
import assets as A
A.BLOCKS[-1]["kind"] = "class"
A.BLOCKS[-1]["class"] = "com.noclip.block.LaundryDryerBlock"
shaped("lm_dryer_top", [(0, 0, 1, 16, 16, 16, {"all": "body", "north": "panel"})],
       {"body": "lm_enamel", "panel": "lm_panel_107"}, display="Dryer", unbreakable=True, sound="metal")
shaped("lm_folding_table", [(0, 12, 0, 16, 14, 16, {"all": "chrome", "up": "top"}), (1, 0, 1, 2, 12, 2, "chrome"),
                            (14, 0, 1, 15, 12, 2, "chrome"), (1, 0, 14, 2, 12, 15, "chrome"), (14, 0, 14, 15, 12, 15, "chrome")],
       {"chrome": "lm_chrome", "top": "lm_folding_top"}, display="Folding Table", unbreakable=True)
shaped("lm_chair", [(3, 6, 3, 13, 8, 13, "seat"), (3, 8, 12, 13, 16, 14, "seat"), (3, 0, 4, 4, 6, 5, "chrome"),
                    (12, 0, 4, 13, 6, 5, "chrome"), (3, 0, 11, 4, 6, 12, "chrome"), (12, 0, 11, 13, 6, 12, "chrome")],
       {"seat": "lm_chair_blue", "chrome": "lm_chrome"}, display="Plastic Chair", unbreakable=True)
shaped("lm_basket", [(2, 0, 3, 14, 9, 13, "b")], {"b": "lm_basket"}, display="Laundry Basket", unbreakable=True,
       collides=False)
shaped("lm_change_machine", [(2, 0, 3, 14, 16, 15, {"all": "body", "north": "front"})],
       {"body": "lm_chrome", "front": "lm_change"}, display="Change Machine", light=4, emissive=True, unbreakable=True)
shaped("lm_detergent_machine", [(1, 0, 2, 15, 16, 15, {"all": "body", "north": "front"})],
       {"body": "lm_enamel", "front": "lm_detergent"}, display="Detergent Vending", kind="pickup",
       item="noclip:almond_water*1,noclip:battery*1", light=5, emissive=True, sound="metal")

# ------------------------------------------------------------------ sound


def laundry_tone():
    d = 24.0
    t = Y.t_axis(d)
    out = np.zeros_like(t)
    # several washers out of phase: agitate left, pause, agitate right
    for k, (period, g) in enumerate(((1.6, 0.25), (2.0, 0.2), (1.3, 0.15))):
        slosh = Y.lp(Y.white(d, f"lmsl{k}"), 500) * (np.sin(2 * np.pi * t / period + k) > 0.2)
        out += slosh * g
    # a dryer: the regular thump of something heavy going round
    thumps = np.zeros_like(t)
    for at in np.arange(0.4, d, 0.75):
        Y.place(thumps, Y.impact(f"lmth{int(at * 10) % 4}", 0.2), at, 0.3)
    tubes = Y.hp(Y.hum(d, 60.0, "lmhum", buzz=0.7, harmonics=(2, 4, 6, 8)), 200) * 0.12
    motor = np.sin(2 * np.pi * 50 * t) * 0.05 + Y.bp(Y.pink(d, "lmmot"), 200, 1200) * 0.08
    x = Y.lp(out, 1500) + Y.lp(thumps, 600) + tubes + motor
    x = K.loop_reverb(x, 1.2, 0.3, "lmtone")
    return Y.stereoize(Y.loopable(Y.norm(x, 0.45), 1.0), 0.0015), "stereo"


def buzzer():
    d = 1.6
    t = Y.t_axis(d)
    x = np.sign(np.sin(2 * np.pi * 220 * t)) * 0.5 * (t < 1.2) + np.sin(2 * np.pi * 440 * t) * 0.2 * (t < 1.2)
    return Y.norm(Y.reverb(Y.bp(x, 200, 3000), 1.5, 0.4, "lmbz"), 0.7)


def spin():
    d = 5.0
    t = Y.t_axis(d)
    f = 30 + 70 * np.clip(t / 3, 0, 1)
    x = np.sin(2 * np.pi * np.cumsum(f) / Y.SR) * 0.5 + Y.bp(Y.white(d, "lmsp"), 300, 2000) * 0.3 * np.clip(t / 3, 0, 1)
    x *= np.minimum((d - t) * 2, 1)
    return Y.norm(Y.reverb(x, 1.5, 0.4, "lmsp"), 0.7)


def coins():
    out = np.zeros(int(1.5 * Y.SR))
    for k in range(5):
        n = int(0.08 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, np.sin(2 * np.pi * (2400 + k * 130) * tt) * np.exp(-tt * 40), 0.05 + k * 0.12, 0.6)
    return Y.norm(Y.reverb(out, 1.5, 0.5, "lmco"), 0.5)


def dryer_door():
    out = np.zeros(int(2.0 * Y.SR))
    Y.place(out, Y.impact("lmdd", 0.3, ring=0.5), 0.05)
    hiss = Y.lp(Y.white(1.0, "lmdh"), 1500) * np.exp(-np.arange(int(Y.SR)) / (0.3 * Y.SR)) * 0.4
    Y.place(out, hiss, 0.1)
    return Y.norm(Y.reverb(out, 1.2, 0.4, "lmdd"), 0.7)


def zipper():
    out = np.zeros(int(3.0 * Y.SR))
    for k in range(4):
        n = int(0.05 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(out, Y.bp(Y.white(0.05, f"lmz{k}"), 2000, 8000)[:n] * np.exp(-tt * 80), 0.2 + k * 0.75, 0.6)
    return Y.norm(Y.reverb(out, 1.5, 0.5, "lmz"), 0.4)


sound("ambient.laundromat", [("ambient/laundromat", laundry_tone)], stream=True)
sound("ambient.laundromat.additions",
      [("ambient/laundromat_buzzer", buzzer), ("ambient/laundromat_coins", coins), ("ambient/laundromat_zipper", zipper),
       ("ambient/laundromat_spin", spin)],
      subtitle="Machines running")
sound("event.laundromat_buzzer", [("event/laundromat_buzzer", buzzer)], subtitle="A cycle ends")
sound("event.laundromat_dryer", [("event/laundromat_dryer", dryer_door)], subtitle="A dryer door opens")
sound("event.laundromat_spin", [("event/laundromat_spin", spin)], subtitle="Spin cycle")

level("laundromat", fog="#c8d0c8", sky="#c8d0c8", ambient="ambient.laundromat", additions="ambient.laundromat.additions",
      additions_chance=0.005, fixed_time=6000, height=48)
