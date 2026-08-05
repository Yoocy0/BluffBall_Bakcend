package com.project.bluffball.domain.user.service;

import com.project.bluffball.domain.user.dto.response.UserProfileResponse;
import com.project.bluffball.domain.user.dto.response.UserRecordsResponse;
import com.project.bluffball.domain.user.service.usecase.reader.UserReader;
import com.project.bluffball.domain.user.service.usecase.reader.UserRecordReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 유저 프로필·전적 조회 서비스.
 *
 * <p>usecase Reader를 조립하며 Entity·Repository에 직접 접근하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /** 유저 기본 정보 Reader */
    private final UserReader userReader;

    /** 유저 전적 Reader */
    private final UserRecordReader userRecordReader;

    /**
     * 본인 기본 프로필(닉네임·재화 등)을 조회한다.
     *
     * @param userId 인증된 유저 ID
     * @return 프로필 응답
     */
    public UserProfileResponse getMyProfile(Long userId) {
        return userReader.getProfileResponse(userId);
    }

    /**
     * 지정 유저의 타자·투수 전적을 조회한다.
     *
     * @param userId 조회 대상 유저 ID
     * @return 전적 응답
     */
    public UserRecordsResponse getRecords(Long userId) {
        return userRecordReader.getRecordsResponse(userId);
    }
}
