package programmerzamannow.mypasar.shared.validation;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@Service
public class ValidationService {

    @Autowired
    private Validator validator;

    public void validate(Object request) {
        Set<ConstraintViolation<Object>> constraintViolation = validator.validate(request);

        if (constraintViolation.size() != 0) {

            throw new ConstraintViolationException(constraintViolation);

        }

    }
}
