# 포털별 서류 업로드 규격표 (SizeScan 프리셋용)
확인일: 2026-10-21 / 파일: portal_specs_2026-10-21.csv (UTF-8, 61행)

## 컬럼
- country: ISO 국가코드 (IN/KR/JP/US/UK/PK/BD/PH/NG)
- portal: 포털·시험명
- item: photo / signature / document / certificate / thumb impression 등
- size_min_kb / size_max_kb: KB 범위 (빈칸 = 규정 없음)
- pixels_or_cm: 픽셀 또는 인쇄 치수·dpi
- format: 허용 포맷
- other_rules: 배경색·잉크·파일명 등 부가 규정, 상충 메모
- source_url: 출처
- verification: 신뢰 등급 (아래)
- checked: 확인일

## verification 등급
- official: 공식 사이트·공고 PDF에서 직접 확인 → 프리셋 1차 반영
- official-mirror / official-archive / official-print: 공식 문서지만 미러·아카이브·인쇄기준 → 1차 반영 가능, 각주 표기
- secondary: 교육·언론 사이트가 공식 공고를 인용 → "출처 확인 필요" 라벨로 반영
- secondary-conflict: 출처 간 수치 상충 → 보류
- unverified: 숫자 규격 없음 → 프리셋 제외

## 프리셋 설계 권장
1. 1차 출시 프리셋: verification = official 계열 (16건)
2. 범위형 규격(min~max)이 많음 → "목표 KB" 하나가 아니라 범위 안으로 맞추는 로직 필요
3. SSC·RRB·JKSSB 사진은 라이브 촬영(파일 업로드 없음) → 사진 프리셋 만들지 말 것
4. 프리셋 데이터는 앱 밖(JSON/원격 설정)에 두어 규격 변경 시 앱 업데이트 없이 갱신
5. 프리셋 화면에 source_url + checked 표기

## 공식 사이트 접근 차단으로 미검증 (추후 수동 확인 필요)
ssc.gov.in, ibps.in, indiapost.gov.in, passportindia.gov.in, voters.eci.gov.in,
abha.abdm.gov.in, pan.utiitsl.com, ctet.nic.in, gosi.kr, passport.go.kr, gov.kr, work24.go.kr
