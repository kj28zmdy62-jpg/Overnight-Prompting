"""Construction Site - an unfinished high-rise at night that never ends in any direction:
bare slabs, columns, rebar, plastic sheeting, tripod work lights, tower cranes against
the sky. When the whistle blows, get under a slab."""
import numpy as np
from assets import cube, cube_faces, cross, tex, shaped, exit_block, level, sound, pane, stairs
from content.ind_kit import anim_tex, text, rivets, steel, concrete, hazard_stripes, rust, vstreaks, glow_spot
import tex as T
import synth as Y

S = 32


def slab_side():
    img = concrete("conss", "#7e7c76", "#908e86", stain="#4e4c46", stains=2)
    yy, xx = np.mgrid[0:S, 0:S]
    # board-marked formwork finish
    img = T.shade(img, 1 - ((yy % 8) == 0) * 0.12 - ((xx % 32) == 0) * 0.1)
    return vstreaks(img, "conssv", "#4a4840", 3, 0.3)


def slab_top():
    img = concrete("const", "#76746e", "#88867e", stain="#3e3c36", stains=3)
    yy, xx = np.mgrid[0:S, 0:S]
    chalk = (np.abs(yy - 22) < 1) & (T.noise("constc", sigma=0.6) > 0.3)
    img = T.blend(img, T.solid("#3a6ac0"), chalk.astype(float) * 0.5)
    return T.stains(img, "constw", "#4a4a46", 2, 0.3, 8)


def column():
    img = concrete("consc", "#8a8880", "#9a988e", stains=1)
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - ((xx % 16) == 0) * 0.12)
    for (x, y) in ((8, 8), (24, 8), (8, 24), (24, 24)):
        img = T.rect(img, x - 1, y - 1, x + 1, y + 1, "#3a3a36")
    return img


def rebar():
    rgb = np.zeros((S, S, 3))
    a = np.zeros((S, S))
    for x in (6, 15, 24):
        col = rust(steel(f"consrb{x}", "#4a3a2e", "#6a4a32"), f"consrbr{x}", 0.6)
        h = 18 + (x * 7) % 12
        for y in range(S - h, S):
            rgb[y, x:x + 2] = col[y, x:x + 2]
            a[y, x:x + 2] = 1
            if y % 3 == 0:
                rgb[y, x - 1 if x > 0 else 0] = col[y, x] * 0.8
                a[y, x - 1 if x > 0 else 0] = 1
        rgb[S - h:S - h + 2, x - 1:x + 3] = T.hexc("#e06a1a")
        a[S - h:S - h + 2, x - 1:x + 3] = 1
    return rgb, a


def mesh():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = rust(steel("consm", "#4a3a2e", "#6a4a32"), "consmr", 0.6)
    a = (((xx % 8) < 1) | ((yy % 8) < 1)).astype(float)
    return rgb, a


def plywood():
    img = T.ramp(T.fbm("consp", octaves=3, base=6), "#9a5a2a", "#b06a32")
    yy, xx = np.mgrid[0:S, 0:S]
    grain_ = np.sin(xx / 1.7 + T.noise("conspg", sigma=3) * 6) * 0.5 + 0.5
    img = T.shade(img, 0.9 + grain_ * 0.12)
    img = T.border(img, "#5a3418", 1)
    img = T.stains(img, "consps", "#4a4a46", 2, 0.4, 6)
    return T.grain(img, "consp", 0.04)


def scaffold():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.ramp(T.noise("consscf", sigma=1), "#7a7e82", "#a0a4a8")
    a = ((xx < 3) | (xx > 28) | (np.abs(yy - 4) < 2) | (np.abs(xx - yy) < 2)).astype(float)
    rgb[np.abs(yy - 4) < 2] = T.hexc("#c8a030")
    return rgb, a


def board():
    img = T.ramp(T.noise("consbd", sigma=1.5), "#7a6040", "#8e7048")
    yy = np.mgrid[0:S, 0:S][0]
    img = T.shade(img, 1 - ((yy % 11) == 0) * 0.4)
    img[:, :3] = T.hexc("#3a3a3a")
    return T.grain(img, "consbd", 0.05)


