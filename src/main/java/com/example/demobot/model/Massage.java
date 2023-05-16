package com.example.demobot.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Getter
@Setter
@Entity
@NoArgsConstructor

public class Massage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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

}
