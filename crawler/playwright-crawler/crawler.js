const { chromium } = require('playwright');
const { Client } = require('pg');

// 1. 상수 정의
const BROWSER_OPTIONS = {
  headless: false,
  slowMo: 100 // 100ms 지연으로 인간다운 속도
};

const CONTEXT_OPTIONS = {
  userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  viewport: { width: 1920, height: 1080 }
};

const DB_CONFIG = {
  host: 'localhost',
  port: 5432,
  database: 'crawler',
  user: 'postgres',
  password: '0000',
};
const BASE_URL = 'https://books.toscrape.com/';


// 2. 헬퍼 함수 정의

/**
 * 주어진 범위 [minMs, maxMs] 내에서 랜덤하게 딜레이합니다.
 * @param {number} minMs 최소 딜레이 시간 (밀리초, 기본값: 1500)
 * @param {number} maxMs 최대 딜레이 시간 (밀리초, 기본값: 4000)
 */
async function randomDelay(minMs = 1500, maxMs = 4000) {
    // Math.random() * (max - min) + min
    const delayTime = Math.random() * (maxMs - minMs) + minMs;
    console.log(`⏳ 다음 작업 전 ${delayTime.toFixed(0)}ms 대기...`);
    return new Promise(resolve => setTimeout(resolve, delayTime));
}


/**
 * DB에 필요한 테이블이 없으면 생성합니다. (스키마 오류 해결을 위해 DROP 후 CREATE)
 * @param {Client} client
 */
async function setupDatabase(client) {
    // 💡 스키마 오류 해결: 기존 테이블 삭제 후 재생성
    await client.query(`DROP TABLE IF EXISTS "Book"`);
    await client.query(`DROP TABLE IF EXISTS "BookCategory"`);

    // 1. BookCategory 테이블 (카테고리 정보) - url_path 컬럼 포함
    await client.query(`
        CREATE TABLE "BookCategory" (
            uuid_bookcategory UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            category_name VARCHAR(255) NOT NULL UNIQUE,
            url_path TEXT NOT NULL
        )
    `);

    // 2. Book 테이블 (책 정보)
    await client.query(`
        CREATE TABLE "Book" (
            uuid_book UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            title VARCHAR(500) NOT NULL,
            price NUMERIC(10,2),
            upc VARCHAR(100) UNIQUE,
            availability VARCHAR(100),
            description TEXT,
            category_name VARCHAR(255) NOT NULL,
            url TEXT NOT NULL
        )
    `);
    console.log('✅ DB 테이블 (Book, BookCategory) 재생성/확인 완료');
}

/**
 * 메인 페이지에서 모든 카테고리를 추출하여 DB에 저장합니다.
 * @param {import('playwright').Page} page
 * @param {Client} client
 */
async function crawlAndSaveCategories(page, client) {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });
    await page.waitForSelector('.side_categories', { state: 'visible' });

    // 카테고리 추출 (이름과 상대 URL 경로)
    const categories = await page.evaluate(() => {
        const categoryLinks = Array.from(document.querySelectorAll('.side_categories li a'));
        return categoryLinks
            .map(link => ({
                name: link.innerText.trim(),
                path: link.getAttribute('href')
            }))
            .filter(cat => cat.name && cat.name !== 'Books' && cat.path);
    });

    console.log(`\n📂 발견된 카테고리: ${categories.length}개`);
    let insertedCount = 0;

    for (const cat of categories) {
        try {
            // ON CONFLICT를 사용하여 중복 카테고리는 업데이트 (url_path) 하거나 무시
            const result = await client.query(`
                INSERT INTO "BookCategory" (category_name, url_path)
                VALUES ($1, $2)
                ON CONFLICT (category_name) DO UPDATE SET url_path = EXCLUDED.url_path
                RETURNING uuid_bookcategory
            `, [cat.name, cat.path]);

            if (result.rowCount > 0) {
                insertedCount++;
            }
        } catch (e) {
            console.error(`❌ 카테고리 저장 오류: ${cat.name} -`, e.message);
        }
    }
    console.log(`✅ 카테고리 ${insertedCount}개 신규 저장/확인 완료.`);
}

/**
 * 개별 책 상세 페이지에서 정보를 추출합니다.
 * @param {import('playwright').Page} page
 * @returns {object} 추출된 책 데이터
 */
