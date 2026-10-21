# 인도 시장 대응 계획 (2026-10-21)

요청서: 2026-10-21 "SizeScan 개발팀 전달 — 인도 시장 반영 요청". 목적은 수익보다 설치 수·리뷰 확보.

## 1. 요청 1 — 스토어 등록정보

- **적용 시점: 프로덕션 출시 때(11/1 신청 전후) 한 번에.** 테스트 기간(~10/31) 중 등록정보는 건드리지 않음.
- 등록 방식: Play Console 기본 스토어 등록정보 > 번역 관리 > **English (India) – en-IN** 추가. 이후 en-GB·en-AU 등 영어권은 en-US 기본 문안을 그대로 씀(별도 작업 불필요).
- 정책 주의(반려 방지)
  - 포털명을 쉼표로 나열하면 Play 메타데이터 정책의 "키워드 목록" 위반 → **문장 안에 자연스럽게 1회씩만** 넣음.
  - 정부 기관과 관계있는 것처럼 보이면 사칭(Misrepresentation) 위반 → 설명 끝에 **비제휴 고지문** 필수, 앱 아이콘·스크린샷에 정부 로고 사용 금지.
  - "best, #1, top" 같은 표현 금지.

### en-IN 초안 (붙여넣기용)

Title (30자, 기존 유지 권장): `SizeScan: PDF Scanner & KB Fit`

Short (79/80자):
```
Resize PDF/JPG to 200KB, 100KB or 50KB for government forms & exam applications
```

Full:
```
Scan documents and save them at exactly the file size an upload portal asks for.

Exam and government application forms often reject files over 200 KB, 100 KB, 50 KB or even 20 KB. SizeScan scans your paper, fixes edges and perspective, and creates a PDF or JPG that fits the limit while keeping the text as sharp as possible.

• Free, no watermark, no page limit, no login
• Auto edge detection, crop and cleanup
• Fit to size: 50 KB, 100 KB, 200 KB, 300 KB, 500 KB, 1 MB, 2 MB or any custom size
• PDF or JPG, from scans or from photos in your gallery
• 1 KB is counted as 1000 bytes, so files also pass portals that count 1 KB as 1024 bytes
• Your documents stay on your device

Useful when you upload certificates, mark sheets, ID proofs and signatures for applications such as UPSC, SSC, IBPS, NEET and JEE, PAN card and passport forms, or EPFO claims. Check the size limit on the portal's instructions page, pick it in SizeScan, and upload.

SizeScan is an independent app. It is not affiliated with, endorsed by or connected to any government body, exam authority or portal mentioned above.
```
(포털명 8개 모두 한 문장에 1회씩. 3번째 불릿의 300 KB는 현재 앱 프리셋과 일치.)

### 스크린샷 "PDF → 200KB"
- 현재 StoreScreenshotTest의 결과 화면(4_result)을 그대로 활용 가능. 1.0.2 빌드 때 **en-IN 전용 결과 화면(PDF, 한도 200KB, 결과 약 196KB)** 1장을 추가 생성.
- Play는 언어별 스크린샷을 따로 올릴 수 있으므로 en-IN에만 교체, 다른 언어는 현행 유지.
- 문서 예시 이름도 인도 사용자에게 익숙한 것으로(예: "Exam application", "Class 12 mark sheet") 바꿈. 실제 기관 로고·서식은 사용하지 않음.

## 2. 요청 2 — 기능 5개 반영 범위·일정

현재 코드 상태(확인함)
- 포맷 선택(PDF/JPG)은 **이미 있음**(내보내기 화면 칩).
- 용량은 "최대 KB"만 지원(SizeFitter가 한도 이하에서 가장 선명한 결과 탐색). 프리셋 50~2000KB, 직접 입력 5~50,000KB → 10KB·20KB 서명용 한도도 직접 입력으로는 지금도 가능.
- 픽셀 크기 지정, 최소 KB, 포털 프리셋은 없음.

| # | 기능 | 버전 | 예상 | 비고 |
|---|---|---|---|---|
| 2 | 범위 맞춤(최소~최대 KB) | **1.0.2** | 10/23 | 결과가 최소보다 작으면 화질을 올려 다시 맞춤. 순수 로직이라 JVM 테스트로 검증 가능 |
| 3 | 서명 KB 맞춤(기본 지원) | **1.0.2** | 10/23 | 한도 칩에 10·20KB 추가. 전용 서명 모드는 1.1 |
| - | en-IN "PDF → 200KB" 스크린샷 | **1.0.2** | 10/23 | 빌드 산출물로 생성, 적용은 출시 때 |
| 1 | 포털 프리셋(KB·픽셀·포맷 자동) | **1.1** | 출시 후 2~3주 (11월 중순~말) | 번들 JSON + 원격 JSON 갱신 |
| 3 | 서명 전용 모드(흰 배경 정리·여백 자르기·픽셀 지정) | **1.1** | 같음 | 프리셋과 함께 |
| 4 | 포맷 강제(프리셋별 JPEG/PDF 고정) | **1.1** | 같음 | 선택 UI는 이미 있으므로 프리셋에서 잠금만 추가 |
| 5 | 공식 출처·확인일 표기 | **1.1** | 같음 | JSON 항목에 source URL·checked 날짜 포함 |
| - | 원격 갱신 자동 점검(규격 변경 감시) | 1.1 이후 | 12월~ | 주기 루틴으로 출처 페이지 점검 후 JSON만 갱신 |

