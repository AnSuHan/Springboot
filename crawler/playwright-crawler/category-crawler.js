// category-crawler.js (수정된 버전)
const { chromium } = require('playwright');
const { Client } = require('pg');

async function crawlCategories() {
  const client = new Client({
    host: 'localhost', port: 5432, database: 'crawler',
    user: 'postgres', password: '0000',
  });

  await client.connect();
  console.log('✅ PostgreSQL 연결 성공');

  // 1️⃣ BookCategory 테이블 + UNIQUE 제약조건 명시적 생성
  await client.query(`
    CREATE TABLE IF NOT EXISTS "BookCategory" (
      uuid_bookcategory UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      category_name VARCHAR(255) NOT NULL
    )
  `);

  // 2️⃣ UNIQUE 인덱스 추가 (ON CONFLICT용 필수!)
  await client.query(`
    CREATE UNIQUE INDEX IF NOT EXISTS idx_bookcategory_name
    ON "BookCategory" (category_name)
  `);
  console.log('✅ BookCategory 테이블 + UNIQUE 인덱스 생성 완료');

  const browser = await chromium.launch({
    headless: false,
    slowMo: 100  // 100ms 지연으로 인간다운 속도
  });
  const context = await browser.newContext({
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    viewport: { width: 1920, height: 1080 }
  });
  const page = await context.newPage();

  await page.goto('https://books.toscrape.com/', { waitUntil: 'networkidle' });

  const categories = await page.evaluate(() => {
    const links = Array.from(document.querySelectorAll('.nav-sidebar .nav-list ul li ul li a'));
    return links.map(link => link.innerText.trim()).filter(name => name);
  });

  console.log('📂 발견된 카테고리:', categories.length, '개');

  // 3️⃣ ON CONFLICT 이제 동작!
  for (const categoryName of categories.slice(0, 10)) {
    await client.query(`
      INSERT INTO "BookCategory" (category_name)
      VALUES ($1)
      ON CONFLICT (category_name) DO NOTHING
    `, [categoryName]);
    console.log(`📂 저장: ${categoryName}`);
  }

  await browser.close();
  await client.end();
  console.log('✅ 카테고리 수집 완료!');
}

crawlCategories().catch(console.error);
