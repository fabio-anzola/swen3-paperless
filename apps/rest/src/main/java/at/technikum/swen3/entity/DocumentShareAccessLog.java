package at.technikum.swen3.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "document_share_access_log")
@Getter
@Setter
@NoArgsConstructor
public class DocumentShareAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "share_id", nullable = false)
    private DocumentShare share;

    @Column(name = "accessed_at", nullable = false)
    private Instant accessedAt;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "remote_address", length = 255)
    private String remoteAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(length = 255)
    private String reason;
}
