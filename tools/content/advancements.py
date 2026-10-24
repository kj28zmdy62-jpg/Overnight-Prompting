"""Advancements for reaching the famous places. Uses vanilla changed_dimension triggers."""
import assets as A

ADV = {}


def adv(name, title, desc, icon, parent, to=None, frame="task", hidden=False):
    d = {
        "display": {"icon": {"id": icon}, "title": title, "description": desc, "frame": frame,
                    "show_toast": True, "announce_to_chat": True, "hidden": hidden},
        "criteria": {"arrive": {"trigger": "minecraft:changed_dimension",
                                "conditions": {"to": f"noclip:{to}"}} if to else {"trigger": "minecraft:tick"}},
    }
    if parent:
        d["parent"] = f"noclip:{parent}"
    else:
        d["display"]["background"] = "noclip:textures/block/l0_wallpaper.png"
    ADV[name] = d


adv("root", "NOCLIP", "Something is wrong with the walls.", "noclip:almond_water", None, "level_0")
ADV["root"]["display"]["show_toast"] = False
ADV["root"]["display"]["announce_to_chat"] = False
adv("noclip", "Noclipped", "Fall out of reality into Level 0.", "noclip:yellow_wallpaper", "root", "level_0")
adv("habitable", "Habitable?", "Reach Level 1.", "noclip:wooden_crate", "noclip", "level_1")
adv("lights_out", "Lights Out", "Reach Level 6. Hope you brought a flashlight.", "noclip:flashlight", "habitable", "level_6")
adv("poolrooms", "Sublimity", "Find the Poolrooms.", "minecraft:water_bucket", "noclip", "level_37", frame="goal")
adv("fun", "=)", "Get invited to the party.", "minecraft:cake", "noclip", "level_fun", hidden=True)
adv("run", "RUN", "Arrive in Level !", "minecraft:redstone", "noclip", "level_run", hidden=True)
adv("manila", "A Quiet Room", "Find the Manila Room.", "minecraft:paper", "noclip", "manila_room")
adv("hub", "Every Door", "Find the Hub.", "minecraft:oak_door", "noclip", "the_hub", frame="goal")
adv("frontrooms", "Home?", "Reach the Frontrooms.", "minecraft:red_bed", "hub", "frontrooms", frame="goal", hidden=True)
adv("exit", "The Exit", "Find the way out.", "minecraft:ender_eye", "frontrooms", "the_exit", frame="challenge", hidden=True)

for name, d in ADV.items():
    A.ADVANCEMENTS[name] = d
