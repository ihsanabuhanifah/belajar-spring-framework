package programmerzamannow.restfull.service.Contact;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.criteria.Predicate; // Untuk Predicate di dalam Specification
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import programmerzamannow.restfull.entity.Contact;
import programmerzamannow.restfull.entity.User;
import programmerzamannow.restfull.model.Contact.ContactResponse;
import programmerzamannow.restfull.model.Contact.CreateContactRequest;
import programmerzamannow.restfull.model.Contact.SearchContactRequest;
import programmerzamannow.restfull.model.Contact.UpdateContactRequest;
import programmerzamannow.restfull.repository.ContactRepository;
import programmerzamannow.restfull.service.ValidationService;

@Service
public class ContactService {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private ValidationService validationService;

    public ContactResponse create(User user, CreateContactRequest request) {
        validationService.validate(request);

        Contact contact = new Contact();
        contact.setId(UUID.randomUUID().toString());
        contact.setFirstName(request.getFirstName());
        contact.setLastName(request.getLastName());
        contact.setPhone(request.getPhone());
        contact.setEmail(request.getEmail());
        contact.setUser(user);

        contactRepository.save(contact);
        return ContactResponse.builder()
                .id(contact.getId())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .phone(contact.getPhone())
                .email(contact.getEmail())
                .build();
    }

    private ContactResponse tContactResponse(Contact contact) {
        return ContactResponse.builder()
                .id(contact.getId())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .phone(contact.getPhone())
                .email(contact.getEmail())
                .build();
    }

    @Transactional(readOnly = true)
    public ContactResponse get(User user, String id) {
        Contact contact = contactRepository.findFirstByUserAndId(user, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));

        return tContactResponse(contact);
    }

    @Transactional
    public ContactResponse update(User user, UpdateContactRequest request) {

        String id = request.getId();
        System.out.println("UpdateContactRequest " + id);
        {
            Contact contact = contactRepository.findFirstByUserAndId(user, request.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
            contact.setPhone(request.getPhone());
            contact.setEmail(request.getEmail());
            contact.setFirstName(request.getFirstName());
            contact.setLastName(request.getLastName());

            contactRepository.save(contact);
            return tContactResponse(contact);

        }

    }

    @Transactional
    public void delete(User user, String contactId) {
        Contact contact = contactRepository.findFirstByUserAndId(user, contactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));

        contactRepository.delete(contact);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> search(User user, SearchContactRequest request) {
        Specification<Contact> specification = (root, query, builder) -> {
            // 1. Perbaikan: 'List<Predicate>' harus berhuruf besar 'P' untuk tipe datanya
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("user"), user));

            if (request.getName() != null) {
                predicates.add(builder.like(root.get("firstName"), "%" + request.getName() + "%"));
                predicates.add(builder.like(root.get("lastName"), "%" + request.getName() + "%"));
            }
            if (request.getEmail() != null) {
                predicates.add(builder.like(root.get("email"), "%" + request.getEmail() + "%"));
            }
            if (request.getPhone() != null) {
                predicates.add(builder.like(root.get("phone"), "%" + request.getPhone() + "%"));
            }

            // 2. Perbaikan: Tambahkan titik koma (;) di akhir return specification ini
            return query.where(predicates.toArray(new Predicate[] {})).getRestriction();
        }; // 3. Perbaikan: Jangan lupa tutup kurung kurawal pembungkus Specification-nya
           // di sini!

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        Page<Contact> contacts = contactRepository.findAll(specification, pageable);

        // 4. Perbaikan: Hapus kelebihan kurung tutup pada 'this::toContactResponse' dan
        // pasang titik koma (;)
        List<ContactResponse> contactResponses = contacts.getContent().stream()
                .map(this::tContactResponse)
                .toList();

        return new PageImpl<>(contactResponses, pageable, contacts.getTotalElements());
    }

}