package com.chat.utils.aptos.request.v1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author liqiang
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Resource extends AbstractModule {

    String resourceName;

    List<Resource> resources;

    /**
     * resourceTag
     *
     * @return
     */
    public String resourceTag() {
        if (Objects.isNull(this.resources) || this.resources.isEmpty()) {
            return this.moduleAddress + "::" + this.moduleName + "::" + this.resourceName;
        } else {
            List<String> resourceTags = new ArrayList<>(this.resources.size());
            this.resources.forEach(resource -> resourceTags.add(resource.resourceTag()));
            return this.moduleAddress + "::" + this.moduleName + "::" + this.resourceName + "<" + resourceTags.stream().collect(Collectors.joining(",")) + ">";
        }
    }

    public static Resource struct(
            String moduleAddress,
            String moduleName,
            String resourceName
    ) {
        return Resource.builder()
                .moduleAddress(moduleAddress)
                .moduleName(moduleName)
                .resourceName(resourceName)
                .build();
    }

    public static Resource ofStruct(String resource) {
        String[] data = resource.split("::");
        if (3 != data.length) {
            return null;
        }

        return Resource.builder()
                .moduleAddress(data[0])
                .moduleName(data[1])
                .resourceName(data[2])
                .build();
    }

    public static Resource ofCoinInfo(Resource resource) {
        return Resource.builder()
                .moduleAddress("0x1")
                .moduleName("coin")
                .resourceName("CoinInfo")
                .resources(Collections.singletonList(resource))
                .build();
    }

    public static Resource ofCoinStore(Resource resource) {
        return Resource.builder()
                .moduleAddress("0x1")
                .moduleName("coin")
                .resourceName("CoinStore")
                .resources(Collections.singletonList(resource))
                .build();
    }

    public static Resource account() {
        return Resource.builder()
                .moduleAddress("0x1")
                .moduleName("account")
                .resourceName("Account")
                .build();
    }

    public static Resource apt() {
        return Resource.builder()
                .moduleAddress("0x1")
                .moduleName("aptos_coin")
                .resourceName("AptosCoin")
                .build();
    }

    public static Resource collections() {
        return Resource.builder()
                .moduleAddress("0x3")
                .moduleName("token")
                .resourceName("Collections")
                .build();
    }

    public static Resource tokenStore() {
        return Resource.builder()
                .moduleAddress("0x3")
                .moduleName("token")
                .resourceName("TokenStore")
                .build();
    }

    public static String get2LevelSplit0Struct(String type) {
        return type.split("<")[1].split(">")[0].split(",")[0].trim();
    }

    public static String get2LevelSplit1Struct(String type) {
        return type.split("<")[1].split(">")[0].split(",")[1].trim();
    }


}