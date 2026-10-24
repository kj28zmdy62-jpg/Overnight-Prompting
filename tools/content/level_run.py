"""Level ! - Run For Your Life. A red-lit corridor, an alarm, and something right behind you."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, cross, flicker_light, entity_model, \
    entity_tex, lang, ITEM_MODELS
import tex as T
import synth as Y
from content.deepend_font import draw
from content.deepend_audio import wrapverb, fit
from content.entities_core import humanoid, paint_humanoid
from entity_paint import Canvas, skin

S = 32
yy, xx = np.mgrid[0:S, 0:S]


def wall_base(seed):
    img = T.ramp(T.fbm(seed, octaves=3, base=5) * 0.6 + 0.2, "#b9aca6", "#cdc0b8")
    seam = ((yy % 16) == 0) | ((xx % 16) == 0)
    img = T.shade(img, 1 - seam * 0.18)
    # scuffs at hip height from things being dragged along
    scuff = np.clip(1 - np.abs(yy - 20) / 3.0, 0, 1) * T.noise(seed + "sc", sigma=0.8)
    img = T.shade(img, 1 - scuff * 0.2)
    return T.grain(T.stains(img, seed, "#6a5a50", count=2, strength=0.25, size=7), seed, 0.04)


def blood(img, seed, amount=1.0, smear=True):
    r = T.rng("blood", seed)
    m = np.zeros((S, S))
    if smear:
        # a hand dragged along the wall
        y0 = 8 + int(r.random() * 10)
        for x in range(S):
            y = y0 + int(2 * np.sin(x / 5.0 + r.random()))
            for k in range(4):
                if 0 <= y + k < S:
                    m[y + k, x] = max(m[y + k, x], (0.9 - k * 0.15) * (0.4 + 0.6 * np.sin(np.pi * x / S)))
    for _ in range(3):
        x = int(r.random() * S)
        y0 = int(r.random() * 16)
        for y in range(y0, min(S, y0 + 6 + int(r.random() * 14))):
            m[y, x] = max(m[y, x], 0.8)
    m = np.clip(m * amount, 0, 1)
    return T.blend(img, T.ramp(T.noise(seed + "bl", sigma=0.7), "#3a0606", "#5a0a0a"), m)


def wall():
    return wall_base("rw")


def wall_blood():
    return blood(wall_base("rwb"), "rwb")


def wall_scratch():
    img = wall_base("rws")
    r = T.rng("rwscr")
    for k in range(5):
        x0 = 6 + k * 4 + int(r.random() * 2)
        for y in range(4, 28):
            x = x0 + int((y - 4) * 0.18)
            img[y, x % S] = T.hexc("#5a4a44")
            img[y, (x + 1) % S] = T.hexc("#8a7a70")
    return img


def wall_scrawl(text, seed, scale=2, y=None):
    def f():
        img = wall_base(seed)
        m = draw(np.zeros((S, S, 3)), text, "#ffffff", scale=scale, y=y, jitter_seed=seed)[..., 0]
        drip = np.zeros((S, S))
        r = T.rng(seed, "drip")
        cols = np.where(m.max(axis=0) > 0)[0]
        for x in cols[::3]:
            ys = np.where(m[:, x] > 0)[0]
            if len(ys) and r.random() < 0.6:
                for yd in range(ys.max(), min(S, ys.max() + 3 + int(r.random() * 8))):
                    drip[yd, x] = 0.85
        return T.blend(img, T.solid("#6a0808"), np.clip(m + drip, 0, 1))
    return f


def wall_base_board():
    img = wall_base("rwbb")
    board = T.ramp(T.noise("rwbbn", sigma=1), "#3a3634", "#4a4644")
    img[S - 6:] = board[S - 6:]
    return img


def floor():
    img = T.ramp(T.fbm("rf", octaves=3, base=5) * 0.6 + T.noise("rfn", sigma=0.6) * 0.4, "#6a6a5e", "#7a7a6c")
    tiles = ((xx % 16) == 0) | ((yy % 16) == 0)
    img = T.shade(img, 1 - tiles * 0.2)
    # black skid marks, all pointing the same way
    r = T.rng("rfskid")
    for k in range(3):
        y = int(r.random() * S)
        x0 = int(r.random() * 16)
        for x in range(x0, x0 + 10 + int(r.random() * 10)):
            img[y % S, x % S] *= 0.55
    return T.grain(img, "rf", 0.04)


def floor_drag():
    img = floor()
    m = np.zeros((S, S))
    for y in range(11, 21):
        m[y, :] = 0.75 * (1 - abs(y - 16) / 6.0) * (0.5 + 0.5 * T.noise("rfdrag", sigma=1.2)[y, :])
    return T.blend(img, T.solid("#4a0808"), np.clip(m, 0, 1))


def ceiling():
    img = T.ramp(T.fbm("rc", octaves=2, base=4), "#3a3634", "#46423e")
    return T.grain(T.border(img, "#2a2624", 1), "rc", 0.03)


def red_light():
    img = T.solid("#ff3020")
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#ffb0a0"), np.clip(1 - d / 10, 0, 1))
    cage = (((xx % 6) == 0) | ((yy % 8) == 0)).astype(float) * 0.35
    img = T.shade(img, 1 - cage)
    return T.border(T.border(img, "#5a5250", 2), "#2a2624", 1)


def red_light_off():
    img = T.solid("#4a1410")
    cage = (((xx % 6) == 0) | ((yy % 8) == 0)).astype(float) * 0.35
    return T.border(T.border(T.shade(img, 1 - cage), "#5a5250", 2), "#2a2624", 1)


def exit_light():
    """The way out: a doorway full of white light."""
    d = np.abs(xx - 15.5) / 16
    img = T.ramp(1 - d, "#c8ffd8", "#f4fff6", "#ffffff")
    return img


def exit_sign():
    img = T.solid("#0e2a14")
    img = draw(img, "EXIT", "#7cff9a", scale=1, y=9)
    img = draw(img, ">>", "#7cff9a", scale=2, y=17)
    return T.border(img, "#2a2a2a", 1)


def alarm_box():
    img = T.ramp(T.noise("ralarm", sigma=1), "#b01818", "#c82020")
    img = T.rect(img, 10, 8, 22, 14, "#f0e8e0")
    img = draw(img, "PULL", "#b01818", scale=1, x=9, y=8)
    img = T.rect(img, 13, 16, 19, 24, "#e0d8d0")
    return T.border(img, "#5a0808", 1)


def debris():
    img = T.ramp(T.fbm("rdeb", octaves=3, base=2), "#5a544e", "#8a8278")
    chunks = (T.noise("rdebc", sigma=1.2) > 0.6).astype(float)
    return T.grain(T.shade(img, 1 - chunks * 0.3), "rdeb", 0.08)


def plank():
    img = T.ramp(T.fbm("rpl", octaves=3, base=3), "#6a4a2a", "#8a6a3a")
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.3)
    return T.grain(img, "rpl", 0.05)


def desk_top():
    return T.grain(T.ramp(T.noise("rdesk", sigma=2), "#8a7a62", "#9a8a70"), "rdesk", 0.04)


def metal():
    return T.grain(T.ramp(T.noise("rmet", sigma=1), "#5a5c60", "#7a7c80"), "rmet", 0.04)


def wires():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    for k, (x0, c) in enumerate(((8, "#1a1a1a"), (14, "#8a1a12"), (22, "#1a3a8a"))):
        ln = 18 + k * 5
        for y in range(ln):
            x = int(x0 + 2 * np.sin(y / 4.0 + k))
            rgb[y, x] = T.hexc(c)
            a[y, x] = 1
        rgb[ln:ln + 2, x - 1:x + 2] = T.hexc("#c8b070")
        a[ln:ln + 2, x - 1:x + 2] = 1
    return rgb, a


def door_tex():
    img = T.ramp(T.noise("rdoor", sigma=2), "#5a6a6a", "#667676")
    img = T.border(img, "#2a3434", 2)
    img = T.rect(img, 8, 6, 24, 16, "#101414")
    img = T.rect(img, 23, 18, 27, 21, "#b0b0a8")
    return blood(T.grain(img, "rdoor", 0.05), "rdoor", 0.6, smear=False)


tex("run_wall", wall)
tex("run_wall_blood", wall_blood)
tex("run_wall_scratch", wall_scratch)
tex("run_wall_scrawl_run", wall_scrawl("RUN", "rsc1", 3))
tex("run_wall_scrawl_dontstop", wall_scrawl("DONT", "rsc2", 2, y=6))
tex("run_wall_scrawl_stop", wall_scrawl("STOP", "rsc3", 2, y=18))
tex("run_wall_scrawl_arrow", wall_scrawl(">>>", "rsc4", 2))
tex("run_wall_base", wall_base_board)
tex("run_floor", floor)
tex("run_floor_drag", floor_drag)
tex("run_ceiling", ceiling)
tex("run_red_light", red_light)
tex("run_red_light_off", red_light_off)
tex("run_exit_light", exit_light)
tex("run_exit_sign", exit_sign)
tex("run_alarm_box", alarm_box)
tex("run_debris", debris)
tex("run_plank", plank)
tex("run_desk", desk_top)
tex("run_metal", metal)
tex("run_wires", wires)
tex("run_door", door_tex)

cube("run_corridor_wall", "run_wall", display="Corridor Wall")
cube("run_corridor_wall_bloody", "run_wall_blood", display="Bloodied Corridor Wall")
cube("run_corridor_wall_scratched", "run_wall_scratch", display="Scratched Corridor Wall")
cube("run_corridor_wall_run", "run_wall_scrawl_run", display="RUN")
cube("run_corridor_wall_dont", "run_wall_scrawl_dontstop", display="DONT")
cube("run_corridor_wall_stop", "run_wall_scrawl_stop", display="STOP")
cube("run_corridor_wall_arrow", "run_wall_scrawl_arrow", display="Arrows")
cube("run_corridor_baseboard", "run_wall_base", display="Corridor Baseboard")
cube("run_corridor_floor", "run_floor", display="Corridor Linoleum")
cube("run_corridor_floor_drag", "run_floor_drag", display="Drag-Marked Linoleum")
cube("run_corridor_ceiling", "run_ceiling", display="Corridor Ceiling", flat=True)
flicker_light("run_emergency_light", "run_red_light", "run_red_light_off", "run_ceiling", light=14, flicker=0.0,
              display="Emergency Light")
flicker_light("run_alarm_strobe", "run_red_light", "run_red_light_off", "run_ceiling", light=14, flicker=0.85,
              display="Alarm Strobe")
exit_block("run_exit_light", "run_exit_light", "DOOR", display="The Way Out", light=15, emissive=True)
shaped("run_exit_sign", [(2, 4, 7, 14, 10, 9, {"all": "frame", "north": "sign", "south": "sign"}),
                         (7.5, 10, 7.5, 8.5, 16, 8.5, "frame")],
       {"sign": "run_exit_sign", "frame": "run_ceiling"}, display="Exit Sign", light=8, emissive=True,
       unbreakable=True, collides=False, sound="glass", render="solid")
shaped("run_fire_alarm", [(5, 3, 14, 11, 12, 16, "box")], {"box": "run_alarm_box"}, display="Fire Alarm",
       collides=False, unbreakable=True, sound="metal")
shaped("run_rubble", [
    (0, 0, 0, 16, 5, 16, "deb"), (2, 5, 1, 12, 10, 9, "deb"), (6, 5, 8, 15, 8, 15, "deb"), (4, 10, 3, 9, 13, 7, "deb"),
], {"deb": "run_debris"}, display="Rubble", unbreakable=True, sound="gravel")
shaped("run_barricade", [
    (0, 2, 6, 16, 6, 8, "plank"), (0, 8, 6, 16, 12, 8, "plank"), (2, 0, 8, 4, 13, 10, "plank"), (12, 0, 8, 14, 13, 10, "plank"),
], {"plank": "run_plank"}, display="Barricade", unbreakable=True, sound="wood")
shaped("run_overturned_desk", [
    (0, 0, 10, 16, 16, 13, {"all": "top"}), (1, 2, 2, 3, 4, 10, "metal"), (13, 2, 2, 15, 4, 10, "metal"),
    (1, 11, 2, 3, 13, 10, "metal"), (13, 11, 2, 15, 13, 10, "metal"),
], {"top": "run_desk", "metal": "run_metal"}, display="Overturned Desk", unbreakable=True, sound="wood")
shaped("run_gurney", [
    (0, 7, 2, 16, 9, 14, {"all": "metal", "up": "top"}), (1, 0, 3, 2, 7, 4, "metal"), (14, 0, 3, 15, 7, 4, "metal"),
    (1, 0, 12, 2, 7, 13, "metal"), (14, 0, 12, 15, 7, 13, "metal"),
], {"metal": "run_metal", "top": "run_wall_blood"}, display="Gurney", unbreakable=True, sound="metal")
cross("run_hanging_wires", "run_wires", display="Hanging Wires", sound="metal")
cube("run_locked_door", "run_door", display="Locked Door", sound="metal")

# ------------------------------------------------------------------ the thing behind you

entity_model("run_chaser", humanoid(arm_len=20, leg_len=15,
                                    head_extra=[(32, 0, -4, -9, -4, 8, 9, 8, 0.4)]))


def chaser_tex():
    c = Canvas()
    hide = skin("rch", "#0c0606", "#1e0c0a", sigma=0.6, spots="#4a0a08")

    def face(w, h):
        img = hide(w, h)
        img[2:4, 1:3] = T.hexc("#ff2a1a")
        img[2:4, 5:7] = T.hexc("#ff2a1a")
        # a mouth that opens all the way down
        img[4:8, 3:5] = T.hexc("#e8e0d0")
        img[5:8, 3:5] = T.hexc("#3a0404")
        return img

    paint_humanoid(c, hide, hide, hide, hide, face=face)

    def veil(w, h):
        img = T.ramp(T.noise(f"rveil{w}{h}", shape=(h, w), sigma=0.8), "#050202", "#140806")
        return img
    c.box(32, 0, 8, 9, 8, all=veil, front=lambda w, h: np.zeros((h, w, 3)))
    rgb, a = c.out()
    # the veil is ragged: holes everywhere
    hole = T.noise("rveilh", shape=(64, 64), sigma=0.7) > 0.62
    a[0:17, 32:64] = np.where(hole[0:17, 32:64], 0, a[0:17, 32:64])
    a[8:17, 40:48] = 0   # the hood is open at the front
    return rgb, a


def chaser_glow():
    c = Canvas()

    def eyes(w, h):
        img = np.zeros((h, w, 3))
        img[2:4, 1:3] = T.hexc("#ff3a2a")
        img[2:4, 5:7] = T.hexc("#ff3a2a")
        img[5:8, 3:5] = T.hexc("#ff6a40") * 0.6
        return img
    c.box(0, 0, 8, 8, 8, front=eyes)
    rgb, _ = c.out()
    return rgb, (rgb.sum(axis=2) > 0.05).astype(float)


entity_tex("run_chaser", chaser_tex)
entity_tex("run_chaser_glow", chaser_glow)
lang("entity.noclip.run_chaser", "It")
lang("item.noclip.run_chaser_spawn_egg", "It Spawn Egg")
ITEM_MODELS["run_chaser_spawn_egg"] = {"parent": "minecraft:item/template_spawn_egg"}

# ------------------------------------------------------------------ sound


def alarm_loop():
    """A slow-whoop evacuation alarm two rooms away, the building's rumble, and your own heart."""
    d = 24.0
    t = Y.t_axis(d)
    n = len(t)
    period = 2.0
    ph = (t % period) / period
    f0 = 500 + 700 * np.clip(ph / 0.8, 0, 1)
    gate = (ph < 0.82).astype(float)
    whoop = np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)) * gate * 0.25
    whoop = Y.lp(Y.hp(whoop, 300), 2600)
    rumble = Y.lp(Y.brown(d, "rrum"), 90) * 0.5
    beat = np.zeros(n)
    for k in range(int(d / 0.5)):
        nn = int(0.2 * Y.SR)
        tt = np.arange(nn) / Y.SR
        th = np.sin(2 * np.pi * (60 - 30 * tt / 0.2) * tt) * np.exp(-tt * 25)
        Y.place(beat, th, k * 0.5, 0.8)
        Y.place(beat, th * 0.6, k * 0.5 + 0.17, 0.6)
    x = wrapverb(whoop, 2.2, 0.6, "ralarm", damp=3000) * 0.6 + rumble + Y.lp(beat, 160) * 0.7
    return Y.stereoize(Y.loopable(Y.norm(x, 0.55), 1.0), 0.002), "stereo"