async function extractBookDetail(page) {
    // 상세 페이지로 이동 후 DOM이 안정화될 때까지 대기
    await page.waitForSelector('.product_main h1', { state: 'visible', timeout: 30000 });

    const bookData = await page.evaluate(() => {
        const title = document.querySelector('h1').innerText.trim();
        const priceText = document.querySelector('.product_main .price_color').innerText.trim();
        const upc = document.querySelector('.table.table-striped tr:nth-child(1) td').innerText.trim();
        const availability = document.querySelector('.table.table-striped tr:nth-child(6) td').innerText.trim();
        const description = document.querySelector('#product_description + p')?.innerText?.trim() || '';

        // 카테고리 이름 추출
        const breadcrumbs = Array.from(document.querySelectorAll('.breadcrumb li a'));
        const categoryName = breadcrumbs.length >= 3 ? breadcrumbs[2].innerText.trim() : null;

        return {
            title,
            price: parseFloat(priceText.replace('£', '')),
            upc,
            availability,
            description,
            categoryName
        };
    });

    bookData.url = page.url(); // 현재 URL 추가
    return bookData;
}


/**
 * 특정 카테고리의 모든 페이지를 순회하며 책 정보를 수집하고 페이지 단위로 DB에 Flush 합니다.
 * @param {import('playwright').Page} page
 * @param {Client} client
 * @param {object} category
 */
async function crawlCategoryPages(page, client, category) {
    let nextUrl = new URL(category.url_path, BASE_URL).href;
    let bookCount = 0;
    let duplicateCount = 0;
    const categoryName = category.category_name;

    console.log(`\n======================================================`);
    console.log(`🚀 카테고리 크롤링 시작: ${categoryName}`);
    console.log(`======================================================`);

    while (nextUrl) {
        const currentUrl = nextUrl;
        console.log(`\n🌐 페이지 이동: ${currentUrl}`);

        // 목록 페이지 로드
        try {
            await page.goto(currentUrl, { waitUntil: 'networkidle' });
        } catch (e) {
            console.error(`❌ 목록 페이지 로딩 오류 (${currentUrl}): ${e.message}. 현재 카테고리 크롤링을 종료합니다.`);
            break;
        }

        // 책 목록 추출 (href 속성만 추출)
        const bookHrefs = await page.evaluate(() => {
            return Array.from(document.querySelectorAll('.product_pod h3 a'))
                .map(a => a.getAttribute('href'));
        });

        const bookUrls = bookHrefs.map(href =>
            new URL(href, currentUrl).href
        );

        if (bookUrls.length === 0) {
            console.log('ℹ️ 현재 목록 페이지에 책 정보가 없습니다. 현재 카테고리 크롤링을 종료합니다.');
            break;
        }

        console.log(`📚 현재 페이지에서 ${bookUrls.length}권의 책 발견`);

        // 개별 책 상세 페이지 크롤링 및 수집
        const pageBookDetails = [];
        let pageSuccessCount = 0;
        let pageDuplicateCount = 0;

        for (const detailUrl of bookUrls) {

            // 💡 랜덤 딜레이 적용 (책 상세 페이지 간 전환 속도 제어)
            await randomDelay();

            // 최대 3회 재시도 루프
            for (let attempt = 1; attempt <= 3; attempt++) {
                try {
                    await page.goto(detailUrl, { waitUntil: 'networkidle', timeout: 30000 });
                    const bookDetail = await extractBookDetail(page);

                    if (!bookDetail.categoryName) {
                        console.error(`❌ 경고: 카테고리 정보를 찾을 수 없습니다. 스킵합니다. URL: ${detailUrl}`);
                        break; // 재시도 없이 다음 책으로 이동
                    }

                    // 💡 DB에 즉시 삽입 대신 배열에 수집
                    pageBookDetails.push(bookDetail);

                    // 성공했으므로 재시도 루프 탈출
                    break;

                } catch (e) {
                    const errorMsg = e.message;
                    console.error(`❌ 상세 페이지 크롤링 오류 (시도 ${attempt}/3): ${detailUrl} - ${errorMsg.substring(0, Math.min(errorMsg.length, 100))}...`);

                    // 오류 복구 로직: 'closed', '404', 'Timeout' 발생 시 현재 목록 페이지로 돌아가 재시도
                    if (attempt < 3 && (errorMsg.includes('closed') || errorMsg.includes('404') || errorMsg.includes('Timeout'))) {
                        console.log(`🔄 오류 복구 시도: 현재 목록 페이지 (${currentUrl})를 다시 로드합니다.`);
                        await page.goto(currentUrl, { waitUntil: 'networkidle' });
                    } else {
                        console.error(`🛑 치명적인 오류 발생 또는 재시도 횟수 초과. 다음 책으로 이동합니다.`);
                        break;
                    }
                }
            } // end of attempt loop
        } // end of bookUrls loop

        // 💡 여기서 페이지 단위 FLUSH (DB 트랜잭션 시작)
        console.log(`\n💾 페이지 단위 FLUSH 시작: ${pageBookDetails.length}권`);

        try {
            await client.query('BEGIN');

            for (const bookDetail of pageBookDetails) {
                // Book 테이블에 삽입 (UPC 중복 시 스킵)
                const bookQuery = `
                    INSERT INTO "Book" (title, price, upc, availability, description, category_name, url)
                    VALUES ($1, $2, $3, $4, $5, $6, $7)
                    ON CONFLICT (upc) DO NOTHING
                    RETURNING uuid_book;
                `;

                const bookRes = await client.query(bookQuery, [
                    bookDetail.title,
                    bookDetail.price,
                    bookDetail.upc,
                    bookDetail.availability,
                    bookDetail.description,
                    bookDetail.categoryName,
                    bookDetail.url
                ]);

                if (bookRes.rowCount > 0) {
                    pageSuccessCount++;
                } else {
                    pageDuplicateCount++;
                }
            }

            await client.query('COMMIT');
            console.log(`✅ FLUSH 완료: 신규 ${pageSuccessCount}권, 중복 ${pageDuplicateCount}권`);

            // 누적 카운트 업데이트
            bookCount += pageSuccessCount;
            duplicateCount += pageDuplicateCount;

        } catch (flushError) {
            await client.query('ROLLBACK');
            console.error(`❌ FLUSH 오류 발생 (ROLLBACK):`, flushError.message);
        }


        // 다음 페이지 URL 찾기
        const nextButton = await page.locator('.pager .next a');
        if (await nextButton.isVisible()) {
            const nextHref = await nextButton.getAttribute('href');
            nextUrl = new URL(nextHref, currentUrl).href;
        } else {
            nextUrl = null;
        }

        // 목록 페이지 전환 전 딜레이
        await randomDelay(500, 1000);
    } // end of while (nextUrl)

    console.log(`✅ 카테고리 ${categoryName} 크롤링 완료. 신규 ${bookCount}권 저장.`);
    return { bookCount, duplicateCount };
}


