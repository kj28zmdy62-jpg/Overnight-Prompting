"""Assembly Line - a factory that makes office chairs forever. Belts, robot arms, presses,
finished chairs stacked to the trusses. The line pulls you along if you step on it.
Something on the line is not a chair."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, level, sound, custom
from content.ind_kit import anim_tex, text, rivets, steel, concrete, hazard_stripes, rust, vstreaks, box_model, \
    facing_states
import tex as T
import synth as Y

S = 32


# ------------------------------------------------------------------ textures

def floor(seed="asmf"):
    img = concrete(seed, "#4a4844", "#5e5b55", stain="#2a2622", stains=3, crack=1)
    img = T.stains(img, seed + "oil", "#1a1816", count=1, strength=0.5, size=6)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - (((xx % 32) == 0) | ((yy % 32) == 0)) * 0.18)
    return img


def floor_line():
    img = floor("asmfl")
    yy, xx = np.mgrid[0:S, 0:S]
    paint = T.ramp(T.noise("asmflp", sigma=1), "#b8901c", "#d8ac2a")
    worn = (T.noise("asmflw", sigma=0.7) > 0.3).astype(float)
    return T.blend(img, paint, ((yy >= 12) & (yy < 20)) * worn * 0.9)


def belt(i):
    """Black rubber belt with ribs, scrolling south (towards +z in the model) frame by frame."""
    yy, xx = np.mgrid[0:S, 0:S]
    base = T.ramp(T.noise("asmbelt", sigma=1.2), "#141414", "#232322")
    ribs = (((yy - i * 2) % 8) < 2)
    img = T.shade(base, 1 + ribs * 0.35)
    wear = T.noise("asmbw", sigma=2)
    img = T.blend(img, T.solid("#3a3632"), np.roll(wear, i * 2, axis=0) * 0.25)
    img[:, :2] = T.hexc("#5a5e62")
    img[:, 30:] = T.hexc("#5a5e62")
    return img


def belt_side():
    img = steel("asmbs", "#3c4146", "#545a60")
    yy, xx = np.mgrid[0:S, 0:S]
    img[:6] = T.hexc("#1a1a1a")
    for cx in (8, 24):
        d = np.sqrt((xx - cx) ** 2 + (yy - 14) ** 2)
        img = T.blend(img, T.solid("#7a8086"), ((d < 5) & (d > 3)).astype(float))
        img = T.blend(img, T.solid("#22262a"), (d <= 3).astype(float))
    img = hazard_stripes(img, 24, 32, "asmbsh", width=4)
    return img


def robot(seed="asmr"):
    img = T.ramp(T.fbm(seed, octaves=3, base=4), "#c4561a", "#e06a22")
    img = T.stains(img, seed + "g", "#3a2a1a", count=2, strength=0.4, size=6)
    img = rivets(img, [(3, 3), (28, 3), (3, 28), (28, 28)], "#f0a060", "#5a2a0a")
    return T.grain(T.border(img, "#7a3410", 1), seed, 0.04)


def robot_joint():
    img = steel("asmrj", "#2a2c30", "#3e4248")
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.blend(img, T.solid("#6a6e74"), ((d < 9) & (d > 6)).astype(float))
    return img


def welder():
    img = T.ramp(T.noise("asmwd", sigma=1), "#e8f4ff", "#ffffff")
    return T.border(img, "#7ab0ff", 3)


def press(seed="asmp"):
    img = steel(seed, "#4c5258", "#646a72", brushed=True)
    img = rust(img, seed + "r", 0.2)
    img = hazard_stripes(img, 0, 3, seed + "h", width=3)
    img = vstreaks(img, seed + "v", "#1a1612", 3, 0.4)
    return img


def column():
    img = steel("asmcol", "#2c4a6a", "#3a5a7e", brushed=False)
    img = T.shade(img, 1 - ((np.abs(np.mgrid[0:S, 0:S][1] - 16) < 2) * 0.25))
    return T.stains(img, "asmcols", "#1a2a3a", 2, 0.3, 6)


def ceiling():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("asmc", sigma=2), "#2a2c2e", "#3a3c3e")
    img = T.shade(img, 1 - ((xx % 8) < 2) * 0.35)
    return img


def lamp():
    yy, xx = np.mgrid[0:S, 0:S]
    d = np.sqrt((xx - 15.5) ** 2 + (yy - 15.5) ** 2)
    img = T.ramp(np.clip(1 - d / 15, 0, 1), "#a06018", "#ffc070", "#fff4d8")
    return T.blend(img, T.solid("#2a2a2a"), (d > 14).astype(float))


def lamp_shade():
    return steel("asmls", "#3a3e42", "#4e5256", brushed=False)


def wrapped():
    """Pale shape under stretched shrink-wrap, creases, a face pressed up against the plastic."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("asmwr", sigma=2.5), "#b8aea0", "#d8cec2")
    folds = np.sin((xx + T.noise("asmwrf", sigma=3) * 10) / 2.3) * 0.5 + 0.5
    img = T.shade(img, 0.85 + folds * 0.2)
    spec = (T.noise("asmwrs", sigma=1.5) > 0.82).astype(float)
    img = T.blend(img, T.solid("#ffffff"), spec * 0.5)
    return T.grain(img, "asmwr", 0.03)


