package com.example.demobot.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Setter
@Entity
public class Massage {

    @Id
    Long id;
    private String name;
    private int price;
    private String duration;
    private String description;

    @Override
    public String toString() {
        return "Массаж " + name + '\'' +
                ", Стоимость услуги = " + price +
                ", Продолжительность = " + duration + '\'' +
                ", Что входит в массаж = " + description + '\'';
    }

    public Massage() {
    }

    public Massage(String name, int price, String duration, String description) {
        this.name = name;
        this.price = price;
        this.duration = duration;
        this.description = description;
    }


}
