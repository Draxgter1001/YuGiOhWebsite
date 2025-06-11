package taf.yugioh.scanner.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_images")
public class CardImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", unique = true, nullable = false)
    private Long cardId;

    // Fixed BYTEA columns with proper annotations
    @Lob
    @Column(name = "image_data", nullable = false, columnDefinition = "bytea")
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    private byte[] imageData;

    @Lob
    @Column(name = "image_small_data", columnDefinition = "bytea")
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    private byte[] imageSmallData;

    @Column(name = "content_type")
    private String contentType = "image/jpeg";

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "small_file_size")
    private Integer smallFileSize;

    @Column(name = "original_url")
    private String originalUrl;

    @Column(name = "original_small_url")
    private String originalSmallUrl;

    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Constructors
    public CardImage() {
        this.createdAt = LocalDateTime.now();
        this.downloadedAt = LocalDateTime.now();
    }

    public CardImage(Long cardId, byte[] imageData, byte[] imageSmallData) {
        this();
        this.cardId = cardId;
        this.imageData = imageData;
        this.imageSmallData = imageSmallData;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { 
        this.imageData = imageData;
        this.fileSize = imageData != null ? imageData.length : null;
    }

    public byte[] getImageSmallData() { return imageSmallData; }
    public void setImageSmallData(byte[] imageSmallData) { 
        this.imageSmallData = imageSmallData;
        this.smallFileSize = imageSmallData != null ? imageSmallData.length : null;
    }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getFileSize() { return fileSize; }
    public void setFileSize(Integer fileSize) { this.fileSize = fileSize; }

    public Integer getSmallFileSize() { return smallFileSize; }
    public void setSmallFileSize(Integer smallFileSize) { this.smallFileSize = smallFileSize; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getOriginalSmallUrl() { return originalSmallUrl; }
    public void setOriginalSmallUrl(String originalSmallUrl) { this.originalSmallUrl = originalSmallUrl; }

    public LocalDateTime getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(LocalDateTime downloadedAt) { this.downloadedAt = downloadedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}