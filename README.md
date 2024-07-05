# robomi_backend

---

### 레파지터리 이용 방식 및 환경 구성 안내

* main 브랜치에 작동하는 소스 상태 유지 필요

* 별도의 환경 설정이 따로 필요한 경우, 간단히 1줄 제목 단위로라도 언급

* 실제 작동하는 코드 외에 다른 추가 내용 작성 여부 결정 반영 필요
  ```
  ※ 사례 #1: 오디오 사운드 시험 테스트 코드 작성 및 사운 파일 저장 기록
  ```
---

### 프로그램 구현 내 포함 기능

* 통신 처리: ROS 통신 연계. 클라이언트와는 웹소켓

* 영상 처리: 카메라 2대

* 음성 처리:

---

### 환경 구성

* 기본 환경: intellj, jdk-21 (외부), jdk-17 (도구 내부)

* 부가 환경: opencv 4.8(외부), java

* 기타 참고: 클라이언트 연동은 안드로이드 폰에서 진행

* 개발 도구: 인텔리제이

---

### 데이터베이스 및 테이블 생성 스크립트

```
jdbc:mariadb://robomidb.c5kcq80emm6y.ap-northeast-2.rds.amazonaws.com:3306/robomidb
---
CREATE DATABASE `robomidb`

-- robomidb.capture definition

CREATE TABLE `capture` (
  `seq` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `status` int(10) unsigned DEFAULT NULL,
  `img_path` varchar(255) DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`seq`)
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='로봇이 순찰하며 촬영한 전시물';
-- robomidb.manager definition

CREATE TABLE `manager` (
  `seq` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(20) DEFAULT NULL,
  `type` int(10) DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `img_path` varchar(255) DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`seq`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
-- robomidb.objects definition

CREATE TABLE `objects` (
  `seq` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `display` int(10) unsigned DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  `img_path` varchar(255) DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  PRIMARY KEY (`seq`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='전시물 목록';
-- robomidb.weights definition

CREATE TABLE `weights` (
  `seq` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `s3_path` varchar(255) DEFAULT NULL,
  `update_date` datetime DEFAULT NULL,
  `create_date` datetime DEFAULT NULL,
  PRIMARY KEY (`seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='학습한 Tensor 모델';
```

---

### 안드로이드 클라리언트 앱의 서버 접속 정보 일부

* ws://192.168.123.122:8080/audio
* ws://192.168.123.122:8080/video
* ws://192.168.123.122:8080/msg
* ... api/capture/allCaptures

  ※ 영상 수신 기능 시험을 위해선 서버 ip 변경 여부 검토 필요
     패키지 com.robomi.robomifront에서 VideoActivity.java, VideoActivity.java

### 스프링부트 서버의 외부 접속 정보 일부 기록

* ROS 토픽: /usb_cam/image_raw

---

### 실행 의존성 설정 관련 정보

* libs 디렉토리를 LD_LIBRARY_PATH 추가

  ※ 개발 환경이 아닌, 실행 시, /robomi_... 디렉토리 관련 별도 고려

* ROS MASTER 접속: http://localhost:11311/

  ※ 실행 시, 패키지 com.robomi.object 의 DetectingObject.java 에서
  하드코딩 주소 변경 필요 가능성 검토


