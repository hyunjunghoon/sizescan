# 정식 출시·운영 준비

## 실제 광고 ID 전환 (정식 출시 직전, 비공개 테스트 중엔 테스트 ID 유지)
1. [사용자] AdMob > 앱 > 앱 추가 > Android, "Google Play에 등록됨" 선택 후 SizeScan 검색(검색 안 되면 "아니요"로 추가 후 나중에 연결)
2. [사용자] 광고 단위 2개: 배너(적응형) 1, 전면 1 → 앱 ID와 광고 단위 ID 3개를 스레드에 붙여넣기
3. [Claude] gradle.properties에 ID 입력 + REQUIRE_REAL_ADS=true → CI 빌드 → 서명 AAB 전달
4. [Claude] hyunjunghoon.github.io 루트에 app-ads.txt 게시 (`google.com, pub-XXXX, DIRECT, f08c47fec0942fa0`)
5. [사용자] Play Console 스토어 설정 > 웹사이트에 https://hyunjunghoon.github.io 입력 (app-ads.txt 인증용)

## 리뷰·문제 자동 점검 루틴 (출시 후)
- 1안(추천): Play Developer API 리뷰 조회. [사용자 1회] Google Cloud에서 서비스 계정 생성 → Play Console 사용자 및 권한에 "리뷰 보기·답글" 권한으로 초대 → 키 JSON을 프로젝트에 업로드. 이후 매일 자동:
  - 새 리뷰 수집 → 분류(버그/요청/칭찬/스팸) → 합당한 요청은 백로그·이슈 등록, 버그는 수정 PR → 다음 버전 반영
  - 리뷰 답글 초안 작성(언어별), 별점 추이·크래시율(Android vitals) 요약은 이상 시에만 보고
- 2안: API 없이 공개 스토어 페이지 주기 확인 (정확도 낮음, 답글 불가)
- 주간: 키워드 순위·경쟁앱 변화 시장조사 → backlog.md 갱신

## 버전 관리
- versionCode = GitHub Actions 실행 번호(자동 증가), versionName은 기능 단위로 올림
