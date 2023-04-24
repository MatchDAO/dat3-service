package com.chat.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletAssetActivity {
    String transactionHash;
    String activityType;
    String value;
    String fromAddress;
    String toAddress;
    String type;
    String time;
    private Integer sentCount;
}
