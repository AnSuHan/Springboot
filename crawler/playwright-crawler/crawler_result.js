// crawler_result.js

// PostgreSQL 클라이언트 모듈만 사용
const { Client } = require('pg');

// 데이터베이스 설정 (기존 크롤러에서 사용된 설정과 동일해야 함)
const DB_CONFIG = {
  host: 'localhost',
  port: 5432,
  database: 'crawler',
  user: 'postgres',
  password: '0000',
};

/**
 * BookCategory 테이블의 모든 데이터를 조회하여 Pretty JSON으로 출력합니다.
 */
async function dumpBookCategoriesToJson() {
    const client = new Client(DB_CONFIG);
    try {
        await client.connect();
        console.log('✅ PostgreSQL 연결 성공');

        const query = 'SELECT * FROM "BookCategory"';
        const result = await client.query(query);
        const categories = result.rows;

        console.log(`\n--- BookCategory 데이터 (${categories.length}개) ---\n`);
        // JSON.stringify(data, replacer, space)를 사용하여 Pretty 출력
        console.log(JSON.stringify(categories, null, 2));

    } catch (error) {
        console.error('❌ BookCategory 데이터 덤프 중 오류 발생:', error);
    } finally {
        if (client) await client.end().catch(() => {});
        console.log('\n🔒 DB 연결 종료');
    }
}

// ----------------------------------------------------

/**
 * Book 테이블의 모든 데이터를 조회하여 Pretty JSON으로 출력합니다.
 */
async function dumpBooksToJson() {
    const client = new Client(DB_CONFIG);
    try {
        await client.connect();
        console.log('✅ PostgreSQL 연결 성공');

        const query = 'SELECT * FROM "Book"';
        const result = await client.query(query);
        const books = result.rows;

        console.log(`\n--- Book 데이터 (${books.length}개) ---\n`);
        // Pretty JSON 출력
        console.log(JSON.stringify(books, null, 2));

    } catch (error) {
        console.error('❌ Book 데이터 덤프 중 오류 발생:', error);
    } finally {
        if (client) await client.end().catch(() => {});
        console.log('\n🔒 DB 연결 종료');
    }
}

// ----------------------------------------------------

// 명령줄 인수에 따라 실행할 함수를 결정합니다.
const command = process.argv[2];

if (command === 'dump:categories') {
    dumpBookCategoriesToJson().catch(console.error);
} else if (command === 'dump:books') {
    dumpBooksToJson().catch(console.error);
} else {
    console.log('사용법: node crawler_result.js [dump:categories | dump:books]');
}