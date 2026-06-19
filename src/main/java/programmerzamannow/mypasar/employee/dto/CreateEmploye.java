package programmerzamannow.mypasar.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateEmploye {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Size(max = 30, message = "Phone number must be at most 30 characters")
    private String phone;

    @NotBlank(message = "Job title is required")
    @Size(max = 30, message = "Job title must be at most 30 characters")
    private String jobTitle;

}