// 3. 메인 함수 정의
async function crawlAllBooks() {
    // [가정] DB_CONFIG 상수는 이 스크립트 상단에 정의되어 있다고 가정합니다.
    const DB_CONFIG = { host: 'localhost', port: 5432, database: 'crawler', user: 'postgres', password: '0000' };
    const client = new Client(DB_CONFIG);
    let browser = null;
    let context = null;
    let page = null;
    let totalBookCount = 0;
    let totalDuplicateCount = 0;

    try {
        await client.connect();
        await setupDatabase(client);

        const { chromium } = require('playwright');
        // [가정] BROWSER_OPTIONS, CONTEXT_OPTIONS 상수는 정의되어 있다고 가정합니다.
        const BROWSER_OPTIONS = { headless: false, slowMo: 100 };
        const CONTEXT_OPTIONS = {
            userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            viewport: { width: 1920, height: 1080 }
        };

        browser = await chromium.launch(BROWSER_OPTIONS);
        context = await browser.newContext(CONTEXT_OPTIONS);
        page = await context.newPage();

        // 1단계: 카테고리 정보 수집 및 DB 저장
        await crawlAndSaveCategories(page, client);

        console.log('\n--- 책 상세 정보 크롤링 시작 ---');

        // 2단계: DB에서 저장된 카테고리 목록을 로드
        const categoryRes = await client.query('SELECT category_name, url_path FROM "BookCategory"');
        const categories = categoryRes.rows;

        // 3단계: 카테고리별로 순회하며 책 정보 수집 (카테고리별 Flush 효과)
        for (const category of categories) {
             // 각 카테고리 루프가 끝날 때마다 데이터는 페이지 단위로 DB에 완전히 반영됨
             const result = await crawlCategoryPages(page, client, category);
             totalBookCount += result.bookCount;
             totalDuplicateCount += result.duplicateCount;
        }

        console.log('\n--- 최종 크롤링 완료 ---');
        console.log(`✅ 총 ${totalBookCount}권의 신규 책 정보 저장 완료.`);
        console.log(`ℹ️ ${totalDuplicateCount}건의 중복 책 정보 스킵됨.`);

    } catch (error) {
        console.error('❌ 전체 크롤링 중 치명적인 오류 발생:', error);
    } finally {
        if (page) await page.close().catch(() => {});
        if (context) await context.close().catch(() => {});
        if (browser) await browser.close().catch(() => {});
        if (client) await client.end().catch(() => {});
        console.log('🔒 모든 리소스 정리 완료');
    }
}

// 실행
crawlAllBooks().catch(console.error);