def sheeting(i):
    yy, xx = np.mgrid[0:S, 0:S]
    wave = np.sin((xx + i * 4) / 4.0 + yy / 9.0) * 0.5 + 0.5
    rgb = T.ramp(wave, "#a8b4bc", "#e0e8ec")
    a = 0.45 + wave * 0.15
    torn = (T.noise("conssht", sigma=2) > 0.8)
    a = np.where(torn, 0.0, a)
    return rgb, a


def mud():
    img = T.ramp(T.fbm("consmd", octaves=4, base=4), "#2e2618", "#423826")
    tracks = (np.abs(np.mgrid[0:S, 0:S][1] % 16 - 8) < 2) & (np.mgrid[0:S, 0:S][0] % 4 < 2)
    img = T.shade(img, 1 - tracks * 0.2)
    wet = (T.noise("consmdw", sigma=2.5) > 0.7).astype(float)
    return T.grain(T.blend(img, T.solid("#1a1a1c"), wet * 0.5), "consmd", 0.05)


def worklight_lamp():
    g = glow_spot((S, S), 15.5, 15.5, 15, 0.9)
    img = T.ramp(g, "#806020", "#fff0c0", "#ffffff")
    return T.border(img, "#2a2a2a", 2)


def worklight_body():
    return T.border(T.ramp(T.noise("conswb", sigma=1), "#c8a020", "#e0b82a"), "#5a4a10", 1)


def crane():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = rust(T.ramp(T.noise("conscr", sigma=1), "#c8a018", "#e0b820"), "conscrr", 0.15)
    a = ((xx < 3) | (xx > 28) | (yy < 3) | (yy > 28) | (np.abs(xx - yy) < 2)).astype(float)
    return rgb, a


def cab():
    img = T.ramp(T.noise("conscb", sigma=1), "#c8a018", "#e0b820")
    img = T.rect(img, 3, 4, 29, 18, "#1a2a34")
    img = T.rect(img, 4, 5, 12, 10, "#4a6a7a", 0.6)
    return T.border(img, "#5a4a10", 1)


def counterweight():
    img = concrete("conscw", "#5a5a56", "#6a6a64", stains=1)
    img = text(img, "4T", 10, 12, "#e0e0e0", 2)
    return T.border(img, "#2a2a28", 1)


def beacon(i):
    on = i < 3
    g = glow_spot((S, S), 15.5, 15.5, 15, 1.0)
    img = T.ramp(g, "#ff2010", "#ff9080", "#ffffff") if on else T.ramp(g, "#3a0806", "#5a100c")
    return T.border(img, "#1a1a1a", 2)


