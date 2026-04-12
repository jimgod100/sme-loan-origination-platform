package com.smeloan.platform.externalcheck.repository;

import com.smeloan.platform.externalcheck.entity.CheckStatus;
import com.smeloan.platform.externalcheck.entity.CheckType;
import com.smeloan.platform.externalcheck.entity.ExternalCheckResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalCheckResultRepository extends JpaRepository<ExternalCheckResult, Long> {

    List<ExternalCheckResult> findByLoanApplicationId(Long applicationId);

    Optional<ExternalCheckResult> findByLoanApplicationIdAndCheckType(Long applicationId, CheckType checkType);

    long countByLoanApplicationIdAndStatus(Long applicationId, CheckStatus status);

    long countByLoanApplicationId(Long applicationId);
}
