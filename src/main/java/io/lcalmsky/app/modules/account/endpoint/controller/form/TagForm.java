package io.lcalmsky.app.modules.account.endpoint.controller.form;


import lombok.*;

import static lombok.AccessLevel.PROTECTED;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagForm {
    private String tagTitle;
}