def barrier():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = np.where((((xx + yy) // 6) % 2 == 0)[..., None], T.solid("#d02020"), T.solid("#e8e8e0"))
    rgb = T.grain(rgb, "consbr", 0.04)
    a = (((yy > 4) & (yy < 10)) | ((yy > 18) & (yy < 22)) | (np.abs(xx - 16) < 2) & (yy > 4)).astype(float)
    return rgb, a


def blocks_pallet():
    img = T.ramp(T.noise("consbp", sigma=1), "#8a8a84", "#9e9e96")
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.shade(img, 1 - (((yy % 8) == 0) | (((xx + (yy // 8) * 8) % 16) == 0)) * 0.3)
    img[26:, :] = T.hexc("#8a6a40")
    img[26:, ::6] = T.hexc("#4a3a20")
    return T.grain(img, "consbp", 0.04)


def rubble():
    img = T.ramp(T.noise("consru", sigma=0.8), "#4a4842", "#7a786e", "#8e8c82")
    return T.grain(T.cracks(img, "consruc", "#1a1a18", 3, 0.7), "consru", 0.06)


def sign():
    img = T.solid("#1a5aa0")
    img = T.border(img, "#ffffff", 1)
    img = text(img, "HARD", 8, 4, "#ffffff")
    img = text(img, "HATS", 8, 11, "#ffffff")
    img = text(img, "MUST", 8, 18, "#ffffff")
    img = text(img, "BE WORN", 2, 25, "#ffffff")
    return img


def hoist():
    img = steel("conshs", "#3a4a3a", "#4a5a4a", brushed=False)
    img = T.rect(img, 6, 4, 26, 28, "#1a1a1a")
    img = T.rect(img, 8, 6, 24, 13, "#2a8a3a")
    img = text(img, "UP", 12, 7, "#ffffff")
    img = T.rect(img, 8, 17, 24, 24, "#8a2a2a")
    return T.border(img, "#c8a020", 2)


def door():
    img = T.ramp(T.noise("consdr", sigma=1.5), "#5a6a7a", "#6a7a8a")
    img = T.border(img, "#2a323a", 2)
    img = T.rect(img, 4, 4, 28, 12, "#e8e4d8")
    img = text(img, "SITE", 8, 5, "#1a1a1a")
    img = T.rect(img, 22, 17, 26, 20, "#1a1a1a")
    return img


tex("cons_slab_side", slab_side)
tex("cons_slab_top", slab_top)
tex("cons_column", column)
tex("cons_rebar", rebar)
tex("cons_mesh", mesh)
tex("cons_plywood", plywood)
tex("cons_scaffold", scaffold)
tex("cons_board", board)
anim_tex("cons_sheeting", sheeting, 8, frametime=3, alpha=True)
tex("cons_mud", mud)
tex("cons_worklight_lamp", worklight_lamp)
tex("cons_worklight_body", worklight_body)
tex("cons_crane", crane)
tex("cons_cab", cab)
tex("cons_counterweight", counterweight)
anim_tex("cons_beacon", beacon, 6, frametime=5)
tex("cons_barrier", barrier)
tex("cons_blocks_pallet", blocks_pallet)
tex("cons_rubble", rubble)
tex("cons_sign", sign)
tex("cons_hoist", hoist)
tex("cons_door", door)

cube_faces("cons_slab", top="cons_slab_top", bottom="cons_slab_side", side="cons_slab_side", display="Floor Slab")
cube("cons_column", "cons_column", display="Concrete Column")
stairs("cons_stairs", "cons_slab_top", "cons_slab", display="Concrete Stairs")
cross("cons_rebar", "cons_rebar", display="Rebar Starters", sound="metal")
shaped("cons_mesh", [(0, 0, 0, 16, 1, 16, "m")], {"m": "cons_mesh"}, display="Rebar Mesh", sound="metal",
       unbreakable=True)
cube("cons_plywood", "cons_plywood", display="Formwork", sound="wood")
pane("cons_scaffold", "cons_scaffold", display="Scaffold Frame", sound="metal")
shaped("cons_board", [(0, 13, 0, 16, 16, 16, "b")], {"b": "cons_board"}, display="Scaffold Board", sound="wood",
       unbreakable=True)
pane("cons_sheeting", "cons_sheeting", display="Plastic Sheeting", sound="wool", render="translucent")
cube("cons_mud", "cons_mud", display="Site Mud", sound="mud")
shaped("cons_worklight", [(7, 0, 7, 9, 22, 9, "b"), (2, 0, 7, 4, 1, 9, "b"), (12, 0, 7, 14, 1, 9, "b"),
                          (7, 0, 2, 9, 1, 4, "b"), (3, 22, 5, 13, 30, 9, {"all": "b", "south": "l"})],
       {"b": "cons_worklight_body", "l": "cons_worklight_lamp"}, display="Work Light", sound="metal",
       unbreakable=True, light=15, emissive=True, collides=False)
cube("cons_crane", "cons_crane", display="Crane Lattice", sound="metal", render="cutout")
cube("cons_cab", "cons_cab", display="Crane Cab", sound="metal")
cube("cons_counterweight", "cons_counterweight", display="Counterweight")
shaped("cons_beacon", [(5, 0, 5, 11, 6, 11, "b")], {"b": "cons_beacon"}, display="Crane Light", sound="glass",
       unbreakable=True, light=9, emissive=True, collides=False)
pane("cons_barrier", "cons_barrier", display="Edge Protection", sound="wood")
cube("cons_blocks_pallet", "cons_blocks_pallet", display="Pallet of Blocks")
cube("cons_rubble", "cons_rubble", display="Rubble", sound="gravel")
shaped("cons_sign", [(1, 2, 0, 15, 14, 1, "s")], {"s": "cons_sign"}, display="Site Sign", sound="metal",
       unbreakable=True, collides=False)
exit_block("cons_hoist", "cons_hoist", "ASCEND", trigger="USE", solid=True, display="Hoist Controls", sound="metal",
           light=5, emissive=True)
exit_block("cons_door", "cons_door", "DOOR", trigger="USE", solid=True, display="Site Office Door", sound="metal")


# ------------------------------------------------------------------ sound

def wind_loop():
    d = 26.0
    t = Y.t_axis(d)
    wind = Y.bp(Y.pink(d, "conswind"), 100, 1400) * (0.7 + 0.3 * np.sin(2 * np.pi * t / 6.5))
    city = Y.lp(Y.brown(d, "conscity"), 120) * 0.5
    flap = np.zeros(len(t))
    r = Y.rng("consflap")
    for k in range(14):
        n = int(0.06 * Y.SR)
        Y.place(flap, Y.bp(Y.white(0.06, f"consfl{k}"), 300, 3000)[:n] * Y.decay(n, 0.02), r.random() * (d - 1), 0.4)
    creak = np.sin(2 * np.pi * (round(97 * d) / d) * t) * 0.03 * (np.sin(2 * np.pi * t / 13.0) > 0.7)
    x = wind + city + flap + creak
    return Y.stereoize(Y.loopable(Y.norm(x, 0.58), 1.5), 0.005), "stereo"


def clank():
    return Y.norm(Y.reverb(Y.impact("conscl", 0.4, ring=0.8), 2.5, 0.5, "consclr"), 0.5)


def hammering():
    out = np.zeros(int(3.0 * Y.SR))
    for k in range(3):
        Y.place(out, Y.impact(f"conshm{k}", 0.25, ring=0.3), 0.2 + k * 0.6, 1.0)
    return Y.norm(Y.reverb(Y.lp(out, 2500), 4.0, 0.7, "conshmr"), 0.4)


def slew():
    d = 4.0
    t = Y.t_axis(d)
    x = np.sin(2 * np.pi * np.cumsum(220 + 30 * np.sin(2 * np.pi * 0.25 * t)) / Y.SR) * 0.3 * Y.env(len(t), 0.8, 1.2)
    x += Y.bp(Y.pink(d, "consslew"), 200, 1200) * 0.3 * Y.env(len(t), 0.8, 1.2)
    return Y.norm(Y.reverb(x, 3.0, 0.5, "consslr"), 0.4)


def whistle():
    d = 2.2
    t = Y.t_axis(d)
    trill = 1 + 0.03 * np.sign(np.sin(2 * np.pi * 28 * t))
    x = np.sin(2 * np.pi * np.cumsum(2900 * trill) / Y.SR) * ((t < 0.5) | ((t > 0.8) & (t < 1.6)))
    x = x * 0.5 + Y.bp(Y.white(d, "conswh"), 2500, 4000) * 0.1 * ((t < 0.5) | ((t > 0.8) & (t < 1.6)))
    return Y.norm(Y.reverb(x, 3.0, 0.5, "conswhr"), 0.8)


def crash():
    out = np.zeros(int(3.0 * Y.SR))
    Y.place(out, Y.impact("conscr1", 1.0), 0.0, 1.0)
    r = Y.rng("conscrd")
    for k in range(10):
        Y.place(out, Y.impact(f"conscrb{k}", 0.15, ring=0.2), 0.1 + r.random() * 0.8, 0.3)
    return Y.norm(Y.reverb(out, 2.5, 0.4, "conscrr"), 0.95)


sound("ambient.construction_site", [("ambient/construction_site", wind_loop)], stream=True)
sound("ambient.construction_site.additions",
      [("ambient/construction_site_clank", clank), ("ambient/construction_site_hammer", hammering),
       ("ambient/construction_site_slew", slew)], subtitle="Someone is still working")
sound("event.construction_site_whistle", [("event/construction_site_whistle", whistle)], subtitle="A whistle, from above")
sound("event.construction_site_crash", [("event/construction_site_crash", crash)], subtitle="Something falls")

level("construction_site", fog="#121620", sky="#0a0d16", ambient="ambient.construction_site",
      additions="ambient.construction_site.additions", additions_chance=0.007, fixed_time=18000, skylight=True,
      ceiling=False, height=80)
