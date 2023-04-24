package com.chat.entity.dto;


public class PriceRangeDto {
    private Integer id;
    private String price;
    private String ePrice;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getePrice() {
        return ePrice;
    }

    public void setePrice(String ePrice) {
        this.ePrice = ePrice;
    }

    @Override
    public String toString() {
        return "PriceRangeDto{" +
                "id=" + id +
                ", price='" + price + '\'' +
                ", ePrice='" + ePrice + '\'' +
                '}';
    }
}
