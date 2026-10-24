package com.noclip.client.render.entities;

import com.noclip.client.model.GenericModel;
import com.noclip.client.render.EntityRenderers;
import com.noclip.client.render.GenericRenderer;
import com.noclip.entity.ElevatorCar;

public final class ElevatorCarRenderers {
    private ElevatorCarRenderers() {}

    public static void register() {
        EntityRenderers.generic(ElevatorCar.TYPE, "elevator_car", GenericModel.Style.STIFF, 0f, 1f,
                GenericRenderer.tex("elevator_car_glow"));
    }
}
