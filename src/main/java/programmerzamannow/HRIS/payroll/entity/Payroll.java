package programmerzamannow.HRIS.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import programmerzamannow.HRIS.employee.entity.Employee;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payrolls")
public class Payroll {

    @Id
    @Column(name = "id", length = 100)
    private String id;

    // Relasi ke Employee (Satu karyawan bisa punya banyak riwayat gaji bulanan)
    @ManyToOne
    @JoinColumn(name = "employee_id", referencedColumnName = "id", nullable = false)
    private Employee employee;

    @Column(name = "period", length = 20, nullable = false)
    private String period; // Format: YYYY-MM (Contoh: 2026-06)

    @Column(name = "basic_salary", nullable = false)
    private Long basicSalary;

    @Column(name = "allowance")
    private Long allowance; // Tunjangan

    @Column(name = "deduction")
    private Long deduction; // Potongan (karena telat/absen)

    @Column(name = "net_salary", nullable = false)
    private Long netSalary; // Gaji bersih (basic + allowance - deduction)

    @Column(name = "paid_at")
    private Long paidAt; // Tanggal transfer (Epoch Time)

    @Column(name = "status", length = 50, nullable = false)
    private String status; // Isi: PENDING, PAID

    // --- MEKANISME OPTIMISTIC LOCKING UNTUK KEUANGAN ---
    @Version
    @Column(name = "version")
    private Long version;
}