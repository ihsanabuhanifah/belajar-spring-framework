package programmerzamannow.restfull.model.Contact;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateContactRequest {

    @JsonIgnore
    @NotBlank
    private String id;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z]*$", message = "First name must contain only letters")
    private String firstName;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z]*$", message = "Last name must contain only letters")
    private String lastName;

    @NotBlank
    @Pattern(regexp = "^\\d{8,20}$", message = "Phone number must be 8-20 digits")
    private String phone;

    @Size(max = 100)
    @Email(message = "Email format is invalid")
    private String email;
}
