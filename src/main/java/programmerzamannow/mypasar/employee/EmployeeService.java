package programmerzamannow.mypasar.employee;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;
import programmerzamannow.mypasar.auth.UserRepository;
import programmerzamannow.mypasar.auth.entity.User;
import programmerzamannow.mypasar.employee.dto.CreateEmploye;
import programmerzamannow.mypasar.employee.dto.ResponseEmployee;
import programmerzamannow.mypasar.employee.entity.Employee;
import programmerzamannow.mypasar.shared.jwt.DecodeJwtService;
import programmerzamannow.mypasar.shared.validation.ValidationService;

@Service
@Slf4j
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private ValidationService validationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DecodeJwtService decodeJwtService;

    @Transactional
    public ResponseEmployee create(CreateEmploye request) {

        validationService.validate(request);

        if (employeeRepository.findByUsername(decodeJwtService.getCurrentName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee already exists");
        }

        Employee employee = new Employee();
        employee.setId(UUID.randomUUID().toString());
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setJobTitle(request.getJobTitle());
        employee.setPhone(request.getPhone());
        employee.setJoinedAt(System.currentTimeMillis());

        employee.setUsername(decodeJwtService.getCurrentName());

        employeeRepository.save(employee);

        return ResponseEmployee.builder()
                .employeeId(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .jobTitle(employee.getJobTitle())
                .joinedAt(String.valueOf(employee.getJoinedAt()))
                .build();
    }

}
