// package programmerzamannow.restfull.controller;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;

// // --- PERBAIKAN IMPORT AUTOCONFIGURE ---
// // Tambahkan .servlet di tengah-tengahnya seperti ini:
// import
// org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.test.web.servlet.MockMvc;

// // Import Static untuk MockMvc
// import static
// org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
// import static
// org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// import static
// org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
// import static org.junit.jupiter.api.Assertions.*;

// // --- PERBAIKAN IMPORT JACKSON AMA ---
// // Karena proyek Mas Eko menggunakan library modern tools.jackson, ganti
// alamatnya ke sini:
// import tools.jackson.core.type.TypeReference;
// import tools.jackson.databind.ObjectMapper;

// import programmerzamannow.restfull.entity.User;
// import programmerzamannow.restfull.model.RegisterUserRequest;
// import programmerzamannow.restfull.model.WebResponse;
// import programmerzamannow.restfull.repository.UserRepository;
// import programmerzamannow.restfull.security.BCrypt;

// @SpringBootTest
// @AutoConfigureMockMvc
// public class UserControllerTest {

// @Autowired
// private MockMvc mockMvc;

// @Autowired
// private UserRepository userRepository;

// @Autowired
// private ObjectMapper objectMapper;

// @BeforeEach
// void setUp() {
// // Memastikan database bersih sebelum setiap fungsi test berjalan
// userRepository.deleteAll();
// }

// @Test
// void testRegisterSuccess() throws Exception {
// RegisterUserRequest request = new RegisterUserRequest();
// request.setUsername("john");
// request.setPassword("password");
// request.setName("John Doe");

// mockMvc.perform(
// post("/api/users/register") // Sudah disesuaikan dengan@PostMapping
// UserController Ama
// .contentType("application/json")
// .content(objectMapper.writeValueAsString(request)))
// .andExpect(
// status().isOk())
// .andDo(
// print())
// .andDo(result -> {
// String jsonResponse = result.getResponse().getContentAsString();
// WebResponse<String> response = objectMapper.readValue(jsonResponse,
// new TypeReference<WebResponse<String>>() {
// });

// // Memastikan response data isinya "OK" sesuai kembalian UserController Ama
// assertEquals("OK", response.getData());
// assertNull(response.getErrors());
// });
// }

// @Test
// void testRegisterBadRequestUsernameAlreadyExists() throws Exception {
// // 1. Kita masukkan dulu user "john" secara manual ke database
// User existingUser = new User();
// existingUser.setUsername("john");
// existingUser.setPassword(BCrypt.hashpw("rahasia", BCrypt.gensalt()));
// existingUser.setName("John Clone");
// userRepository.save(existingUser);

// // 2. Kita kirim request registrasi baru dengan username "john" yang sama
// RegisterUserRequest request = new RegisterUserRequest();
// request.setUsername("john"); // Memicu throw ResponseStatusException
// request.setPassword("password");
// request.setName("John Doe");

// mockMvc.perform(
// post("/api/users/register")
// .contentType("application/json")
// .content(objectMapper.writeValueAsString(request)))
// .andExpect(
// status().isBadRequest() // Memastikan HTTP Status yang kembali adalah 400 Bad
// Request
// ).andDo(
// print());
// }
// }