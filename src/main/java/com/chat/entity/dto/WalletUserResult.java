package com.chat.entity.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletUserResult {
    private int code;
    private List<WalletAddress> address;
    private String message;
    private String transactionHash;
}
