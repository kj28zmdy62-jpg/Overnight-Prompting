package com.noclip.entity;

import com.noclip.level.BackroomsLevel;
import com.noclip.level.Levels;
import com.noclip.levels.EmptyBuildingKit;
import com.noclip.levels.LiftLobby;
import com.noclip.registry.ModEntities;
import com.noclip.registry.ModSounds;
import com.noclip.sanity.PlayerData;
import com.noclip.sanity.Sanity;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The Lift Attendant. Burgundy jacket, white gloves, eyes politely closed. He asks
 * which floor you want and takes the answer you type in chat: a level number, a
 * level's name, "up" or "down". He has never been wrong about a floor. He does not
 * like to be touched.
 */
public class LiftAttendant extends PathfinderMob implements Sanity.Frightening {
    public static EntityType<LiftAttendant> TYPE;
    private static final String NAME = "Lift Attendant";

    enum State { IDLE, APPROACH, WAITING, ESCORT, LEAVING }

    private State state = State.IDLE;
    private UUID target;
    private int stateTicks, asks, fails, hits;

    public LiftAttendant(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    public static void register() {
        TYPE = ModEntities.register("lift_attendant", LiftAttendant::new, MobCategory.MONSTER, 0.6f, 2.1f,
                createAttributes(), 0x6E1A22, 0xD4AC48);
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String text = message.signedContent();
            sender.server.execute(() -> onChat(sender, text));
        });
    }

    private static void onChat(ServerPlayer p, String text) {
        for (LiftAttendant a : p.level().getEntitiesOfClass(LiftAttendant.class, p.getBoundingBox().inflate(14))) {
            if (a.state == State.WAITING && p.getUUID().equals(a.target)) {
                a.answer(p, text);
                return;
            }
        }
    }

    @Override
    protected void registerGoals() {
    }

    /** Someone rang. Go to them. */
    public void summon(Player p) {
        target = p.getUUID();
        state = State.APPROACH;
        stateTicks = 0;
    }

    private ServerPlayer targetPlayer() {
        if (target == null || !(level().getPlayerByUUID(target) instanceof ServerPlayer sp)) return null;
        return sp;
    }

    private void speak(ServerPlayer p, String text) {
        EmptyBuildingKit.say(p, NAME, ChatFormatting.GOLD, text);
        level().playSound(null, blockPosition(), ModSounds.get("entity.lift_attendant.speak"), SoundSource.NEUTRAL, 0.7f,
                0.95f + random.nextFloat() * 0.1f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        stateTicks++;
        ServerPlayer p = targetPlayer();
        if (state == State.IDLE) {
            Player near = level().getNearestPlayer(this, 9);
            if (near instanceof ServerPlayer sp && !sp.isSpectator() && hasLineOfSight(sp)) summon(sp);
            if (tickCount > 20 * 240 && !anyoneSees()) discard();
            return;
        }
        if (p == null || p.level() != level() || p.distanceToSqr(this) > 56 * 56) {
            state = State.LEAVING;
        }
        if (p != null) getLookControl().setLookAt(p, 30f, 30f);
        switch (state) {
            case APPROACH -> {
                if (p == null) return;
                if (distanceToSqr(p) > 3.2 * 3.2) {
                    if (stateTicks % 10 == 0) getNavigation().moveTo(p, 0.9);
                } else {
                    getNavigation().stop();
                    greet(p);
                }
                if (stateTicks > 20 * 40) state = State.LEAVING;
            }
            case WAITING -> {
                if (p == null) return;
                if (distanceToSqr(p) > 6 * 6) {
                    if (stateTicks % 10 == 0) getNavigation().moveTo(p, 0.7);
                } else getNavigation().stop();
                if (stateTicks % 400 == 0) {
                    asks++;
                    if (asks == 1) speak(p, "Which floor, please?");
                    else if (asks == 2) speak(p, "I can wait. The lift can wait. Which floor?");
                    else {
                        speak(p, "Then I'll choose for you.");
                        resolve(p, pickFor(p), "Very good.");
                    }
                }
            }
            case ESCORT -> {
                if (stateTicks > 20 * 30) state = State.LEAVING;
            }
            case LEAVING -> {
                if (stateTicks % 20 == 0) {
                    getNavigation().moveTo(getX() + random.nextGaussian() * 10, getY(), getZ() + random.nextGaussian() * 10, 0.6);
                }
                if (!anyoneSees() || stateTicks > 20 * 60) discard();
            }
            default -> {
            }
        }
    }

    private boolean anyoneSees() {
        return Mannequin.seenByAnyone(level(), this, 0.3);
    }

    private void greet(ServerPlayer p) {
        state = State.WAITING;
        stateTicks = 0;
        String[] hello = {"Good evening. Going up?", "Good evening. Which floor, please?", "Ah. There you are. Which floor?",
                "Mind the doors. Which floor would you like?"};
        speak(p, hello[random.nextInt(hello.length)]);
        List<String> numbered = new ArrayList<>();
        for (BackroomsLevel l : Levels.all()) if (!l.number.isEmpty() && numbered.size() < 8) numbered.add(l.number);
        EmptyBuildingKit.narrate(p, "(Answer in chat: a floor number such as " + String.join(", ", numbered)
                + ", a place by name, or simply up or down.)");
    }

    private void answer(ServerPlayer p, String raw) {
        String text = raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 =!?_-]", " ")
                .replaceAll("\\b(floor|level|the|please|to|go|take me|i want|number|uh|um)\\b", " ").trim().replaceAll("\\s+", " ");
        if (text.isEmpty()) {
            speak(p, "I'm sorry?");
            return;
        }
        String here = LiftLobby.ID;
        if (text.matches("(up|upward|upwards|higher|top|roof)")) {
            resolve(p, "#ASCEND", "Going up.");
            return;
        }
        if (text.matches("(down|downward|downwards|lower|basement|bottom|b)")) {
            resolve(p, "#DESCEND", "Going down.");
            return;
        }
        if (text.matches("(no|nothing|none|nowhere|stairs|walk|i'll walk|neither)")) {
            speak(p, "Then you'll take the stairs.");
            if (Levels.get("stairwell") != null) {
                state = State.ESCORT;
                stateTicks = 0;
                LiftLobby.rideNow(p, "stairwell");
            }
            return;
        }
        if (text.matches("(home|exit|out|leave|outside|surface|overworld|real world|reality|back|the exit|way out)")) {
            fails++;
            String[] r = {"Everyone asks for that one. There isn't a button for it.", "That floor was removed some time ago.",
                    "I'm afraid this lift doesn't stop there. Nothing does."};
            speak(p, r[Math.min(fails - 1, r.length - 1)]);
            return;
        }
        if (text.matches("(lobby|ground|ground floor|g|0|zero)")) {
            if (Levels.get("level_0") != null) resolve(p, "level_0", "The Lobby. Mind the carpet, it's damp.");
            return;
        }
        if (text.matches("-?\\d+")) {
            String n = text;
            if (n.equals("13")) {
                if (Levels.get("level_13") != null) resolve(p, "level_13", "Thirteen. Of course.");
                else speak(p, "We don't have a thirteenth floor. We never did. Another?");
                return;
            }
            if (n.startsWith("-")) {
                speak(p, "Below the basement? ...Very well.");
                resolve(p, "#DESCEND", "Going down. A long way.");
                return;
            }
            String id = "level_" + Integer.parseInt(n);
            if (Levels.get(id) != null) {
                resolve(p, id, "Floor " + n + ". Very good.");
            } else {
                fails++;
                String[] r = {"Floor " + n + " is closed for cleaning.", "There's no floor " + n + ". There has never been a floor " + n + ".",
                        "Floor " + n + "? No. Choose another."};
                speak(p, r[random.nextInt(r.length)]);
                if (fails >= 3) {
                    speak(p, "Allow me.");
                    resolve(p, pickFor(p), "Very good.");
                }
            }
            return;
        }
        if (text.equals("fun") || text.contains("party") || text.contains("fun =")) {
            if (Levels.get("level_fun") != null) {
                resolve(p, "level_fun", "...If you insist. Do try to smile.");
                return;
            }
        }
        // a place by name
        BackroomsLevel match = null;
        for (BackroomsLevel l : Levels.all()) {
            if (l.id.equals(here)) continue;
            String id = l.id.replace('_', ' ');
            String title = l.title.toLowerCase(Locale.ROOT).replaceAll("^the ", "");
            if (text.equals(id) || text.equals(title) || text.equals(l.id)
                    || (text.length() >= 4 && (title.contains(text) || id.contains(text)))
                    || (title.length() >= 4 && text.contains(title))) {
                match = l;
                break;
            }
        }
        if (match != null) {
            resolve(p, match.id, (match.number.isEmpty() ? match.title : "Floor " + match.number) + ". Very good.");
            return;
        }
        if (text.contains("lift lobby") || text.equals("here") || text.equals("this one")) {
            speak(p, "You're on it. You have always been on it.");
            return;
        }
        fails++;
        String[] r = {"I don't know that floor.", "There's no such floor. Another?", "That isn't on the panel."};
        speak(p, r[Math.min(fails - 1, r.length - 1)]);
        if (fails >= 3) {
            speak(p, "Allow me.");
            resolve(p, pickFor(p), "Very good.");
        }
    }

    /** Somewhere the player has been, but not here; or up. */
    private String pickFor(ServerPlayer p) {
        List<String> been = new ArrayList<>(PlayerData.visited(p));
        been.remove(LiftLobby.ID);
        been.removeIf(id -> Levels.get(id) == null);
        if (been.isEmpty() || random.nextFloat() < 0.3f) return "#ASCEND";
        return been.get(random.nextInt(been.size()));
    }

    private void resolve(ServerPlayer p, String dest, String line) {
        speak(p, line);
        state = State.ESCORT;
        stateTicks = 0;
        if (LiftLobby.prepareRide(p, dest, position())) {
            EmptyBuildingKit.narrate(p, "(A lift has arrived. Step inside.)");
        } else {
            speak(p, "The lift is busy. Close your eyes.");
            LiftLobby.rideNow(p, dest);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (source.getEntity() instanceof ServerPlayer p && !p.isCreative()) {
            hits++;
            if (hits == 1) speak(p, "Please don't touch the uniform.");
            else {
                speak(p, "Mind the gap.");
                LiftLobby.dropThrough(p);
            }
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSq) {
        return distanceSq > 80 * 80;
    }

    @Override
    public float fear() {
        return state == State.WAITING ? 0.03f : 0.01f;
    }
}
