"""Server Farm - cold aisles of racks under hard white light, LEDs blinking in patterns
nobody reads, fans so loud you cannot think. Sometimes the fire suppression arms itself."""
import numpy as np
from assets import cube, cube_faces, tex, shaped, pane, exit_block, level, sound, custom, BLOCK_MODELS
from content.ind_kit import anim_tex, text, rivets, steel, box_model, facing_states
import tex as T
import synth as Y

S = 32


def rack_front(seed, frame, n, failing=False):
    """Black mesh door; behind it, unit after unit of blinking LEDs."""
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise(seed + "b", sigma=1), "#0c0d10", "#16181c")
    mesh = ((xx + yy) % 3 == 0) | ((xx - yy) % 3 == 0)
    img = T.shade(img, 1 - mesh * 0.25)
    r = T.rng(seed)
    for unit in range(8):
        y = 2 + unit * 4
        img[y - 1, 3:29] = T.hexc("#24272c")
        kinds = r.random()
        for led in range(5):
            x = 4 + led * 3 if kinds < 0.6 else 18 + led * 2
            period = 1 + int(r.random() * n)
            phase = int(r.random() * n)
            on = ((frame + phase) // max(1, period // 2 + 1)) % 2 == 0 or r.random() < 0.3
            if failing:
                col = "#ff3020" if led % 2 == 0 else "#ffa020"
            else:
                col = ["#40ff70", "#40ff70", "#40ff70", "#ffb030", "#40a0ff"][int(r.random() * 5)]
            if on:
                img[y, x] = T.hexc(col)
                img[y, x + 1] = T.hexc(col) * 0.6
        # drive bays
        if kinds > 0.6:
            for b in range(3):
                img[y:y + 2, 4 + b * 4:7 + b * 4] = T.hexc("#2a2d33")
    img = T.border(img, "#2c2f35", 2, "ew")
    img[:, 15:17] = img[:, 15:17] * 0.7
    return img


def rack_back():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("srvbk", sigma=1), "#121418", "#1c1f24")
    r = T.rng("srvcab")
    cols = ["#1a3a80", "#8a7418", "#7a1818", "#186a2a", "#6a6a6a", "#101010", "#101010", "#101010"]
    for k in range(16):
        x = int(r.random() * 28) + 2
        col = T.hexc(cols[int(r.random() * len(cols))])
        sag = r.random() * 3
        for y in range(32):
            xx2 = int(x + np.sin(y / 5.0 + k) * sag) % S
            img[y, xx2] = col * (0.45 + 0.3 * r.random())
    d = np.sqrt((xx - 16) ** 2 + (yy - 24) ** 2)
    fan = (d < 5) & (((np.degrees(np.arctan2(yy - 24, xx - 16)) + 360) % 60) < 25)
    img = np.where(fan[..., None], T.solid("#08090a"), img)
    img = T.border(img, "#2c2f35", 2, "ew")
    return img


