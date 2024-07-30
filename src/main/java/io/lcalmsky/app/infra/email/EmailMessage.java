package io.lcalmsky.app.infra.email;

import lombok.*;

import static lombok.AccessLevel.PROTECTED;

@Data
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor
@Builder
public class EmailMessage {
    private String to;
    private String subject;
    private String message;
}
