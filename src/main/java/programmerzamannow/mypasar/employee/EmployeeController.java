package programmerzamannow.mypasar.employee;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import programmerzamannow.mypasar.employee.dto.CreateEmploye;
import programmerzamannow.mypasar.employee.dto.ResponseEmployee;
import programmerzamannow.mypasar.shared.response.WebResponse;

@RestController
@RequestMapping("/api/v1/profile")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping(path = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public WebResponse<ResponseEmployee> create(@RequestBody CreateEmploye request) {

        ResponseEmployee responseEmployee = employeeService.create(request);
        return WebResponse.<ResponseEmployee>builder()
                .data(responseEmployee)
                .build();
    }

}
