package programmerzamannow.mypasar.auth.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProfileResponse {
    private String username;
    private String role;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String jobTitle;
    private Long joinedAt;
}
