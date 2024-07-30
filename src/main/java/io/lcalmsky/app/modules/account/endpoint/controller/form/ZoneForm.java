package io.lcalmsky.app.modules.account.endpoint.controller.form;

import lombok.*;

import static lombok.AccessLevel.PROTECTED;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneForm {

    private String zoneName;

    public String getCityName() {
        return zoneName.substring(0, zoneName.indexOf("("));
    }

    public String getProvinceName() {
        return zoneName.substring(zoneName.lastIndexOf("/") + 1);
    }

    public String getLocalNameOfCity() {
        return zoneName.substring(zoneName.indexOf("(") + 1, zoneName.indexOf(")"));
    }
}
