package dev.vepo.passport.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_service", nullable = false, length = 50)
    private String sourceService;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "engage_channel_id")
    private Long engageChannelId;

    @Column(nullable = false)
    private String title;

    private String description;

    private String report;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<NotificationItem> items = new ArrayList<>();

    public Notification() {}

    public Notification(String sourceService,
                        String sourceType,
                        Long engageChannelId,
                        String title,
                        String description,
                        String report) {
        this.sourceService = sourceService;
        this.sourceType = sourceType;
        this.engageChannelId = engageChannelId;
        this.title = title;
        this.description = description;
        this.report = report;
    }

    public void addItem(NotificationItem item) {
        items.add(item);
        item.setNotification(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceService() {
        return sourceService;
    }

    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getEngageChannelId() {
        return engageChannelId;
    }

    public void setEngageChannelId(Long engageChannelId) {
        this.engageChannelId = engageChannelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public List<NotificationItem> getItems() {
        return items;
    }

    public void setItems(List<NotificationItem> items) {
        this.items = items;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        var other = (Notification) obj;
        return Objects.equals(id, other.id);
    }
}
