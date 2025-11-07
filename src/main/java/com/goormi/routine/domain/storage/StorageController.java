// src/main/java/com/goormi/routine/storage/StorageController.java
package com.goormi.routine.domain.storage;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Validated
@Tag(name = "Storage", description = "S3 Presigned URL 발급 및 조회 API")
public class StorageController {

    private final S3Presigner presigner;

    @Value("${routineit.s3.profile-bucket}")
    private String bucket;

    @Value("${routineit.s3.presign-exp-minutes:5}")
    private long expMin;

    private static final Set<String> TYPES = Set.of(
            "image/jpeg","image/png","image/webp","image/heic","image/heif"
    );
    private static final Set<String> EXTS  = Set.of(
            "jpg","jpeg","png","webp","heic","heif"
    );

    private final S3Client s3;

    enum Visibility { PUBLIC, PRIVATE }

    private Visibility parseVisibility(String v) {
        if (v == null) return Visibility.PUBLIC;
        try { return Visibility.valueOf(v.toUpperCase()); }
        catch (IllegalArgumentException e) { return Visibility.PUBLIC; }
    }

    private String prefixFor(Visibility v) {
        return (v == Visibility.PUBLIC) ? "public" : "private";
    }
    // ---------- 공통 유틸 ----------
    private ResponseEntity<Map<String,String>> makePutUrl(String key, String contentType) {
        if (!TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().body(Map.of("error","unsupported contentType"));
        }
        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .cacheControl("no-cache")
                .build();

        PresignedPutObjectRequest pre = presigner.presignPutObject(b -> b
                .signatureDuration(Duration.ofMinutes(expMin))
                .putObjectRequest(por));

        return ResponseEntity.ok(Map.of(
                "uploadUrl", pre.url().toString(),
                "key", key,
                "expiresIn", String.valueOf(expMin * 60)
        ));
    }

    private static String safeExt(String filename) {
        String ext = filename.lastIndexOf('.') > 0
                ? filename.substring(filename.lastIndexOf('.')+1).toLowerCase()
                : "";
        return ext;
    }

