package com.smeloan.platform.application.repository;

import com.smeloan.platform.application.entity.ApplicationParty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationPartyRepository extends JpaRepository<ApplicationParty, Long> {

    List<ApplicationParty> findByLoanApplicationId(Long applicationId);

    void deleteByLoanApplicationId(Long applicationId);
}
