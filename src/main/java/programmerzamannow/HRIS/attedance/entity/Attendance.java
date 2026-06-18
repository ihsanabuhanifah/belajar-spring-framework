package programmerzamannow.HRIS.attedance.entity;

import jakarta.persistence.*;
import lombok.*;
import programmerzamannow.HRIS.employee.entity.Employee;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "attendances")
public class Attendance {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    // Relasi ke Employee (Banyak catatan absen dimiliki oleh satu karyawan)
    @ManyToOne
    @JoinColumn(name = "employee_id", referencedColumnName = "id", nullable = false)
    private Employee employee;

    @Column(name = "date_bytes", nullable = false)
    private Long dateBytes; // Format: YYYYMMDD dalam bentuk angka angka (misal: 20260618)

    @Column(name = "clock_in")
    private Long clockIn; // Timestamp waktu masuk

    @Column(name = "clock_out")
    private Long clockOut; // Timestamp waktu pulang

    @Column(name = "status", length = 50, nullable = false)
    private String status; // Isi: PRESENT, LATE, SICK, LEAVE
}