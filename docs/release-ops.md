# 정식 출시·운영 준비

## 실제 광고 ID 전환 (정식 출시 직전, 비공개 테스트 중엔 테스트 ID 유지)
테스트 기간에 실제 ID를 쓰면 테스터(특히 유료 테스터)의 노출·클릭이 무효 트래픽으로 잡혀 AdMob 계정이 제한될 수 있다. 그래서 전환은 프로덕션 출시 빌드에서만 한다. AdMob 앱·광고 단위 생성 자체는 미리 해 둬도 무방.

### [사용자] AdMob 화면 순서 (apps.admob.com, 10분 정도)
1. 왼쪽 메뉴 **앱** → **앱 추가**
2. 플랫폼 **Android** → "지원되는 앱 스토어에 등록되어 있나요?" → 아직 공개 전이므로 **아니요** → 앱 이름 `SizeScan` → **앱 추가** (출시 후 **앱 설정 > 앱 스토어 세부정보 > 추가**로 com.tallowbird.sizescan 연결)
3. 만들어진 앱에서 **광고 단위 추가** → **배너** → 이름 `home_banner` → **광고 단위 만들기** → 완료
4. 다시 **광고 단위 추가** → **전면 광고** → 이름 `result_interstitial` → 만들기 → 완료
5. 왼쪽 메뉴 **개인 정보 보호 및 메시지** → **GDPR** → **메시지 만들기** → 앱 SizeScan 선택, 언어 기본값 → 개인정보처리방침 URL `https://hyunjunghoon.github.io/sizescan/privacy.html` → **게시** (EU/영국 사용자 동의 화면. 앱의 UMP 코드는 이미 들어 있음)
6. 같은 메뉴 **미국 주 규정**도 **메시지 만들기 → 게시** (선택, 권장)
7. 아래 4개를 스레드에 붙여넣기: 앱 ID(`ca-app-pub-…~…`), 배너 ID, 전면 ID(`ca-app-pub-…/…`), 게시자 ID(`pub-…`, **설정 > 계정 정보**)
8. Play Console **스토어 설정 > 스토어 등록정보 연락처 > 웹사이트**에 `https://hyunjunghoon.github.io` 입력 (app-ads.txt 확인용)

### [Claude] 받은 뒤 자동 처리
1. gradle.properties에 `ADMOB_APP_ID`, `ADMOB_BANNER`, `ADMOB_INTERSTITIAL` 입력, `REQUIRE_REAL_ADS=true` (테스트 ID가 남아 있으면 빌드 실패하도록 되어 있음)
2. `VERSION_NAME` 확인 → main 푸시 → CI 빌드(versionCode 자동 증가) → 컨테이너에서 업로드 키로 jarsigner 서명 → 매니페스트 package·APPLICATION_ID 메타데이터 확인
3. hyunjunghoon.github.io 저장소 루트에 `app-ads.txt` 게시: `google.com, pub-XXXXXXXXXXXXXXXX, DIRECT, f08c47fec0942fa0`
4. 서명 AAB를 /mnt/project-files/app1-builds/ 에 두고 스레드로 전달 → 사용자가 프로덕션 트랙에 업로드

## 리뷰·문제 자동 점검 루틴 (출시 후)
확인 결과(2026-10-14): 컨테이너에서 play.google.com 직접 접속은 정책 차단, androidpublisher/oauth2.googleapis.com 은 접속 가능. WebFetch로 스토어 페이지를 읽으면 대표 리뷰 3개 정도와 평균 별점·리뷰 수만 보임(개별 별점·전체 목록 없음).

- **1단계(서비스 계정 없이, 출시 즉시 시작)**: 주 2회 루틴이 WebFetch로 스토어 페이지(en, ko, 주요 언어 hl=)를 읽어 평균 별점·리뷰 수 변화와 보이는 리뷰를 기록(/mnt/project-files/app1/reviews/log.md). 별점 하락·새 불만 리뷰가 보이면 스레드에 보고, 합당한 요청은 backlog/수정. 한계: 일부 리뷰만 보임, 답글 불가.
- **2단계(리뷰가 쌓이면, 추천)**: Play Developer API. [사용자 1회] Google Cloud에서 서비스 계정 생성 → 키 JSON 업로드 → Play Console **사용자 및 권한**에 그 계정을 "리뷰 보기·답글" 권한으로 초대. 이후 매일 자동:
  - 새 리뷰 전부 수집 → 분류(버그/요청/칭찬/스팸) → 합당한 요청은 backlog·이슈, 버그는 수정 후 다음 버전
  - 언어별 답글 작성·게시, 별점 추이·크래시율은 이상 시에만 보고
- 주간: 키워드 순위·경쟁앱 변화 시장조사 → backlog.md 갱신

## 버전 관리
- versionCode = GitHub Actions 실행 번호(자동 증가), versionName은 기능 단위로 올림

## 버전 코드
- versionCode = GitHub Actions run_number (android.yml의 ORG_GRADLE_PROJECT_VERSION_CODE). 실행마다 증가하므로 다음 빌드는 항상 이전보다 큼.
- 1.0.0 첫 업로드는 버전 코드 15 (커밋 eac220f, 2026-10-14). 다음 업로드는 16 이상이 자동 보장됨.
- versionName은 VERSION_NAME 속성(기본 1.0.0). 버전 올릴 때 gradle.properties에 VERSION_NAME 추가.
