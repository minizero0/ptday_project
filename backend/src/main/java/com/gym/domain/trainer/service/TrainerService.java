package com.gym.domain.trainer.service;

import com.gym.common.exception.BusinessException;
import com.gym.common.exception.ErrorCode;
import com.gym.common.util.PhoneNumber;
import com.gym.domain.trainer.dto.TrainerResponse;
import com.gym.domain.trainer.dto.TrainerSaveRequest;
import com.gym.domain.trainer.entity.Trainer;
import com.gym.domain.trainer.repository.TrainerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainerService {

    private final TrainerRepository trainerRepository;

    public TrainerService(TrainerRepository trainerRepository) {
        this.trainerRepository = trainerRepository;
    }

    @Transactional
    public TrainerResponse createTrainer(TrainerSaveRequest request) {
        Trainer saved = trainerRepository.save(
                new Trainer(request.name().trim(), PhoneNumber.normalize(request.phone())));
        return TrainerResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TrainerResponse> getTrainers(boolean includeInactive) {
        List<Trainer> trainers = includeInactive
                ? trainerRepository.findAllByOrderByActiveDescNameAscIdAsc()
                : trainerRepository.findByActiveTrueOrderByNameAscIdAsc();
        return trainers.stream().map(TrainerResponse::from).toList();
    }

    @Transactional
    public TrainerResponse updateTrainer(Long id, TrainerSaveRequest request) {
        Trainer trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRAINER_NOT_FOUND));

        trainer.updateInfo(request.name().trim(), PhoneNumber.normalize(request.phone()));
        if (request.active() != null) {
            trainer.changeActive(request.active());
        }
        return TrainerResponse.from(trainer);
    }
}
