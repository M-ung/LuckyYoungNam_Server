package org.example.youngnam.domain.post.entity;

import jakarta.persistence.*;
import lombok.*;
import org.example.youngnam.domain.post.dto.request.PostRequestDTO;
import org.example.youngnam.global.base.BaseTimeEntity;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;
    @Column(name = "post_pre_content", columnDefinition = "TEXT")
    private String postPreContent; // 사용자가 등록한 내용

    @Column(name = "post_gpt_content", columnDefinition = "TEXT")
    private String postGptContent; // 지피티가 반환한 내용

    @Column(name = "post_final_content", columnDefinition = "TEXT")
    private String postFinalContent;// 최종 내용
    @Column(name = "user_id")
    private long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_status")
    private PostStatus postStatus;

    public static Post from(PostRequestDTO.PostPreContentSaveDTO requestDTO, Long userId) {
        return Post.builder()
                .postPreContent(requestDTO.postPreContent())
                .userId(userId)
                .postStatus(PostStatus.ACTIVE)
                .build();
    }
    public void savePostGptContent(String postGptContent) {
        this.postGptContent = postGptContent;
    }

    public void savePostFinalContent(String postFinalContent) {
        this.postFinalContent = postFinalContent;
    }
}