def wrapped_face():
    img = wrapped()
    for cx in (11, 20):
        img = T.rect(img, cx - 2, 11, cx + 2, 14, "#6a5e52", 0.8)
    img = T.rect(img, 12, 20, 20, 23, "#5a4e44", 0.7)
    img = T.rect(img, 14, 21, 18, 22, "#2a2220", 0.9)
    return img


def bin_tex():
    img = T.ramp(T.noise("asmbin", sigma=1.5), "#1e4a8a", "#2a5aa0")
    img = T.border(img, "#123060", 2)
    img = T.rect(img, 6, 4, 26, 9, "#d0d0d0")
    img = text(img, "BASE", 8, 4, "#202020")
    return T.grain(img, "asmbin", 0.03)


def bin_top():
    img = T.ramp(T.noise("asmbint", sigma=1), "#141414", "#242424")
    r = T.rng("asmcast")
    for _ in range(14):
        x, y = int(r.random() * 26) + 3, int(r.random() * 26) + 3
        img = T.rect(img, x - 2, y - 2, x + 2, y + 2, "#2a2a2c")
        img[y, x] = T.hexc("#5a5a5c")
    return T.border(img, "#123060", 2)


def counter(i):
    img = T.ramp(T.noise("asmcnt", sigma=1), "#0a0a0a", "#141414")
    img = T.border(img, "#3a3a3a", 1)
    img = text(img, "UNITS", 2, 3, "#c0c0c0")
    n = 4217 + i * 3
    img = text(img, f"{n % 10000:04d}", 2, 12, "#ff3a20", 1)
    img = text(img, "TODAY", 2, 22, "#7a7a7a")
    return img


def shipping_door():
    img = steel("asmsd", "#5a5e44", "#6e7254", brushed=False)
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - ((yy % 4) == 0) * 0.25)
    img = T.rect(img, 3, 3, 29, 11, "#1a1a1a")
    img = text(img, "SHIP", 8, 5, "#e0c040")
    img = T.rect(img, 22, 18, 27, 21, "#2a2a2a")
    return T.border(img, "#2a2c20", 2)


def booth_wall():
    img = T.ramp(T.noise("asmbw2", sigma=2), "#8a8e84", "#9ea294")
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.15)
    return T.grain(T.stains(img, "asmbw2s", "#5a5a50", 2, 0.3, 7), "asmbw2", 0.03)


def pallet():
    img = T.ramp(T.noise("asmpal", sigma=1.2), "#8a6a40", "#a88250")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - (((xx % 11) < 2)) * 0.5)
    return T.grain(img, "asmpal", 0.05)


tex("asm_floor", lambda: floor())
tex("asm_floor_line", floor_line)
anim_tex("asm_belt", belt, 4, frametime=2)
tex("asm_belt_side", belt_side)
tex("asm_robot", lambda: robot())
tex("asm_robot_joint", robot_joint)
tex("asm_welder", welder)
tex("asm_press", lambda: press())
tex("asm_column", column)
tex("asm_ceiling", ceiling)
tex("asm_lamp", lamp)
tex("asm_lamp_shade", lamp_shade)
tex("asm_wrapped", wrapped)
tex("asm_wrapped_face", wrapped_face)
tex("asm_bin", bin_tex)
tex("asm_bin_top", bin_top)
anim_tex("asm_counter", counter, 8, frametime=30)
tex("asm_shipping_door", shipping_door)
tex("asm_booth_wall", booth_wall)
tex("asm_pallet", pallet)

cube("asm_floor", "asm_floor", display="Factory Floor")
cube("asm_floor_line", "asm_floor_line", display="Safety Line")
# conveyor: a full block, belt on top; the belt scrolls toward the block's back (AsmBeltBlock.runs)
custom("asm_belt", "com.noclip.block.AsmBeltBlock",
       box_model([(0, 0, 0, 16, 16, 16, {"all": "side", "up": "belt"})], {"belt": "asm_belt", "side": "asm_belt_side"}),
       blockstate=facing_states(lambda v: "asm_belt"), display="Conveyor", sound="metal", unbreakable=True)
