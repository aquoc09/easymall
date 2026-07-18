package com.quocnva.easymall.service.risk;

import com.quocnva.easymall.dtos.request.risk.RiskAlertResolveRequest;
import com.quocnva.easymall.dtos.request.risk.RiskRuleUpdateRequest;
import com.quocnva.easymall.dtos.response.risk.RiskAlertResponse;
import com.quocnva.easymall.dtos.response.risk.RiskRuleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RiskService {
    List<RiskRuleResponse> getAllRules();
    RiskRuleResponse updateRule(String ruleCode, RiskRuleUpdateRequest request);

    Page<RiskAlertResponse> getAlerts(String status, Pageable pageable);
    void resolveAlert(Long alertId, RiskAlertResolveRequest request);
}
