import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

/**
 * 날짜 기반 파일명 생성 헬퍼
 * 예: 테스트데이터_생성_20240520143000
 */
const getTimestampFilename = (prefix = '테스트데이터_생성') => {
    const now = new Date();
    const yyyy = now.getFullYear();
    const mm = String(now.getMonth() + 1).padStart(2, '0');
    const dd = String(now.getDate()).padStart(2, '0');
    const hh = String(now.getHours()).padStart(2, '0');
    const min = String(now.getMinutes()).padStart(2, '0');
    const ss = String(now.getSeconds()).padStart(2, '0');
    return `${prefix}_${yyyy}${mm}${dd}${hh}${min}${ss}`;
};

/**
 * Helper to download data in various formats
 */
const ExportUtils = {
    // 1. JSON Export
    downloadJson: (data, filename) => {
        if (!data || Object.keys(data).length === 0) {
            alert('생성된 데이터가 없어 다운로드할 수 없습니다.');
            return;
        }

        const finalFilename = filename || getTimestampFilename();
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        saveAs(blob, `${finalFilename}.json`);
    },

    // 2. CSV Export
    downloadCsv: (data, filename) => {
        if (!data || Object.keys(data).length === 0) {
            alert('생성된 데이터가 없어 다운로드할 수 없습니다.');
            return;
        }

        const finalFilename = filename || getTimestampFilename();
        let csvContent = "";
        let hasData = false;

        Object.keys(data).forEach(tableName => {
            const rows = data[tableName];
            if (!Array.isArray(rows) || rows.length === 0) return;
            if (!rows[0]) return;

            hasData = true;
            csvContent += `TABLE: ${tableName}\n`;
            const headers = Object.keys(rows[0]);
            csvContent += headers.join(",") + "\n";

            rows.forEach(row => {
                if (!row) return;
                const values = headers.map(header => {
                    const val = row[header];
                    const stringVal = String(val === null || val === undefined ? '' : val);
                    if (stringVal.includes(",") || stringVal.includes('"') || stringVal.includes("\n")) {
                        return `"${stringVal.replace(/"/g, '""')}"`;
                    }
                    return stringVal;
                });
                csvContent += values.join(",") + "\n";
            });
            csvContent += "\n";
        });

        if (!hasData) {
            alert('생성된 데이터가 없어 다운로드할 수 없습니다.');
            return;
        }

        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        saveAs(blob, `${finalFilename}.csv`);
    },

    // 3. SQL Export
    downloadSql: (data, filename) => {
        if (!data || Object.keys(data).length === 0) {
            alert('생성된 데이터가 없어 다운로드할 수 없습니다.');
            return;
        }

        const finalFilename = filename || getTimestampFilename();
        let sqlContent = "";
        let hasData = false;

        Object.keys(data).forEach(tableName => {
            const rows = data[tableName];
            if (!Array.isArray(rows) || rows.length === 0) return;
            if (!rows[0]) return;

            hasData = true;
            sqlContent += `-- Data for table: ${tableName}\n`;

            rows.forEach(row => {
                if (!row) return;
                const headers = Object.keys(row);
                if (headers.length === 0) return;

                const values = headers.map(key => {
                    const val = row[key];
                    if (val === null || val === undefined) return 'NULL';
                    if (typeof val === 'number') return val;
                    if (typeof val === 'boolean') return val ? 1 : 0; // or true/false depending on DB
                    return `'${String(val).replace(/'/g, "''")}'`;
                });

                sqlContent += `INSERT INTO ${tableName} (${headers.join(", ")}) VALUES (${values.join(", ")});\n`;
            });
            sqlContent += "\n";
        });

        if (!hasData) {
            alert('생성된 데이터가 없어 다운로드할 수 없습니다.');
            return;
        }

        const blob = new Blob([sqlContent], { type: 'text/plain;charset=utf-8' });
        saveAs(blob, `${finalFilename}.sql`);
    },

    // 4. Excel (XLSX / XLS) Export
    downloadExcel: (data, filename, format = 'xlsx') => {
        if (!data || Object.keys(data).length === 0) {
            alert('생성된 데이터가 없어 다운로드할 수 없습니다.');
            return;
        }

        const finalFilename = filename || getTimestampFilename();
        const wb = XLSX.utils.book_new();
        let hasData = false;

        Object.keys(data).forEach(tableName => {
            const rows = data[tableName];
            if (!Array.isArray(rows) || rows.length === 0) return;

            const validRows = rows.filter(r => r !== null && r !== undefined);
            if (validRows.length === 0) return;

            hasData = true;
            const ws = XLSX.utils.json_to_sheet(validRows);
            const sheetName = tableName.length > 30 ? tableName.substring(0, 30) : tableName;
            XLSX.utils.book_append_sheet(wb, ws, sheetName);
        });

        // 데이터 없음 체크
        if (!hasData || wb.SheetNames.length === 0) {
            alert('생성된 데이터가 없어 다운로드할 수 없습니다.');
            return;
        }

        // Write file using binary string conversion for better compatibility
        const wbout = XLSX.write(wb, { bookType: format, type: 'binary' });

        function s2ab(s) {
            const buf = new ArrayBuffer(s.length);
            const view = new Uint8Array(buf);
            for (let i = 0; i < s.length; i++) view[i] = s.charCodeAt(i) & 0xFF;
            return buf;
        }

        saveAs(new Blob([s2ab(wbout)], { type: "application/octet-stream" }), `${finalFilename}.${format}`);
    }
};

export default ExportUtils;