def rack_side():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("srvsd", sigma=2), "#16181c", "#202328")
    vents = ((yy % 6) < 2) & (xx > 6) & (xx < 26) & ((yy // 6) % 2 == 0)
    img = T.shade(img, 1 - vents * 0.5)
    return T.grain(img, "srvsd", 0.02)


def rack_top():
    img = rack_side()
    img = T.rect(img, 6, 6, 26, 12, "#08090a")
    return img


def tile(seed="srvt", scuff=False):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise(seed, sigma=2), "#b8bcc0", "#ccd0d4")
    holes = ((xx % 4) == 2) & ((yy % 4) == 2) & (xx > 3) & (xx < 28) & (yy > 3) & (yy < 28)
    img = np.where(holes[..., None], T.solid("#3a3e44"), img)
    img = T.border(img, "#8a8e94", 1)
    img = T.border(img, "#5a5e64", 1, "se")
    if scuff:
        img = T.stains(img, seed + "s", "#7a7e84", 2, 0.35, 6)
    return T.grain(img, seed, 0.02)


def tile_solid():
    img = T.ramp(T.noise("srvts", sigma=2), "#a8acb0", "#bcc0c4")
    img = T.border(img, "#7a7e84", 1)
    return T.grain(img, "srvts", 0.02)


def underfloor():
    img = T.ramp(T.fbm("srvuf", octaves=3, base=4), "#2a2c2e", "#363a3c")
    r = T.rng("srvufc")
    for k in range(6):
        y = int(r.random() * 32)
        col = T.hexc(["#2050c0", "#e0c020", "#606060", "#c02020"][k % 4])
        for x in range(32):
            img[(y + int(np.sin(x / 6.0 + k) * 2)) % S, x] = col * 0.7
    return T.grain(img, "srvuf", 0.04)


def wall(seed="srvw"):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise(seed, sigma=3), "#c8ccd0", "#d8dce0")
    mortar = ((yy % 16) == 0) | (((xx + (yy // 16) * 16) % 32) == 0)
    img = T.shade(img, 1 - mortar * 0.07)
    return T.grain(img, seed, 0.02)


def wall_sign(label):
    def f():
        img = wall("srvws" + label)
        img = T.rect(img, 3, 9, 29, 23, "#1a3a6a")
        w = len(label) * 4 - 1
        img = text(img, label, 16 - w // 2, 14, "#ffffff")
        return img
    return f


def halon_sign():
    img = wall("srvhs")
    img = T.rect(img, 2, 4, 30, 28, "#c01818")
    img = text(img, "HALON", 6, 7, "#ffffff")
    img = text(img, "LEAVE", 6, 14, "#ffffff")
    img = text(img, "ON", 6, 21, "#ffffff")
    img = text(img, "ALARM", 13, 21, "#ffffff")
    return img


def glass():
    rgb = T.ramp(T.noise("srvgl", sigma=3), "#a8c4d8", "#d0e4f0")
    yy, xx = np.mgrid[0:S, 0:S]
    a = np.full((S, S), 0.22)
    streak = np.abs((xx + yy) % 24 - 6) < 1.5
    a[streak] = 0.36
    edge = (xx < 1) | (xx > 30)
    a[edge] = 0.9
    rgb[edge] = T.hexc("#6a7076")
    return rgb, a


def ceiling():
    img = T.ramp(T.noise("srvc", sigma=2), "#9a9ea2", "#aaaeb2")
    img = T.border(img, "#6a6e72", 1)
    pits = (T.rng("srvcp").random((S, S)) > 0.9).astype(float)
    return T.shade(img, 1 - pits * 0.08)


def led_panel():
    img = T.ramp(T.noise("srvled", sigma=3), "#eef6ff", "#ffffff")
    return T.border(T.border(img, "#c0c8d0", 2), "#8a9098", 1)


def led_panel_off():
    img = T.ramp(T.noise("srvledo", sigma=3), "#5a6068", "#6a7078")
    return T.border(T.border(img, "#7a8088", 2), "#5a6068", 1)


def tray():
    yy, xx = np.mgrid[0:S, 0:S]
    rgb = T.ramp(T.noise("srvtr", sigma=1), "#7a7e82", "#9a9ea2")
    a = (((xx % 32) < 2) | ((xx % 32) > 29) | ((yy % 8) < 2)).astype(float)
    r = T.rng("srvtrc")
    for k in range(5):
        x = 4 + k * 5
        rgb[:, x:x + 2] = T.hexc(["#e0c020", "#2050c0", "#c02020", "#d0d0d0", "#20a040"][k])
        a[:, x:x + 2] = 1
    return rgb, a


def screen(i):
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("srvscr", sigma=2), "#04080c", "#081018")
    img = T.shade(img, 1 - ((yy % 2) == 0) * 0.15)
    for k in range(5):
        ln = 6 + int(T.rng(f"srvl{k}").random() * 18)
        img[5 + k * 4, 3:3 + ln] = T.hexc("#30c060") * (0.5 if k < 4 else 1)
    if i % 2 == 0:
        img[21, 3 + 6:5 + 6] = T.hexc("#80ff9a")
    return T.border(img, "#1a1c20", 2)


def keyboard():
    yy, xx = np.mgrid[0:S, 0:S]
    img = T.ramp(T.noise("srvkb", sigma=1), "#1a1c20", "#24272c")
    keys = ((xx % 3) != 0) & ((yy % 3) != 0)
    return T.blend(img, T.solid("#3a3e44"), keys.astype(float) * 0.8)


def beacon(i, n):
    yy, xx = np.mgrid[0:S, 0:S]
    ang = np.arctan2(yy - 15.5, xx - 15.5)
    sweep = np.cos(ang - 2 * np.pi * i / n) * 0.5 + 0.5
    img = T.ramp(sweep ** 3, "#5a0806", "#ff2a1a", "#fff0e0")
    return T.border(img, "#2a2a2a", 2)


def beacon_off():
    img = T.ramp(T.noise("srvbo", sigma=2), "#3a0a08", "#4a1210")
    return T.border(img, "#2a2a2a", 2)


def exit_door():
    img = steel("srvdoor", "#8a9098", "#a0a6ae", brushed=True)
    img = T.border(img, "#4a5058", 2)
    img = T.rect(img, 23, 14, 28, 18, "#2a2e34")
    img = T.rect(img, 4, 4, 28, 10, "#1a6a3a")
    img = text(img, "EXIT", 9, 5, "#ffffff")
    img = T.rect(img, 6, 22, 14, 28, "#2a2e34")
    img[24:26, 8:12] = T.hexc("#40ff70")
    return img


tex("srv_rack_side", rack_side)
tex("srv_rack_top", rack_top)
tex("srv_rack_back", rack_back)
for v in ("a", "b", "c"):
    anim_tex(f"srv_rack_front_{v}", (lambda vv: lambda i: rack_front("srvrf" + vv, i, 8))(v), 8, frametime=3)
anim_tex("srv_rack_front_red", lambda i: rack_front("srvrfr", i, 8, failing=True), 8, frametime=2)
tex("srv_tile", lambda: tile())
tex("srv_tile_scuff", lambda: tile("srvts2", True))
tex("srv_tile_solid", tile_solid)
tex("srv_underfloor", underfloor)
tex("srv_wall", lambda: wall())
tex("srv_sign_hall", wall_sign("HALL"))
tex("srv_sign_cold", wall_sign("COLD"))
tex("srv_sign_halon", halon_sign)
tex("srv_glass", glass)
tex("srv_ceiling", ceiling)
tex("srv_led", led_panel)
tex("srv_led_off", led_panel_off)
tex("srv_tray", tray)
anim_tex("srv_screen", screen, 2, frametime=10)
tex("srv_keyboard", keyboard)
anim_tex("srv_beacon", lambda i: beacon(i, 6), 6, frametime=1)
tex("srv_beacon_off", beacon_off)
tex("srv_door", exit_door)

RACK_TEX = {"side": "srv_rack_side", "back": "srv_rack_back", "top": "srv_rack_top"}
for v in ("a", "b", "c", "red"):
    t = dict(RACK_TEX, front=f"srv_rack_front_{v}")
    shaped(f"srv_rack_{v}", [(0, 0, 0, 16, 16, 16, {"all": "side", "south": "front", "north": "back", "up": "top"})],
           t, display="Server Rack", sound="metal", unbreakable=True, light=3, emissive=True, render="solid")
shaped("srv_console", [(0, 0, 0, 16, 16, 16, {"all": "side", "south": "front", "north": "back", "up": "top"}),
                       (2, 5, 16, 14, 6, 22, {"all": "side", "up": "kb"})],
       {"side": "srv_rack_side", "back": "srv_rack_back", "top": "srv_rack_top", "front": "srv_screen",
        "kb": "srv_keyboard"}, display="KVM Console", sound="metal", unbreakable=True, light=6, emissive=True,
       render="solid")
cube("srv_floor_tile", "srv_tile", display="Perforated Floor Tile", sound="metal")
cube("srv_floor_scuffed", "srv_tile_scuff", display="Perforated Floor Tile", sound="metal")
cube("srv_floor_solid", "srv_tile_solid", display="Floor Tile", sound="metal")
cube("srv_underfloor", "srv_underfloor", display="Underfloor")
cube("srv_wall", "srv_wall", display="Painted Block Wall")
cube("srv_sign_hall", "srv_sign_hall", display="Hall Sign")
cube("srv_sign_cold", "srv_sign_cold", display="Aisle Sign")
cube("srv_sign_halon", "srv_sign_halon", display="Halon Warning")
pane("srv_glass", "srv_glass", display="Glass Partition", sound="glass", render="translucent")
cube("srv_ceiling", "srv_ceiling", display="Ceiling Grid", flat=True)
cube_faces("srv_led", bottom="srv_led", top="srv_ceiling", side="srv_ceiling", display="LED Panel", light=15,
           emissive=True, flat=True, sound="glass")
cube_faces("srv_led_off", bottom="srv_led_off", top="srv_ceiling", side="srv_ceiling", display="Dead LED Panel",
           flat=True, sound="glass")
shaped("srv_tray", [(0, 12, 0, 16, 13, 16, "t"), (0, 13, 0, 1, 16, 16, "t"), (15, 13, 0, 16, 16, 16, "t")],
       {"t": "srv_tray"}, display="Cable Tray", collides=False, unbreakable=True, sound="metal")
exit_block("srv_exit_door", "srv_door", "DOOR", trigger="USE", solid=True, display="Data Hall Exit", sound="metal")

# alarm beacon: dark until the behaviour lights it for a halon alarm
BLOCK_MODELS["srv_beacon_on"] = box_model([(5, 0, 5, 11, 6, 11, "b"), (4, 0, 4, 12, 1, 12, "f")],
                                          {"b": "srv_beacon", "f": "srv_rack_side"})
BLOCK_MODELS["srv_beacon_off"] = box_model([(5, 0, 5, 11, 6, 11, "b"), (4, 0, 4, 12, 1, 12, "f")],
                                           {"b": "srv_beacon_off", "f": "srv_rack_side"})
custom("srv_beacon", "com.noclip.block.AlarmBeaconBlock", BLOCK_MODELS["srv_beacon_off"],
       blockstate={"variants": {"lit=false": {"model": "noclip:block/srv_beacon_off"},
                                "lit=true": {"model": "noclip:block/srv_beacon_on"}}},
       display="Alarm Beacon", sound="glass", unbreakable=True, render="cutout")

# ------------------------------------------------------------------ sound


def fans():
    d = 24.0
    t = Y.t_axis(d)
    roar = Y.bp(Y.pink(d, "srvroar"), 80, 9000) * 0.8
    whoosh = Y.bp(Y.white(d, "srvwh"), 1500, 6000) * 0.25
    blades = Y.hum(d, 37.5, "srvbl", buzz=0.0, harmonics=(4, 8, 12, 16)) * 0.12
    whine = sum(np.sin(2 * np.pi * (round(f * d) / d) * t) * g for f, g in ((2875, 0.01), (4210, 0.006), (6125, 0.004)))
    beat = 0.92 + 0.08 * np.sin(2 * np.pi * t / 6.0)
    x = (roar + whoosh) * beat + blades + whine
    return Y.stereoize(Y.loopable(Y.norm(x, 0.62), 1.0), 0.0025), "stereo"


def seek(i):
    def f():
        out = np.zeros(int(1.5 * Y.SR))
        r = Y.rng(f"srvsk{i}")
        tt = 0.05
        for k in range(8 + i * 4):
            n = int(0.012 * Y.SR)
            c = Y.bp(Y.white(0.012, f"srvsk{i}{k}"), 1800, 9000)[:n] * np.exp(-np.arange(n) / (0.002 * Y.SR))
            Y.place(out, c, tt, 0.4 + r.random() * 0.6)
            tt += 0.02 + r.random() * 0.08
        return Y.norm(out, 0.5)
    return f


def post_beep():
    d = 0.6
    t = Y.t_axis(d)
    x = np.sign(np.sin(2 * np.pi * 1000 * t)) * (t < 0.18) * 0.5
    return Y.norm(Y.reverb(Y.lp(x, 4000), 1.0, 0.3, "srvbeep"), 0.5)


def relay():
    out = np.zeros(int(1.2 * Y.SR))
    Y.place(out, Y.impact("srvrel", 0.15, ring=0.3), 0.05, 1.0)
    Y.place(out, Y.impact("srvrel2", 0.12, ring=0.2), 0.11, 0.6)
    return Y.norm(Y.reverb(out, 1.4, 0.4, "srvrel"), 0.5)


def spin_up():
    d = 4.0
    t = Y.t_axis(d)
    f0 = 200 + 3800 * np.clip(t / 3.0, 0, 1) ** 0.7
    x = np.sin(2 * np.pi * np.cumsum(f0) / Y.SR) * 0.15 * np.clip(t / 0.3, 0, 1) * np.clip((d - t) / 1.0, 0, 1)
    x += Y.bp(Y.white(d, "srvsu"), 800, 5000) * 0.3 * np.clip(t / 3.0, 0, 1) * np.clip((d - t) / 1.0, 0, 1)
    return Y.norm(Y.reverb(x, 1.5, 0.3, "srvsu"), 0.5)


def alarm():
    d = 2.0
    t = Y.t_axis(d)
    hi = (t % 1.0) < 0.5
    f0 = np.where(hi, 960.0, 770.0)
    x = np.sign(np.sin(2 * np.pi * np.cumsum(f0) / Y.SR)) * 0.4
    x = Y.lp(x, 3500) * (0.8 + 0.2 * np.sin(2 * np.pi * 8 * t))
    return Y.norm(Y.reverb(x, 1.8, 0.4, "srvalarm"), 0.8)


def discharge():
    d = 6.0
    t = Y.t_axis(d)
    env = (1 - np.exp(-t * 12)) * np.clip((d - t) / 3.0, 0, 1)
    x = Y.bp(Y.white(d, "srvdis"), 300, 12000) * env + Y.lp(Y.brown(d, "srvdis2"), 150) * env * 1.4
    thump = np.zeros(len(t))
    Y.place(thump, Y.impact("srvdisk", 1.4), 0.0, 1.0)
    return Y.norm(Y.reverb(x + thump, 2.0, 0.5, "srvdis"), 0.95)


sound("ambient.server_farm", [("ambient/server_farm", fans)], stream=True)
sound("ambient.server_farm.additions",
      [(f"ambient/server_farm_seek{i}", seek(i)) for i in range(3)] +
      [("ambient/server_farm_beep", post_beep), ("ambient/server_farm_relay", relay),
       ("ambient/server_farm_spin", spin_up)],
      subtitle="Drives chatter")
sound("event.server_farm_alarm", [("event/server_farm_alarm", alarm)], subtitle="Fire suppression alarm")
sound("event.server_farm_discharge", [("event/server_farm_discharge", discharge)], subtitle="Halon discharges")

level("server_farm", fog="#a9b6c2", sky="#a9b6c2", ambient="ambient.server_farm",
      additions="ambient.server_farm.additions", additions_chance=0.01, fixed_time=6000, height=48)
