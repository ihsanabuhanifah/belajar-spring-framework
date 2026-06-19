package programmerzamannow.mypasar.employee.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseEmployee {
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String jobTitle;
    private String joinedAt;
}