def scream(i):
    def f():
        d = 2.2
        t = Y.t_axis(d)
        f0 = (600 + i * 90) * (1 + 0.12 * np.sin(2 * np.pi * 6 * t)) * (1 - 0.25 * t / d)
        ph = 2 * np.pi * np.cumsum(f0) / Y.SR
        x = np.sin(ph) + 0.6 * np.sin(2 * ph) + 0.4 * np.sin(3 * ph)
        x += Y.bp(Y.white(d, f"rscr{i}"), 1000, 5000) * 0.6
        x *= np.clip(t / 0.08, 0, 1) * np.clip((d - t) / 0.8, 0, 1)
        return Y.reverb(Y.lp(Y.norm(x), 3000), 3.0, 0.75, f"rscr{i}")
    return f


def crash():
    out = np.zeros(int(3.5 * Y.SR))
    Y.place(out, Y.impact("rcrash", 1.2, ring=0.6), 0.0)
    gl = Y.bp(Y.white(1.0, "rglass"), 3000, 12000) * np.exp(-np.arange(int(Y.SR)) / Y.SR * 5)
    Y.place(out, gl, 0.05, 0.6)
    return Y.reverb(out, 2.8, 0.6, "rcrash")


def steps_behind():
    """Something heavy running. Fast. Behind you."""
    out = np.zeros(int(3.0 * Y.SR))
    for k in range(12):
        Y.place(out, Y.footstep(f"rstep{k}", "concrete", 1.0), 0.1 + k * 0.21, 0.4 + k * 0.05)
        Y.place(out, Y.lp(Y.impact(f"rthud{k}", 0.4), 200), 0.1 + k * 0.21, 0.3 + k * 0.04)
    return Y.reverb(out, 1.8, 0.5, "rsteps")


