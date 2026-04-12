package com.smeloan.platform.scoring.repository;

import com.smeloan.platform.scoring.entity.ScoreResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoreResultRepository extends JpaRepository<ScoreResult, Long> {

    Optional<ScoreResult> findByLoanApplicationId(Long applicationId);
}
