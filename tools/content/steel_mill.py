"""Steel Mill - a cathedral-sized casting hall in soot and orange glow: ladles, crane girders,
roller tables, sparks in the air. When the siren winds up, molten steel runs down the floor channels."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, exit_block, hazard, level, sound, custom, BLOCK_MODELS
from content.ind_kit import anim_tex, text, vstreaks, rust, steel, concrete, hazard_stripes, rivets, box_model, \
    glow_spot
import tex as T
import synth as Y

S = 32


def soot(img, seed, amount=0.4):
    m = np.clip(T.fbm(seed, octaves=4, base=4) * 1.6 - (1.3 - amount), 0, 1)
    return T.blend(img, T.solid("#0c0a09"), m * 0.85)


def molten(i, n=8):
    """Flowing molten steel: bright cores, darker crust rafts drifting along the flow."""
    yy, xx = np.mgrid[0:S, 0:S]
    shift = int(i * S / n)
    base = T.fbm("smmol", octaves=4, base=4)
    base = np.roll(base, shift, axis=0)
    crust = np.roll(T.noise("smcr", sigma=1.6), shift, axis=0)
    img = T.ramp(base, "#c83000", "#ff7a10", "#ffd060", "#fff4c0")
    img = T.blend(img, T.solid("#5a1404"), np.clip((crust - 0.62) * 4, 0, 1) * 0.85)
    return img


def crust():
    img = T.ramp(T.fbm("smcrust", octaves=4, base=3), "#1a0e08", "#3a1a0c", "#5a2a10")
    cracks = T.cracks(np.zeros((S, S, 3)) + 1, "smcrc", "#000000", 4, 1.0)[..., 0] < 0.5
    return np.where(cracks[..., None], T.solid("#ff6a10"), img)


def refractory():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.fbm("smref", octaves=3, base=3), "#5a3a2a", "#6e4a36")
    mortar = ((yy % 8) == 0) | (((xx + (yy // 8) * 8) % 16) == 0)
    img = T.blend(img, T.solid("#2a1e18"), mortar.astype(float) * 0.8)
    return soot(T.grain(img, "smref", 0.05), "smrefs", 0.45)


def floor_plate():
    yy, xx = np.mgrid[0:S, 0:S]
    img = steel("smfp", "#2e2c2a", "#3e3a36", brushed=False)
    tread = (((xx + yy) % 8 < 2) & ((xx // 4 + yy // 4) % 2 == 0)) | (((xx - yy) % 8 < 2) & ((xx // 4 + yy // 4) % 2 == 1))
    img = T.shade(img, 1 + tread * 0.18)
    return soot(rust(img, "smfpr", 0.25, "#3a2010", "#5a3018"), "smfps", 0.4)


def floor_hazard():
    img = floor_plate()
    return hazard_stripes(img, 0, 32, "smfh", "#c8901a", "#1a1614", 6, 0.35)


def column():
    img = steel("smcol", "#2a2826", "#3a3632", brushed=False)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((np.abs(xx - 4) < 2) | (np.abs(xx - 27) < 2)) * -0.2)
    img = rivets(img, [(x, y) for x in (2, 29) for y in range(2, 32, 6)], "#5a524a", "#0a0806")
    return soot(img, "smcols", 0.5)


def girder():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("smgir", sigma=1.5), "#5a4a1a", "#7a6224")
    web = (np.abs(xx - yy) < 2) | (np.abs(xx + yy - 31) < 2) | (yy < 3) | (yy > 28)
    img = np.where(web[..., None], img, T.shade(img, 0.35))
    return soot(rust(img, "smgirr", 0.3), "smgirs", 0.35)


def ladle_shell():
    img = steel("smlad", "#2a2422", "#3a322c", brushed=False)
    img = rivets(img, [(x, y) for x in range(3, 32, 7) for y in (4, 27)], "#6a5a4a", "#0a0806")
    yy = np.arange(S)[:, None] * np.ones((1, S))
    heat = np.clip(1 - yy / 32.0, 0, 1) * 0.25
    img = T.blend(img, T.solid("#ff5010"), heat * (T.noise("smladh", sigma=2) > 0.5))
    return soot(img, "smlads", 0.3)


def roller():
    yy, xx = np.mgrid[0:S, 0:S]
    img = steel("smrol", "#4a4844", "#6a6660", brushed=True)
    return T.shade(img, 1 - (np.abs(yy - 16) / 16.0) ** 2 * 0.5)


def slab(i):
    img = T.ramp(T.fbm(f"smslab{i % 2}", octaves=3, base=4), "#a02a04", "#e05a10", "#ff9a30")
    scale = (T.noise(f"smsc{i}", sigma=1.2) > 0.6).astype(float)
    return T.blend(img, T.solid("#3a1204"), scale * 0.7)


def machine():
    img = steel("smmac", "#3a4440", "#4a5450", brushed=False)
    img = T.border(img, "#1a2220", 2)
    img = T.rect(img, 6, 6, 26, 14, "#1a1e1c")
    img[9, 9:23] = T.hexc("#ff9a20")
    img = text(img, "PRESS", 6, 18, "#c8c0a8")
    return soot(rust(img, "smmacr", 0.2), "smmacs", 0.3)


def pulpit_glass():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.ramp(T.noise("smpg", sigma=3), "#3a4a3a", "#5a6a5a")
    a = np.full((S, S), 0.45)
    edge = (xx < 2) | (xx > 29) | (yy < 2) | (yy > 29)
    a[edge] = 1
    rgb[edge] = T.hexc("#2a2826")
    return rgb, a


def amber(i, n):
    yy, xx = np.mgrid[0:S, 0:S]
    ang = np.arctan2(yy - 15.5, xx - 15.5)
    sweep = np.cos(ang - 2 * np.pi * i / n) * 0.5 + 0.5
    return T.border(T.ramp(sweep ** 3, "#5a3006", "#ffa01a", "#fff4d0"), "#2a2a2a", 2)


def door():
    img = steel("smdoor", "#3a3a34", "#4a4a42", brushed=False)
    img = T.border(img, "#1a1a16", 2)
    img = T.rect(img, 23, 14, 28, 18, "#8a8070")
    img = T.rect(img, 4, 5, 28, 11, "#c8901a")
    img = text(img, "EXIT", 9, 6, "#1a1610")
    return soot(rust(img, "smdoorr", 0.3), "smdoors", 0.3)


def chute():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("smchute", sigma=2), "#0a0604", "#2a0e04")
    return T.blend(img, T.solid("#ff4a08"), glow_spot((S, S), 16, 16, 18, 2.5) * 0.4)


def lamp():
    return T.border(T.ramp(T.noise("smlamp", sigma=2), "#ffc070", "#ffe8c0"), "#2a2826", 3)


def ceiling():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("smceil", sigma=2), "#0e0c0a", "#1a1614")
    return T.blend(img, T.solid("#2a2420"), ((np.abs(xx - 16) < 2) | (np.abs(yy - 16) < 2)).astype(float))


anim_tex("sm_molten", molten, 8, frametime=3, interpolate=True)
tex("sm_crust", crust)
tex("sm_refractory", refractory)
tex("sm_floor", floor_plate)
tex("sm_floor_hazard", floor_hazard)
tex("sm_column", column)
tex("sm_girder", girder)
tex("sm_ladle", ladle_shell)
tex("sm_roller", roller)
anim_tex("sm_slab", slab, 2, frametime=20, interpolate=True)
tex("sm_machine", machine)
tex("sm_pulpit", pulpit_glass)
anim_tex("sm_amber", lambda i: amber(i, 6), 6, frametime=1)
tex("sm_amber_off", lambda: T.border(T.ramp(T.noise("smambo", sigma=2), "#3a2006", "#4a2a0a"), "#2a2a2a", 2))
tex("sm_door", door)
tex("sm_chute", chute)
tex("sm_lamp", lamp)
tex("sm_ceiling", ceiling)

hazard("sm_molten", "sm_molten", damage=7.0, solid=False, display="Molten Steel", light=15, emissive=True,
       sound="stone")
cube("sm_crust", "sm_crust", display="Cooling Slag", light=6, emissive=True)
hazard("sm_pool", "sm_molten", damage=7.0, solid=False, display="Molten Pool", light=15, emissive=True)
cube("sm_refractory", "sm_refractory", display="Refractory Brick")
cube("sm_floor", "sm_floor", display="Tread Plate", sound="metal")
cube("sm_floor_hazard", "sm_floor_hazard", display="Hazard Plate", sound="metal")
cube("sm_column", "sm_column", display="Steel Column", sound="metal")
cube("sm_girder", "sm_girder", display="Crane Girder", sound="metal")
cube("sm_ladle", "sm_ladle", display="Ladle Shell", sound="metal")
shaped("sm_roller", [(0, 4, 2, 16, 12, 14, "r"), (0, 0, 6, 2, 4, 10, "r"), (14, 0, 6, 16, 4, 10, "r")],
       {"r": "sm_roller"}, display="Roller", sound="metal", unbreakable=True)
hazard("sm_slab", "sm_slab", damage=4.0, display="Red-Hot Slab", light=13, emissive=True)
cube("sm_machine", "sm_machine", display="Press Housing", sound="metal")
cube("sm_pulpit_glass", "sm_pulpit", display="Pulpit Glass", sound="glass", render="translucent")
BLOCK_MODELS["sm_amber_on"] = box_model([(4, 0, 4, 12, 7, 12, "b")], {"b": "sm_amber"})
BLOCK_MODELS["sm_amber_off"] = box_model([(4, 0, 4, 12, 7, 12, "b")], {"b": "sm_amber_off"})
custom("sm_beacon", "com.noclip.block.AlarmBeaconBlock", BLOCK_MODELS["sm_amber_off"],
       blockstate={"variants": {"lit=false": {"model": "noclip:block/sm_amber_off"},
                                "lit=true": {"model": "noclip:block/sm_amber_on"}}},
       display="Pour Beacon", sound="glass", unbreakable=True, render="cutout")
exit_block("sm_door", "sm_door", "DOOR", trigger="USE", solid=True, display="Mill Door", sound="metal")
exit_block("sm_chute", "sm_chute", "FALL", display="Slag Chute", light=8, emissive=True)
cube_faces("sm_lamp", bottom="sm_lamp", top="sm_ceiling", side="sm_ceiling", display="High Bay Lamp", light=14,
           emissive=True, flat=True, sound="glass")
cube("sm_ceiling", "sm_ceiling", display="Roof", flat=True)
shaped("sm_hook", [(5, 0, 5, 11, 10, 11, "g"), (7, 10, 7, 9, 16, 9, "g")], {"g": "sm_girder"},
       display="Crane Hook", sound="metal", unbreakable=True, collides=False)

# ------------------------------------------------------------------ sound


def furnace_roar():
    d = 26.0
    t = Y.t_axis(d)
    roar = Y.lp(Y.brown(d, "smroar"), 180) * 1.0
    flame = Y.bp(Y.pink(d, "smflame"), 100, 1200) * 0.35 * (0.8 + 0.2 * np.sin(2 * np.pi * t / 7))
    arc = Y.bp(Y.white(d, "smarc"), 2000, 9000) * (Y.rng("smarc2").random(len(t)) > 0.995) * 0.6
    arc = Y.lp(arc, 8000)
    hum = Y.hum(d, 50.0, "smhum", buzz=0.2, harmonics=(1, 2, 3, 6)) * 0.08
    hammer = np.zeros(len(t))
    for k in range(int(d / 1.6)):
        Y.place(hammer, Y.impact(f"smham{k}", 1.0) * 0.35, 0.4 + k * 1.6)
    x = roar + flame + arc + Y.lp(hum, 600) + Y.lp(hammer, 300)
    return Y.stereoize(Y.loopable(Y.norm(Y.reverb(x, 3.5, 0.35, "smhall")[:len(t)], 0.6), 2.0), 0.003), "stereo"


def crash(i):
    def f():
        out = np.zeros(int(4.0 * Y.SR))
        r = Y.rng(f"smcrash{i}")
        for k in range(3 + i):
            Y.place(out, Y.impact(f"smcr{i}{k}", 0.8 + r.random() * 0.6, ring=0.9), 0.05 + k * 0.12 + r.random() * 0.05, 0.6 + r.random() * 0.4)
        return Y.norm(Y.reverb(out, 4.0, 0.6, f"smcrash{i}"), 0.8)
    return f


def crane_horn():
    d = 3.0
    t = Y.t_axis(d)
    x = sum(np.sin(2 * np.pi * f * t) * g for f, g in ((311, 1.0), (466, 0.7), (622, 0.3)))
    x = np.sign(x) * 0.5 * ((t % 1.0) < 0.6)
    return Y.norm(Y.reverb(Y.lp(x, 2500), 4.0, 0.7, "smhorn"), 0.5)


def quench():
    d = 3.5
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "smq"), 1500, 10000) * np.exp(-t * 1.2) * (1 - np.exp(-t * 30))
    x += Y.bp(Y.white(d, "smq2"), 200, 900) * np.exp(-t * 2) * 0.5 * (Y.rng("smq3").random(len(t)) > 0.6)
    return Y.norm(Y.reverb(x, 2.5, 0.5, "smq"), 0.6)


def siren():
    d = 6.0
    t = Y.t_axis(d)
    f0 = 300 + 500 * np.clip(np.sin(np.pi * t / 3.0), 0, None)
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) + 0.4 * np.sin(2 * np.pi * np.cumsum(f0 * 2) / Y.SR)
    x *= np.clip(t / 0.5, 0, 1) * np.clip((d - t) / 1.0, 0, 1)
    return Y.norm(Y.reverb(x, 4.0, 0.6, "smsiren"), 0.8)


def pour():
    d = 5.0
    t = Y.t_axis(d)
    rumble = Y.lp(Y.brown(d, "smpour"), 150) * np.clip(t / 1.0, 0, 1) * 1.4
    glug = Y.bp(Y.white(d, "smglug"), 200, 1200) * (0.5 + 0.5 * np.sin(2 * np.pi * 4 * t)) * 0.6
    sizzle = Y.bp(Y.white(d, "smsiz"), 3000, 10000) * 0.25 * (Y.rng("smsiz2").random(len(t)) > 0.5)
    groan = np.sin(2 * np.pi * np.cumsum(70 - 20 * t / d) / Y.SR) * 0.4 * np.exp(-t * 0.5)
    return Y.norm(Y.reverb(rumble + glug + sizzle + groan, 3.0, 0.5, "smpour"), 0.9)


def sizzle():
    d = 2.0
    t = Y.t_axis(d)
    x = Y.bp(Y.white(d, "smsz"), 2500, 11000) * (Y.rng("smsz2").random(len(t)) > 0.4) * np.sin(np.pi * t / d)
    x += Y.lp(Y.brown(d, "smsz3"), 200) * np.sin(np.pi * t / d) * 0.6
    return Y.norm(x, 0.7)


sound("ambient.steel_mill", [("ambient/steel_mill", furnace_roar)], stream=True)
sound("ambient.steel_mill.additions",
      [(f"ambient/steel_mill_crash{i}", crash(i)) for i in range(2)] +
      [("ambient/steel_mill_horn", crane_horn), ("ambient/steel_mill_quench", quench)],
      subtitle="The mill clangs")
sound("event.steel_mill_siren", [("event/steel_mill_siren", siren)], subtitle="The pour siren winds up")
sound("event.steel_mill_pour", [("event/steel_mill_pour", pour)], subtitle="A ladle tips")
sound("event.steel_mill_sizzle", [("event/steel_mill_sizzle", sizzle)], subtitle="Molten steel runs")

level("steel_mill", fog="#2a140a", sky="#2a140a", ambient="ambient.steel_mill", additions="ambient.steel_mill.additions",
      additions_chance=0.006, fixed_time=18000, height=64, particle="minecraft:lava", particle_chance=0.0015)
