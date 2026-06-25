package com.quocnva.easymall.dtos.response.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class FraudResponseDTO {

    @JsonProperty("risk_score")
    private Double riskScore;

    @JsonProperty("top_risk_factors")
    private List<String> topRiskFactors;
}
