package com.quocnva.easymall.controller;

import com.quocnva.easymall.util.Translator;

import com.quocnva.easymall.dtos.request.risk.RiskAlertResolveRequest;
import com.quocnva.easymall.dtos.request.risk.RiskRuleUpdateRequest;
import com.quocnva.easymall.dtos.response.ApiResponse;
import com.quocnva.easymall.dtos.response.risk.RiskAlertResponse;
import com.quocnva.easymall.dtos.response.risk.RiskRuleResponse;
import com.quocnva.easymall.service.risk.RiskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/risk")
@RequiredArgsConstructor
public class RiskAdminController {

    private final RiskService riskService;

    @GetMapping("/rules")
    @PreAuthorize("@permissionChecker.has('risk_rule:manage')")
    public ApiResponse<List<RiskRuleResponse>> getAllRules() {
        return ApiResponse.<List<RiskRuleResponse>>builder()
                .result(riskService.getAllRules())
                .build();
    }

    @PutMapping("/rules/{ruleCode}")
    @PreAuthorize("@permissionChecker.has('risk_rule:manage')")
    public ApiResponse<RiskRuleResponse> updateRule(
            @PathVariable String ruleCode,
            @Valid @RequestBody RiskRuleUpdateRequest request) {
        return ApiResponse.<RiskRuleResponse>builder()
                .result(riskService.updateRule(ruleCode, request))
                .build();
    }

    @GetMapping("/alerts")
    @PreAuthorize("@permissionChecker.has('risk_alert:view')")
    public ApiResponse<Page<RiskAlertResponse>> getAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.<Page<RiskAlertResponse>>builder()
                .result(riskService.getAlerts(status, pageable))
                .build();
    }

    @PostMapping("/alerts/{alertId}/resolve")
    @PreAuthorize("@permissionChecker.has('risk_alert:resolve')")
    public ApiResponse<Void> resolveAlert(
            @PathVariable Long alertId,
            @Valid @RequestBody RiskAlertResolveRequest request) {
        riskService.resolveAlert(alertId, request);
        return ApiResponse.<Void>builder()
                .message(Translator.toLocale("success.risk.alert-resolved"))
                .build();
    }
}
