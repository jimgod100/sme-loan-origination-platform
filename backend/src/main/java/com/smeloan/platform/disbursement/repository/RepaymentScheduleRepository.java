package com.smeloan.platform.disbursement.repository;

import com.smeloan.platform.disbursement.entity.RepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, Long> {

    List<RepaymentSchedule> findByLoanOfferIdOrderByInstallmentNumber(Long loanOfferId);

    void deleteByLoanOfferId(Long loanOfferId);
}
