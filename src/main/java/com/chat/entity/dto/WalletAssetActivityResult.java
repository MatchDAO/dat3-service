package com.chat.entity.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletAssetActivityResult {
    private int code;
    private List<WalletAssetActivity> data;
    private String message;
}