shaped("asm_arm_base", [(2, 0, 2, 14, 4, 14, "j"), (5, 4, 5, 11, 32, 11, "o")],
       {"o": "asm_robot", "j": "asm_robot_joint"}, display="Robot Arm", sound="metal", unbreakable=True)
shaped("asm_arm_head", [(5, 0, 5, 11, 4, 11, "j"), (6, 3, -10, 10, 7, 11, "o"), (6, -2, -12, 10, 4, -8, "o"),
                        (7, -4, -11, 9, -2, -9, "w")],
       {"o": "asm_robot", "j": "asm_robot_joint", "w": "asm_welder"}, display="Robot Arm", sound="metal",
       unbreakable=True, light=6, collides=False)
cube("asm_press", "asm_press", display="Hydraulic Press")
shaped("asm_press_head", [(0, 6, 0, 16, 16, 16, "p"), (2, 0, 2, 14, 6, 14, "p")], {"p": "asm_press"},
       display="Press Ram", sound="metal", unbreakable=True)
cube("asm_column", "asm_column", display="Steel Column", sound="metal")
cube("asm_ceiling", "asm_ceiling", display="Roof Deck", flat=True, sound="metal")
shaped("asm_lamp", [(3, 10, 3, 13, 16, 13, {"all": "shade", "down": "lamp"}), (7, 16, 7, 9, 16, 9, "shade")],
       {"lamp": "asm_lamp", "shade": "asm_lamp_shade"}, display="High-Bay Lamp", sound="metal", unbreakable=True,
       light=15, emissive=True, collides=False)
shaped("asm_wrapped", [(3, 0, 3, 13, 9, 13, "w"), (4, 9, 9, 12, 20, 14, "w"), (5, 20, 6, 11, 27, 12, {"all": "w", "north": "f"}),
                       (2, 9, 4, 4, 17, 11, "w"), (12, 9, 4, 14, 17, 11, "w")],
       {"w": "asm_wrapped", "f": "asm_wrapped_face"}, display="Unit", sound="wool", unbreakable=True)
shaped("asm_bin", [(1, 0, 1, 15, 12, 15, {"all": "bin", "up": "top"})], {"bin": "asm_bin", "top": "asm_bin_top"},
       display="Parts Bin", sound="wood", unbreakable=True)
cube_faces("asm_counter", top="asm_column", bottom="asm_column", side="asm_counter", display="Unit Counter",
           light=4, emissive=True, sound="metal")
exit_block("asm_shipping_door", "asm_shipping_door", "DOOR", trigger="USE", solid=True, display="Shipping Door",
           sound="metal")
cube("asm_booth_wall", "asm_booth_wall", display="Office Partition")
shaped("asm_pallet", [(0, 0, 0, 16, 3, 16, "p")], {"p": "asm_pallet"}, display="Pallet", sound="wood",
       unbreakable=True)


# ------------------------------------------------------------------ sound

def line_loop():
    d = 24.0
    t = Y.t_axis(d)
    rumble = Y.lp(Y.brown(d, "asmrum"), 160) * 0.9
    hum = Y.hum(d, 50, "asmhum", buzz=0.15) * 0.18
    rattle = Y.bp(Y.pink(d, "asmrat"), 400, 2500) * 0.15 * (0.7 + 0.3 * np.sin(2 * np.pi * t * 3))
    out = rumble + hum + rattle
    # the cycle: every 1.5 s a clank, every 3 s a pneumatic hiss, every 6 s a distant stamp
    for k in range(int(d / 1.5)):
        Y.place(out, Y.impact(f"asmcl{k % 3}", 0.3, ring=0.35) * 0.35, k * 1.5 + 0.1, 1.0)
    for k in range(int(d / 3)):
        n = int(0.4 * Y.SR)
        h = Y.bp(Y.white(0.4, f"asmhs{k}"), 2500, 9000)[:n] * Y.decay(n, 0.12)
        Y.place(out, h * 0.3, k * 3 + 0.8, 1.0)
    for k in range(int(d / 6)):
        Y.place(out, Y.lp(Y.impact(f"asmst{k}", 1.4), 400) * 0.6, k * 6 + 2.2, 1.0)
    out = Y.reverb(out, 2.2, 0.35, "asmrv")
    return Y.stereoize(Y.loopable(Y.norm(out, 0.6), 1.0), 0.003), "stereo"


