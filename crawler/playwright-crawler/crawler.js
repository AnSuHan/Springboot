// crawler.js (완전 수정 버전 - 테이블 자동 생성 포함)
const { chromium } = require('playwright');
const { Client } = require('pg');

async function crawlBooks() {
  const client = new Client({
    host: 'localhost',
    port: 5432,
    database: 'crawler',
    user: 'postgres',
    password: '0000',
  });

  await client.connect();
  console.log('✅ PostgreSQL 연결 성공');

  // Book 테이블 자동 생성 (존재하지 않으면)
  await client.query(`
    CREATE TABLE IF NOT EXISTS "Book" (
      uuid_book UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      title VARCHAR(500) NOT NULL,
      price NUMERIC(10,2),
      upc VARCHAR(100),
      availability VARCHAR(100),
      description TEXT,
      url TEXT NOT NULL
    )
  `);
  console.log('✅ Book 테이블 생성/확인 완료');

  const browser = await chromium.launch({
    headless: false,
    slowMo: 100  // 100ms 지연으로 인간다운 속도
  });
  const context = await browser.newContext({
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    viewport: { width: 1920, height: 1080 }
  });
  const page = await context.newPage();

  // books.toscrape.com 특정 책 페이지 접속
  await page.goto('https://books.toscrape.com/catalogue/its-only-the-himalayas_981/index.html', {
    waitUntil: 'networkidle'
  });
  console.log('🌐 페이지 로딩 완료:', page.url());

  // 책 정보 추출 (셀렉터 정확히 매칭)
  const bookData = await page.evaluate(() => ({
    title: document.querySelector('h1').innerText.trim(),
    price: document.querySelector('.product_main .price_color').innerText.trim(),
    upc: document.querySelector('.table.table-striped tr:nth-child(1) td').innerText.trim(),
    availability: document.querySelector('.table.table-striped tr:nth-child(6) td').innerText.trim(),
    description: document.querySelector('#product_description + p')?.innerText?.trim() || '',
  }));

  console.log('📖 추출된 데이터:', bookData);

  // Node.js 내장 UUID 생성
  const book = {
    uuid_book: crypto.randomUUID(),
    title: bookData.title,
    price: parseFloat(bookData.price.replace('£', '')),
    upc: bookData.upc,
    availability: bookData.availability,
    description: bookData.description,
    url: page.url(),
  };

  // PostgreSQL에 삽입
  const query = `
    INSERT INTO "Book" (uuid_book, title, price, upc, availability, description, url)
    VALUES ($1, $2, $3, $4, $5, $6, $7)
  `;

  await client.query(query, [
    book.uuid_book,
    book.title,
    book.price,
    book.upc,
    book.availability,
    book.description,
    book.url
  ]);

  console.log('📚 저장 완료:', book.title);
  console.log('💰 가격:', book.price);
  console.log('🆔 UUID:', book.uuid_book);

  await browser.close();
  await client.end();
  console.log('✅ 크롤링 + DB 저장 완료!');
}

// 실행
crawlBooks().catch(console.error);