def chaser_roar():
    d = 2.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "rroar"), 120, 2400) * (1 + 0.8 * np.sin(2 * np.pi * 31 * t))
    x += np.sin(2 * np.pi * np.cumsum(70 + 20 * np.sin(2 * np.pi * 3 * t)) / Y.SR) * 0.8
    x *= np.clip(t / 0.1, 0, 1) * np.clip((d - t) / 0.6, 0, 1)
    return Y.norm(Y.reverb(x, 1.2, 0.3, "rroar"), 0.95)


def chaser_step():
    out = np.zeros(int(0.5 * Y.SR))
    Y.place(out, Y.footstep("rcs", "concrete", 1.0), 0.0, 1.0)
    Y.place(out, Y.lp(Y.impact("rcs2", 0.5), 180), 0.0, 0.9)
    return Y.norm(out, 0.9)


def chaser_kill():
    d = 2.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "rkill"), 200, 6000) * np.exp(-t * 2.5)
    x += fit(Y.impact("rkill2", 1.0), len(t)) * 0.8
    return Y.norm(x, 0.95)


def run_sting():
    """The alarm, right in your ear, as you arrive."""
    d = 2.5
    t = Y.t_axis(d)
    x = np.zeros_like(t)
    for f in (880, 932, 988):
        x += np.sign(np.sin(2 * np.pi * f * t)) * 0.2
    x *= np.clip(t / 0.02, 0, 1) * np.exp(-t * 1.2)
    x += Y.lp(Y.white(d, "rsting"), 200) * np.exp(-t * 3) * 0.8
    return Y.norm(Y.reverb(x, 1.5, 0.4, "rsting"), 0.9)


sound("ambient.level_run", [("ambient/level_run", alarm_loop)], stream=True)
sound("ambient.level_run.additions",
      [(f"ambient/level_run_scream{i}", scream(i)) for i in range(2)] +
      [("ambient/level_run_crash", crash), ("ambient/level_run_steps", steps_behind)],
      subtitle="Something behind you")
sound("entity.run_chaser.roar", [("entity/run_chaser_roar", chaser_roar)], subtitle="It roars")
sound("entity.run_chaser.step", [("entity/run_chaser_step", chaser_step)], subtitle="Heavy running")
sound("entity.run_chaser.kill", [("entity/run_chaser_kill", chaser_kill)], subtitle="It caught you")
sound("event.level_run_start", [("event/level_run_start", run_sting)], subtitle="RUN")

level("level_run", fog="#2a0606", sky="#2a0606", ambient="ambient.level_run", additions="ambient.level_run.additions",
      additions_chance=0.006, fixed_time=18000, height=32)