def servo():
    d = 1.6
    t = Y.t_axis(d)
    f = 400 + 900 * np.clip(t / 0.6, 0, 1) - 500 * np.clip((t - 0.9) / 0.5, 0, 1)
    x = np.sign(np.sin(2 * np.pi * np.cumsum(f) / Y.SR)) * 0.12 + np.sin(2 * np.pi * np.cumsum(f * 2) / Y.SR) * 0.1
    x = Y.lp(x, 3000) * Y.env(len(t), 0.05, 0.3)
    Y.place(x, Y.impact("asmsvk", 0.2) * 0.4, 1.4, 1.0)
    return Y.norm(Y.reverb(x, 1.5, 0.35, "asmsv"), 0.5)


def wrench():
    out = np.zeros(int(1.4 * Y.SR))
    for k in range(3):
        n = int(0.18 * Y.SR)
        tt = np.arange(n) / Y.SR
        c = (np.sign(np.sin(2 * np.pi * 95 * tt)) * 0.3 + Y.bp(Y.white(0.18, f"asmwr{k}"), 1000, 6000)[:n]) * Y.env(n, 0.005, 0.03)
        Y.place(out, c, 0.1 + k * 0.28, 1.0)
    return Y.norm(Y.reverb(out, 1.6, 0.4, "asmwrr"), 0.5)


def casters():
    d = 2.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "asmca"), 1500, 7000) * (0.5 + 0.5 * np.sin(2 * np.pi * 13 * t)) * Y.env(len(t), 0.2, 0.8) * 0.4
    Y.place(x, Y.impact("asmcak", 0.5, ring=0.2), 2.0, 0.7)
    return Y.norm(Y.reverb(x, 2.5, 0.5, "asmcar"), 0.45)


def pa_chime():
    d = 3.0
    t = Y.t_axis(d)
    x = np.zeros(len(t))
    for k, f in enumerate((659, 523, 392)):
        n = int(1.4 * Y.SR)
        tt = np.arange(n) / Y.SR
        Y.place(x, (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(2 * np.pi * f * 2 * tt)) * np.exp(-tt * 3), k * 0.45, 0.4)
    return Y.norm(Y.reverb(Y.bp(x, 300, 3500), 3.0, 0.6, "asmpa"), 0.4)


def horn():
    d = 3.5
    t = Y.t_axis(d)
    x = sum(np.sign(np.sin(2 * np.pi * f * t)) * g for f, g in ((155, 0.4), (233, 0.3), (311, 0.15)))
    x = Y.lp(x, 2200) * Y.env(len(t), 0.08, 0.6)
    return Y.norm(Y.reverb(x, 3.0, 0.5, "asmhorn"), 0.9)


def stamp():
    out = np.zeros(int(1.6 * Y.SR))
    n = int(0.25 * Y.SR)
    Y.place(out, Y.bp(Y.white(0.25, "asmpsh"), 1500, 8000)[:n] * Y.decay(n, 0.08) * 0.5, 0.0, 1.0)
    Y.place(out, Y.impact("asmpst", 1.2, ring=0.4), 0.22, 1.0)
    return Y.norm(Y.reverb(out, 1.6, 0.35, "asmpstr"), 0.9)


def breath():
    d = 3.0
    t = Y.t_axis(d)
    crinkle = Y.bp(Y.white(d, "asmcr"), 3000, 10000) * (Y.rng("asmcr2").random(len(t)) > 0.995) * 1.5
    inhale = Y.bp(Y.pink(d, "asmin"), 300, 1800) * np.clip(np.sin(np.pi * t / 1.4), 0, 1) ** 2 * (t < 1.4)
    exhale = Y.bp(Y.pink(d, "asmex"), 200, 1200) * np.clip(np.sin(np.pi * (t - 1.6) / 1.2), 0, 1) ** 2 * (t > 1.6)
    return Y.norm(Y.lp(crinkle, 9000) * 0.3 + inhale * 0.6 + exhale * 0.7, 0.6)


sound("ambient.assembly_line", [("ambient/assembly_line", line_loop)], stream=True)
sound("ambient.assembly_line.additions",
      [("ambient/assembly_line_servo", servo), ("ambient/assembly_line_wrench", wrench),
       ("ambient/assembly_line_casters", casters), ("ambient/assembly_line_pa", pa_chime)],
      subtitle="The line keeps working")
sound("event.assembly_line_horn", [("event/assembly_line_horn", horn)], subtitle="Shift horn")
sound("event.assembly_line_stamp", [("event/assembly_line_stamp", stamp)], subtitle="Press stamps")
sound("event.assembly_line_breath", [("event/assembly_line_breath", breath)], subtitle="Plastic crinkles, breathing")

level("assembly_line", fog="#2a2620", sky="#2a2620", ambient="ambient.assembly_line",
      additions="ambient.assembly_line.additions", additions_chance=0.008, fixed_time=18000, height=48)
