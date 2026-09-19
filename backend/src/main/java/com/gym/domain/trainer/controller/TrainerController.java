package com.gym.domain.trainer.controller;

import com.gym.common.response.ApiResponse;
import com.gym.domain.trainer.dto.TrainerResponse;
import com.gym.domain.trainer.dto.TrainerSaveRequest;
import com.gym.domain.trainer.service.TrainerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {

    private final TrainerService trainerService;

    public TrainerController(TrainerService trainerService) {
        this.trainerService = trainerService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrainerResponse>>> getTrainers(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        return ResponseEntity.ok(ApiResponse.success(trainerService.getTrainers(includeInactive)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TrainerResponse>> createTrainer(
            @Valid @RequestBody TrainerSaveRequest request) {
        TrainerResponse response = trainerService.createTrainer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<TrainerResponse>> updateTrainer(
            @PathVariable Long id, @Valid @RequestBody TrainerSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(trainerService.updateTrainer(id, request)));
    }
}
