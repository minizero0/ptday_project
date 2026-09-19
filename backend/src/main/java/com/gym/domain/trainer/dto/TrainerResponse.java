package com.gym.domain.trainer.dto;

import com.gym.domain.trainer.entity.Trainer;

public record TrainerResponse(Long id, String name, String phone, boolean active) {

    public static TrainerResponse from(Trainer trainer) {
        return new TrainerResponse(trainer.getId(), trainer.getName(), trainer.getPhone(), trainer.isActive());
    }
}
