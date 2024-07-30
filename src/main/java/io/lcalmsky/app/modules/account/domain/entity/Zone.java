package io.lcalmsky.app.modules.account.domain.entity;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static lombok.AccessLevel.PROTECTED;

@Entity
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
@Getter
@Builder
public class Zone {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String localNameOfCity;

    private String province;

    public static Zone map(String line) {
        String[] split = line.split(",");
        Zone zone = new Zone();
        zone.city = split[0];
        zone.localNameOfCity = split[1];
        zone.province = split[2];
        return zone;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)/%s", city, localNameOfCity, province);
    }
}
