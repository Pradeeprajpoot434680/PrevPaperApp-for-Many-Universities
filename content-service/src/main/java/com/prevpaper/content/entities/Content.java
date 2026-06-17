//package com.prevpaper.content.entities;
//
//import com.prevpaper.comman.enums.ContentType;
//import com.prevpaper.content.enums.FileType;
//import com.prevpaper.content.enums.VerificationStatus;
//import jakarta.persistence.*;
//import lombok.*;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//import java.util.UUID;
//
//@Entity
//@Table(name = "contents")
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class Content {
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private UUID id; // [cite: 11]
//
//    private String title; // [cite: 12]
//    private String description; // [cite: 13]
//
//    @Enumerated(EnumType.STRING)
//    private ContentType contentType; //
//
//    // Educational Hierarchy [cite: 15-20]
//    private UUID universityId;
//    private UUID departmentId;
//    private UUID programId;
//    private Integer academicYear;
//    private Integer semester;
//    private UUID subjectId;
//
//    private UUID teacherId; // Nullable [cite: 21]
//    private UUID uploadedBy; // User UUID from Gateway [cite: 22]
//    private String fileUrl; // Initially NULL
//
//    @Enumerated(EnumType.STRING)
//    private FileType fileType; // PDF, IMAGE [cite: 24]
//
//    @Enumerated(EnumType.STRING)
//    private VerificationStatus verificationStatus; // [cite: 25]
//
//    private UUID verifiedBy; // [cite: 26]
//    private LocalDateTime verifiedAt; // [cite: 27]
//
//    @CreationTimestamp
//    private LocalDateTime createdAt; // [cite: 28]
//
//    @UpdateTimestamp
//    private LocalDateTime updatedAt; // [cite: 29]
//}


package com.prevpaper.content.entities;

import com.prevpaper.comman.enums.ContentType;
import com.prevpaper.content.enums.FileType;
import com.prevpaper.content.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    // Educational Hierarchy
    private UUID universityId;
    private UUID departmentId;
    private UUID programId;
    private Integer academicYear;
    private Integer semester;
    private UUID subjectId;

    // 🟢 ADDED: Missing structural relationship identifier column
    private UUID examTypeId;

    private UUID teacherId; // Nullable
    private UUID uploadedBy; // User UUID from Gateway
    private String fileUrl; // Initially NULL

    @Enumerated(EnumType.STRING)
    private FileType fileType; // PDF, IMAGE

    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    private UUID verifiedBy;
    private LocalDateTime verifiedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}