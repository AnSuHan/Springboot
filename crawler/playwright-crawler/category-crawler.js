const { chromium } = require('playwright');
const { Client } = require('pg');

async function crawlCategories() {
  const client = new Client({
    host: 'localhost', port: 5432, database: 'crawler',
    user: 'postgres', password: '0000',
  });

  let browser = null;
  let context = null;

  try {
    await client.connect();
    console.log('✅ PostgreSQL 연결 성공');

    // 1️⃣ BookCategory 테이블 생성 + UNIQUE 제약조건 명시적 생성
    await client.query(`
      CREATE TABLE IF NOT EXISTS "BookCategory" (
        uuid_bookcategory UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        category_name VARCHAR(255) NOT NULL
      )
    `);
    // 2️⃣ UNIQUE 인덱스 추가 (중복 체크용)
    await client.query(`
      CREATE UNIQUE INDEX IF NOT EXISTS idx_bookcategory_name
      ON "BookCategory" (category_name)
    `);
    console.log('✅ BookCategory 테이블 + UNIQUE 인덱스 생성 완료');

    browser = await chromium.launch({
      headless: false,
      slowMo: 100
    });

    context = await browser.newContext({
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      viewport: { width: 1920, height: 1080 }
    });

    const page = await context.newPage();

    // 페이지로드 및 DOM 완전 대기
    await page.goto('https://books.toscrape.com/', { waitUntil: 'networkidle' });
    await page.waitForSelector('.side_categories', { state: 'visible' });
    await page.waitForFunction(() =>
      document.querySelectorAll('.side_categories li a').length > 10
    );

    // 카테고리 추출
    const categories = await page.evaluate(() => {
      const categoryLinks = Array.from(document.querySelectorAll('.side_categories li a'));
      return categoryLinks
        .map(link => link.innerText.trim())
        .filter(name => name && name !== 'Books')
        .slice(0, -2);
    });

    console.log('📂 발견된 카테고리:', categories.length, '개');

    // 중복, 신규, 오류 카운트 변수
    let insertedCount = 0;
    let duplicateCount = 0;
    let errorCount = 0;

    for (const categoryName of categories) {
      try {
        const result = await client.query(`
          INSERT INTO "BookCategory" (category_name)
          VALUES ($1)
          ON CONFLICT (category_name) DO NOTHING
          RETURNING uuid_bookcategory
        `, [categoryName]);

        if (result.rowCount > 0) {
          insertedCount++;
          console.log(`📂 신규 저장: ${categoryName} (${insertedCount}/${categories.length})`);
        } else {
          duplicateCount++;
          console.log(`⏭️  중복 스킵: ${categoryName}`);
        }
      } catch (e) {
        errorCount++;
        console.error(`❌ 저장 오류: ${categoryName} -`, e.message);
      }
    }

    console.log(`✅ 총 ${categories.length}개 중 신규 ${insertedCount}개, 중복 ${duplicateCount}개, 오류 ${errorCount}개 저장 완료!`);

  } catch (error) {
    console.error('❌ 크롤링 오류:', error);
  } finally {
    if (context) await context.close().catch(() => {});
    if (browser) await browser.close().catch(() => {});
    await client.end().catch(() => {});
    console.log('🔒 모든 리소스 정리 완료');
  }
}

crawlCategories().catch(console.error);
