package com.chat.entity.dto;

import lombok.*;

@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class WalletAddress {
   private String address;
   private String chain ;
   private Boolean addressDefault ;
}
