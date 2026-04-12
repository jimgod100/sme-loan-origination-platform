package com.smeloan.platform.disbursement.repository;

import com.smeloan.platform.disbursement.entity.LoanOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoanOfferRepository extends JpaRepository<LoanOffer, Long> {

    Optional<LoanOffer> findByLoanApplicationId(Long applicationId);
}