    // ---------- 1) 프로필 이미지 ----------
    @Operation(
            summary = "프로필 이미지 업로드 presign 발급",
            description = "사용자 ID와 파일명을 받아 S3 업로드용 presigned PUT URL을 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "{ \"uploadUrl\": \"https://...\", \"key\": \"users/1/profile/uuid.jpg\", \"expiresIn\": \"300\" }")
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "허용되지 않은 확장자/타입")
            }
    )
    @PostMapping("/profile/presign")
    public ResponseEntity<Map<String, String>> presignProfilePut(
            @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId,
            @Parameter(description = "파일명 (확장자 필수)", required = true) @RequestParam String filename,
            @Parameter(description = "MIME 타입", example = "image/jpeg")
            @RequestParam(defaultValue = "image/jpeg") String contentType,
            @RequestParam(defaultValue = "PRIVATE") String visibility
    ) {

        String ext = safeExt(filename);
        if (!EXTS.contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("error","ext must be jpg/jpeg/png/webp/heic/heif"));
        }

        Visibility v = parseVisibility(visibility);
        String prefix = prefixFor(v);

        String key = "%s/users/%d/profile/%s.%s".formatted(prefix,userId, UUID.randomUUID(), ext);
        return makePutUrl(key, contentType);
    }

    // ---------- 2) 그룹루틴 인증샷(proof-shot) ----------
    @Operation(
            summary = "그룹루틴 인증샷 presign 발급",
            description = "그룹 ID, 사용자 ID를 받아 그룹루틴 인증샷 업로드용 presigned PUT URL을 반환합니다."
    )
    @PostMapping("/proof-shot/presign")
    public ResponseEntity<Map<String, String>> presignProofShotPut(
            @Parameter(description = "그룹 ID", required = true) @RequestParam Long groupId,
            @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId,
            @Parameter(description = "MIME 타입", example = "image/jpeg") @RequestParam(defaultValue = "image/jpeg") String contentType,
            @Parameter(description = "파일명", example = "photo.jpg") @RequestParam(defaultValue = "photo.jpg") String filename,
            @Parameter(description = "공개 비공개", example="PUBLIC/PRIVATE") @RequestParam(defaultValue = "PRIVATE") String visibility
    ) {

        String ext = safeExt(filename);
        if (!EXTS.contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("error","ext must be jpg/jpeg/png/webp/heic/heif"));
        }
        Visibility v = parseVisibility(visibility);
        String prefix = prefixFor(v);

        LocalDate d = LocalDate.now();
        String key = "%s/proof-shots/%d/%d/%d/%02d/%02d/%s.%s"
                .formatted(prefix ,groupId, userId, d.getYear(), d.getMonthValue(), d.getDayOfMonth(), UUID.randomUUID(), ext);
        return makePutUrl(key, contentType);
    }

    // ---------- 3) 그룹 채팅방 사진(group-room) ----------
    @Operation(
            summary = "그룹 채팅방 사진 presign 발급",
            description = "채팅방 ID, 사용자 ID를 받아 그룹 채팅방 사진 업로드용 presigned PUT URL을 반환합니다."
    )
    @PostMapping("/group-room/presign")
    public ResponseEntity<Map<String, String>> presignGroupRoomPut(
            @Parameter(description = "채팅방 ID", required = true) @RequestParam Long roomId,
            @Parameter(description = "사용자 ID", required = true) @RequestParam Long userId,
            @Parameter(description = "MIME 타입", example = "image/jpeg")
            @RequestParam(defaultValue = "image/jpeg") String contentType,
            @Parameter(description = "파일명", example = "photo.jpg")
            @RequestParam(defaultValue = "photo.jpg") String filename,
            @Parameter(description = "", example = "")
            @RequestParam(defaultValue = "PUBLIC") String visibility
            ) {

        String ext = safeExt(filename);
        if (!EXTS.contains(ext)) {
            return ResponseEntity.badRequest().body(Map.of("error","ext must be jpg/jpeg/png/webp/heic/heif"));
        }
        LocalDate d = LocalDate.now();
        String key = "group-rooms/%d/%d/%d/%02d/%02d/%s.%s"
                .formatted(roomId, userId, d.getYear(), d.getMonthValue(), d.getDayOfMonth(), UUID.randomUUID(), ext);
        return makePutUrl(key, contentType);
    }

    // ---------- (공통) 일시 조회용 GET 프리사인 ----------
    @Operation(
            summary = "파일 접근 presign 발급",
            description = "S3에 업로드된 객체 키를 받아 임시 접근용 presigned GET URL을 반환합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(value = "{ \"url\": \"https://...\" }")
                            )
                    )
            }
    )
    @GetMapping("/presign-get")
    public Map<String, String> presignGet(
            @Parameter(description = "S3 객체 키", required = true, example = "users/1/profile/uuid.jpg")
            @RequestParam String key,
            @Parameter(description = "다운로드 여부 (download|inline)", example = "inline")
            @RequestParam(required = false) String as) {

        boolean download = "download".equalsIgnoreCase(as);

        GetObjectRequest gor = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .responseContentDisposition(download ? "attachment" : "inline")
                .build();

        PresignedGetObjectRequest req = presigner.presignGetObject(b -> b
                .signatureDuration(Duration.ofMinutes(expMin))
                .getObjectRequest(gor));

        return Map.of("url", req.url().toString());
    }

    @Operation(
            summary = "객체 삭제",
            description = "S3 객체 키를 받아 해당 객체를 삭제합니다. 권한이 허용된 prefix(public/private) 하위만 삭제됩니다.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "삭제 성공(또는 이미 없음)"),
                    @ApiResponse(responseCode = "400", description = "허용되지 않은 키"),
                    @ApiResponse(responseCode = "403", description = "권한 없음"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            }
    )
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteObject(
            @Parameter(description = "S3 객체 키", required = true, example = "public/users/1/profile/xxx.jpg")
            @RequestParam String key
    ) {
        // 1) 키 검증: public/ 또는 private/ 로 시작하는지만 허용
        if (key == null || !(key.startsWith("public/") || key.startsWith("private/"))) {
            return ResponseEntity.badRequest().build();
        }

        try {
            DeleteObjectRequest req = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3.deleteObject(req);

            // 존재하지 않는 경우에도 S3는 204 유사 처리로 넘어갈 수 있음.
            return ResponseEntity.noContent().build(); // 204
        } catch (NoSuchKeyException e) {
            // 이미 없는 경우: 204로 동일 처리하거나 404를 주고 싶다면 아래로 변경
            return ResponseEntity.noContent().build();
        } catch (SdkException e) {
            // 권한, 네트워크 등
            return ResponseEntity.status(500).build();
        }
    }
}