1.1을 출시 후로 미루는 이유: 테스트 기간 중 큰 기능 변경은 프로덕션 심사 리스크, 그리고 1.0.2는 이틀 안에 끝나야 함.

### 프리셋 데이터 구조 (1.1)
- 앱 안에 `presets.json` 번들(오프라인·첫 실행 대비) + `https://hyunjunghoon.github.io/sizescan/presets.json` 원격 파일.
- 앱은 하루 1회 이하로 원격 파일을 받아 `version`이 더 높을 때만 교체. 실패하면 번들/캐시 사용. 개인정보 전송 없음(파일 GET만) → 개인정보처리방침에 "프리셋 목록 다운로드" 한 줄 추가.
- 항목 예시:
```json
{
  "version": 3,
  "updated": "2026-11-10",
  "portals": [
    {
      "id": "pan-nsdl",
      "name": "PAN (NSDL/Protean)",
      "country": "IN",
      "items": [
        {"type": "photo", "format": "JPG", "minKb": null, "maxKb": 20, "px": [213, 213]},
        {"type": "signature", "format": "JPG", "minKb": null, "maxKb": 10, "px": [400, 200]},
        {"type": "document", "format": "PDF", "minKb": null, "maxKb": 300, "perPage": true}
      ],
      "source": "https://…공식 안내 페이지",
      "checked": "2026-10-21"
    }
  ]
}
```
(px 값은 예시. 실제 값은 31개 포털 규격표로 채움.)
- 화면 문구: "공식 규격은 바뀔 수 있습니다. 업로드 전 포털 안내를 확인하세요." + 출처 링크·확인일.

### 2호 앱(증명사진 메이커)과의 경계
- 스팸 정책 회피 원칙상 두 앱 기능이 겹치면 안 됨. **SizeScan은 문서·서명·사진의 "용량 맞춤"까지만**, 얼굴 위치 맞춤·배경 교체·규격 사진 촬영은 2호 앱 몫으로 둠.
- SSC·RRB·JKSSB는 사진 업로드를 폐지했으므로 SizeScan은 서명·서류 프리셋을 우선.

## 2-1. 규격표 반영 규칙 (2026-10-21 사용자 전달 기준)
- 파일: portal_specs_2026-10-21.csv (61행, 9개국: IN/KR/JP/US/UK/PK/BD/PH/NG). 컬럼 country, portal, item, size_min_kb, size_max_kb, pixels_or_cm, format, other_rules, source_url, verification, checked.
- 1차 프리셋: verification = official 계열(16건). official-mirror/archive/print는 각주 표기.
- secondary는 "출처 확인 필요" 라벨, secondary-conflict·unverified는 제외.
- SSC·RRB·JKSSB 사진 프리셋은 만들지 않음(라이브 촬영).
- 미검증 공식 사이트(ssc.gov.in, ibps.in, indiapost.gov.in, passportindia.gov.in, voters.eci.gov.in, abha.abdm.gov.in, pan.utiitsl.com, ctet.nic.in, gosi.kr, passport.go.kr, gov.kr, work24.go.kr)는 추후 수동 확인 대상.

### 변환 결과 (2026-10-21)
- 원본·변환본: /mnt/project-files/app1/data/ (portal_specs_2026-10-21.csv, presets.v1.json, held.md)
- presets.v1.json: 25개 포털·42개 항목. 이 중 공식 확인(verified=true) 9개 포털·13개 항목, 나머지는 "출처 확인 필요" 라벨.
- 제외 20건: KB 규정 없음·라이브 촬영, secondary-conflict, unverified, 여권·비자 사진(DS-160, UK·일본·인도 여권 → 2호 앱 몫).
- 인도 주요 시험(UPSC·SSC·IBPS·NEET·JEE)은 전부 secondary → 1.1 출시 전 공식 공고로 재확인 필요.

## 3. 필요한 것 (사용자)
- ~~31개 포털 규격표 파일~~ 수령 완료(2026-10-21).
- 출시 때 en-IN 등록정보 입력은 기존처럼 데스크톱 채팅방 복붙 프롬프트로 전달 예정.
